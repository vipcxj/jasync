package io.github.vipcxj.jasync.ng.core.asm;

import io.github.vipcxj.jasync.ng.utils.Logger;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ECJClassWriterEnhancer extends AsmVisitorWrapper.AbstractBase {

    private final String methodName;

    public ECJClassWriterEnhancer(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, Implementation.Context implementationContext, TypePool typePool, FieldList<FieldDescription.InDefinedShape> fields, MethodList<?> methods, int writerFlags, int readerFlags) {
        return new ECJClassVisitor(methodName, classVisitor);
    }

    protected static class ECJClassVisitor extends ClassVisitor {

        private final String methodSign;

        public ECJClassVisitor(String methodName, ClassVisitor classVisitor) {
            super(OpenedClassReader.ASM_API, classVisitor);
            this.methodSign = methodName;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (mv != null && name != null && Objects.equals(methodSign, name)) {
                Logger.info("Find method: " + name + descriptor);
                return new ECJMethodVisitor(mv);
            } else {
                return mv;
            }
        }
    }

    protected static class ECJMethodVisitor extends MethodNode {
        private final MethodVisitor mv;
        public ECJMethodVisitor(MethodVisitor mv) {
            super(io.github.vipcxj.jasync.ng.core.asm.Constants.ASM_VERSION);
            this.mv = mv;
        }

        private static final String CLASS_FILE_TYPE = "org/eclipse/jdt/internal/compiler/ClassFile";
        private static final String WRITER_WRITE_NAME = "write";
        private static final String WRITER_WRITE_DESCRIPTOR = "([BII)V";

        private VarInsnNode getALoadInst(AbstractInsnNode insnNode) {
            if (insnNode.getOpcode() == Opcodes.ALOAD) {
                return (VarInsnNode) insnNode;
            } else {
                return null;
            }
        }

        private FieldInsnNode getGetFieldInst(AbstractInsnNode insnNode) {
            if (insnNode.getOpcode() == Opcodes.GETFIELD) {
                return (FieldInsnNode) insnNode;
            } else {
                return null;
            }
        }

        private LdcInsnNode getLdcZeroInst(AbstractInsnNode insnNode) {
            if (insnNode.getOpcode() == Opcodes.LDC) {
                LdcInsnNode ldcInsnNode = (LdcInsnNode) insnNode;
                if (Objects.equals(ldcInsnNode.cst, 0) || Objects.equals(ldcInsnNode.cst, 0L)) {
                    return ldcInsnNode;
                }
            }
            return null;
        }

        private InsnNode getConstZeroInst(AbstractInsnNode insnNode) {
            if (insnNode.getOpcode() == Opcodes.ICONST_0) {
                return (InsnNode) insnNode;
            } else {
                return null;
            }
        }

        private MethodInsnNode getMethodInst(AbstractInsnNode insnNode) {
            if (insnNode.getOpcode() == Opcodes.INVOKEVIRTUAL || insnNode.getOpcode() == Opcodes.INVOKEINTERFACE) {
                return (MethodInsnNode) insnNode;
            } else {
                return null;
            }
        }

        private boolean isSkip(AbstractInsnNode insnNode) {
            return insnNode instanceof LabelNode || insnNode instanceof LineNumberNode || insnNode instanceof FrameNode;
        }

        private boolean equalMethodInsn(MethodInsnNode a, MethodInsnNode b) {
            if (!Objects.equals(a.getOpcode(), b.getOpcode())) {
                return false;
            }
            if (!Objects.equals(a.name, b.name)) {
                return false;
            }
            if (!Objects.equals(a.owner, b.owner)) {
                return false;
            }
            return Objects.equals(a.desc, b.desc);
        }

        private boolean checkInst(int pos, AbstractInsnNode inst, String field1, String field2, MethodInsnNode[] outInst, int[] vars) {
            boolean second = false;
            if (pos >= 7) {
                second = true;
                pos = pos - 7 - vars[2];
            }
            if (pos == 0) {
                if (second && isSkip(inst)) {
                    vars[2]++;
                    return true;
                }
                VarInsnNode aLoadInst = getALoadInst(inst);
                if (aLoadInst == null) {
                    return false;
                }
                int writerVar = vars[0];
                int var = aLoadInst.var;
                if (writerVar < 0) {
                    vars[0] = var;
                } else if (writerVar != var) {
                    return false;
                }
            }
            if (pos == 1) {
                VarInsnNode aLoadInst = getALoadInst(inst);
                if (aLoadInst == null) {
                    return false;
                }
                int classFileVar = vars[1];
                int var = aLoadInst.var;
                if (classFileVar < 0) {
                    vars[1] = var;
                } else if (classFileVar != var) {
                    return false;
                }
            }
            if (pos == 2) {
                FieldInsnNode getFieldInst = getGetFieldInst(inst);
                if (getFieldInst == null) {
                    return false;
                }
                if (!CLASS_FILE_TYPE.equals(getFieldInst.owner)
                        || !field1.equals(getFieldInst.name)
                        || !"[B".equals(getFieldInst.desc)) {
                    return false;
                }
            }
            if (pos == 3 && getConstZeroInst(inst) == null && getLdcZeroInst(inst) == null) {
                return false;
            }
            if (pos == 4) {
                VarInsnNode aLoadInst = getALoadInst(inst);
                if (aLoadInst == null) {
                    return false;
                }
                if (aLoadInst.var != vars[1]) {
                    return false;
                }
            }
            if (pos == 5) {
                FieldInsnNode getFieldInst = getGetFieldInst(inst);
                if (getFieldInst == null) {
                    return false;
                }
                if (!CLASS_FILE_TYPE.equals(getFieldInst.owner)
                        || !field2.equals(getFieldInst.name)
                        || !"I".equals(getFieldInst.desc)) {
                    return false;
                }
            }
            if (pos == 6) {
                MethodInsnNode methodInst = getMethodInst(inst);
                if (methodInst == null) {
                    return false;
                }
                if (!WRITER_WRITE_NAME.equals(methodInst.name) || !WRITER_WRITE_DESCRIPTOR.equals(methodInst.desc)) {
                    return false;
                }
                if (outInst[0] == null) {
                    outInst[0] = (MethodInsnNode) methodInst.clone(Collections.emptyMap());
                    return true;
                } else {
                    return equalMethodInsn(outInst[0], methodInst);
                }
            }
            return true;
        }

        private void clearCache(InsnList newInsnList, List<AbstractInsnNode> cache) {
            for (AbstractInsnNode insnNode : cache) {
                newInsnList.add(insnNode);
            }
            cache.clear();
        }

        private void reset(MethodInsnNode[] outInst, int[] vars) {
            outInst[0] = null;
            vars[0] = vars[1] = -1;
            vars[2] = 0;
        }

        private void doReplace(InsnList newInsnList, MethodInsnNode[] outInst, int[] vars) {
            Logger.info("Replace the output class bytes code.");
            int writerVar = vars[0];
            int classFileVar = vars[1];
            newInsnList.add(new VarInsnNode(Opcodes.ALOAD, writerVar));
            newInsnList.add(new VarInsnNode(Opcodes.ALOAD, classFileVar));
            newInsnList.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    CLASS_FILE_TYPE,
                    "getBytes",
                    "()[B",
                    false
            ));
            newInsnList.add(new InsnNode(Opcodes.ICONST_0));
            newInsnList.add(new VarInsnNode(Opcodes.ALOAD, classFileVar));
            newInsnList.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    CLASS_FILE_TYPE,
                    "getBytes",
                    "()[B",
                    false
            ));
            newInsnList.add(new InsnNode(Opcodes.ARRAYLENGTH));
            newInsnList.add(outInst[0]);
        }

        @Override
        public void visitEnd() {
            MethodInsnNode[] outInst = new MethodInsnNode[] { null };
            int[] vars = new int[] { -1, -1, 0 };
            int pos = 0;
            InsnList newInsnList = new InsnList();
            List<AbstractInsnNode> cache = new ArrayList<>();
            ListIterator<AbstractInsnNode> iterator = instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insnNode = iterator.next();
                iterator.remove();
                if (pos < 7) {
                    if (checkInst(pos, insnNode, "header", "headerOffset", outInst, vars)) {
                        cache.add(insnNode);
                        ++pos;
                    } else {
                        pos = 0;
                        clearCache(newInsnList, cache);
                        reset(outInst, vars);
                        newInsnList.add(insnNode);
                    }
                } else if (checkInst(pos, insnNode, "contents", "contentsOffset", outInst, vars)) {
                    cache.add(insnNode);
                    if (pos == 13 + vars[2]) {
                        doReplace(newInsnList, outInst, vars);
                        cache.clear();
                        pos = 0;
                        reset(outInst, vars);
                    }
                    ++pos;
                } else {
                    pos = 0;
                    clearCache(newInsnList, cache);
                    reset(outInst, vars);
                    newInsnList.add(insnNode);
                }
            }
            clearCache(newInsnList, cache);
            instructions = newInsnList;
            if (mv != null) {
                accept(mv);
            }
            super.visitEnd();
        }
    }
}
