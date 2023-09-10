package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;

import java.io.PrintWriter;
import java.util.*;

public class ChainMethodNode extends MethodVisitor {
    private final MethodVisitor nextVisitor;
    private final ClassVisitor nextClassVisitor;
    private final MethodNode methodNode;
    private final ClassContext classContext;

    public ChainMethodNode(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions,
            final MethodVisitor nextVisitor,
            final ClassVisitor nextClassVisitor,
            final ClassContext classContext
    ) {
        super(
                Constants.ASM_VERSION,
                new MethodNode(
                        Constants.ASM_VERSION,
                        access,
                        name,
                        descriptor,
                        signature,
                        exceptions
                )
        );
        this.nextVisitor = nextVisitor;
        this.nextClassVisitor = nextClassVisitor;
        this.methodNode = (MethodNode) mv;
        this.classContext = classContext;
    }

    private void verifyMethod(MethodContext methodContext) {
        BasicVerifier verifier = new MyVerifier();
        Analyzer<BasicValue> analyzer = new BranchAnalyzer(verifier, false);
        MethodNode methodNode = methodContext.getMv();
        try {
            analyzer.analyzeAndComputeMaxs(classContext.getInternalName(), methodNode);
        } catch (AnalyzerException e) {
            AsmHelper.printFrameProblem(
                    classContext.getInternalName(),
                    methodNode,
                    analyzer.getFrames(),
                    methodContext.getMap(),
                    e,
                    JAsyncInfo.BYTE_CODE_OPTION_FULL_SUPPORT,
                    -12, 12
            );
            throw new RuntimeException(e);
        }
    }

    private void log(MethodNode methodNode, Printer printer) {
        PrintWriter printWriter = new PrintWriter(System.out);
        AsmHelper.printMethod(methodNode, printer, printWriter, true);
    }

    /**
     * asm merge the insn node in try block to the try handler.
     * However the frame of the insn node is the state before executed.
     * The state after the last insn node executed in the try block is not merged to the try handler.
     * So we add a label node just before try end, so the last state is always merged to try handler.
     */
    private void patchTryCatchFrame(MethodNode methodNode) {
        if (methodNode.tryCatchBlocks != null && !methodNode.tryCatchBlocks.isEmpty()) {
            Set<LabelNode> labels = new HashSet<>();
            for (TryCatchBlockNode tryCatchBlock : methodNode.tryCatchBlocks) {
                labels.add(tryCatchBlock.end);
            }
            ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insnNode = iterator.next();
                if (insnNode instanceof LabelNode && labels.contains(insnNode)) {
                    if (iterator.hasPrevious()) {
                        iterator.previous();
                        iterator.add(new LabelNode());
                        iterator.next();
                    }
                }
            }
        }
    }

    private int calcRealMethodAccess(int access) {
        int outAccess = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
        if ((access & Opcodes.ACC_STATIC) != 0) {
            outAccess |= Opcodes.ACC_STATIC;
        }
        return outAccess;
    }

    private LocalVariableNode findLocalVar(int index, List<LocalVariableNode> nodes, String desc) {
        for (LocalVariableNode node : nodes) {
            if (node.index == index && Objects.equals(node.desc, desc)) {
                return node;
            }
        }
        return null;
    }

    private String calcLocalVarName(String baseName, List<LocalVariableNode> nodes) {
        if (nodes != null) {
            for (LocalVariableNode lvn : nodes) {
                if (Objects.equals(lvn.name, baseName)) {
                    return calcLocalVarName(baseName + "_", nodes);
                }
            }
        }
        return baseName;
    }

    private void toBridgeMethod(MethodNode mv, String realMethodName) {
        mv.instructions = new InsnList();
        if (mv.tryCatchBlocks != null) {
            mv.tryCatchBlocks.clear();
        }
        // All content of bride method is moved to real method, so its localVariableAnnotations is invalid.
        mv.visibleLocalVariableAnnotations = null;
        mv.invisibleLocalVariableAnnotations = null;
        boolean isStatic = (mv.access & Opcodes.ACC_STATIC) != 0;
        Type methodType = Type.getMethodType(mv.desc);
        mv.maxLocals = AsmHelper.calcMethodArgLocals(mv);
        mv.maxStack = 4;
        LabelNode start = new LabelNode();
        mv.instructions.add(start);
        if (!isStatic) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
        }
        int i = isStatic ? 0 : 1;
        Type[] argumentTypes = methodType.getArgumentTypes();
        for (Type argumentType : argumentTypes) {
            mv.visitVarInsn(argumentType.getOpcode(Opcodes.ILOAD), i);
            i += argumentType.getSize();
        }
        Type ownerType = Type.getObjectType(classContext.getInternalName());
        InvokeDynamicInsnNode invokeDynamicInsnNode = LambdaUtils.invokeJAsyncPromiseSupplier1(
                ownerType,
                realMethodName,
                isStatic,
                argumentTypes
        );
        invokeDynamicInsnNode.accept(mv);
        mv.visitLdcInsn(classContext.getQualifiedName());
        mv.visitLdcInsn(mv.name);
        String source = classContext.getChecker().getSource();
        if (source != null) {
            mv.visitLdcInsn(source);
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_METHOD_DEBUG_INFO_NAME,
                Constants.JPROMISE_METHOD_DEBUG_INFO_DESC.getDescriptor(),
                true);
        mv.visitInsn(Opcodes.ARETURN);
        LabelNode end = new LabelNode();
        mv.instructions.add(end);
        if (mv.localVariables != null) {
            Type[] argTypes = Type.getMethodType(mv.desc).getArgumentTypes();
            int argNum = argTypes.length;
            int offset = AsmHelper.isStatic(mv) ? 0 : 1;
            List<LocalVariableNode> newLocalVariables = new ArrayList<>();
            if (!AsmHelper.isStatic(mv)) {
                String thisDesc = Type.getObjectType(classContext.getInternalName()).getDescriptor();
                LocalVariableNode thisVar = findLocalVar(0, mv.localVariables, thisDesc);
                if (thisVar != null) {
                    thisVar.start = start;
                    thisVar.end = end;
                } else {
                    thisVar = new LocalVariableNode(
                            "this",
                            thisDesc,
                            null,
                            start,
                            end,
                            0
                    );
                }
                newLocalVariables.add(thisVar);
            }
            for (int j = 0; j < argNum; ++j) {
                String descriptor = argTypes[j].getDescriptor();
                LocalVariableNode localVar = findLocalVar(j + offset, mv.localVariables, descriptor);
                if (localVar != null) {
                    localVar.start = start;
                    localVar.end = end;
                } else {
                    String name = calcLocalVarName("param" + j, mv.localVariables);
                    localVar = new LocalVariableNode(
                            name,
                            descriptor,
                            null,
                            start,
                            end,
                            j + offset
                    );
                }
                newLocalVariables.add(localVar);
            }
            mv.localVariables = newLocalVariables;
        }
    }

    private String toBridgeMethodDesc(String desc) {
        if (desc == null) {
            return null;
        }
        Type methodType = Type.getMethodType(desc);
        Type[] argumentTypes = methodType.getArgumentTypes();
        Type[] newArgumentTypes = new Type[argumentTypes.length + 1];
        System.arraycopy(argumentTypes, 0, newArgumentTypes, 0, argumentTypes.length);
        newArgumentTypes[argumentTypes.length] = Constants.JCONTEXT_DESC;
        return Type.getMethodDescriptor(methodType.getReturnType(), newArgumentTypes);
    }

    private String toBridgeMethodSignature(String signature) {
        if (signature == null) {
            return null;
        }
        int i = signature.indexOf(")");
        if (i == -1) {
            return signature;
        }
        return signature.substring(0, i) + Constants.JCONTEXT_DESC.getDescriptor() + signature.substring(i);
    }

    private void fixRealMethodLocalVars(MethodNode methodNode) {
        if (methodNode.localVariables == null) {
            return;
        }
        LabelNode start = null, end = null;
        boolean isStatic = AsmHelper.isStatic(methodNode);
        if (!isStatic) {
            String thisDesc = Type.getObjectType(classContext.getInternalName()).getDescriptor();
            LocalVariableNode thisVar = findLocalVar(0, methodNode.localVariables, thisDesc);
            if (thisVar != null) {
                start = thisVar.start;
                end = thisVar.end;
            }
        }
        if (start == null) {
            AbstractInsnNode firstNode = methodNode.instructions.getFirst();
            AbstractInsnNode lastNode = methodNode.instructions.getLast();
            if (firstNode instanceof LabelNode && lastNode instanceof LabelNode) {
                start = (LabelNode) firstNode;
                end = (LabelNode) lastNode;
            }
        }
        if (start == null) {
            start = new LabelNode();
            end = new LabelNode();
            methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), start);
            methodNode.instructions.insert(methodNode.instructions.getLast(), end);
        }
        Type[] argumentTypes = Type.getMethodType(methodNode.desc).getArgumentTypes();
        methodNode.localVariables.add(new LocalVariableNode(
                calcLocalVarName("context", methodNode.localVariables),
                Constants.JCONTEXT_DESC.getDescriptor(),
                null,
                start,
                end,
                argumentTypes.length - 1 + (isStatic ? 0 : 1)
        ));
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        patchTryCatchFrame(methodNode);
        String realMethodName = classContext.nextLambdaName(methodNode.name);
        MethodNode realMethod = new MethodNode(
                Constants.ASM_VERSION,
                calcRealMethodAccess(methodNode.access),
                realMethodName,
                toBridgeMethodDesc(methodNode.desc),
                toBridgeMethodSignature(methodNode.signature),
                new String[] { Constants.THROWABLE_NAME }
        );
        methodNode.accept(realMethod);
        fixRealMethodLocalVars(realMethod);
        realMethod.visibleAnnotableParameterCount = 0;
        realMethod.visibleParameterAnnotations = null;
        realMethod.visibleAnnotations = null;
        realMethod.visibleTypeAnnotations = null;
        realMethod.invisibleAnnotableParameterCount = 0;
        realMethod.invisibleParameterAnnotations = null;
        realMethod.invisibleAnnotations = null;
        realMethod.invisibleTypeAnnotations = null;
        toBridgeMethod(methodNode, realMethodName);
        if (nextVisitor != null) {
            methodNode.accept(nextVisitor);
        }

        MethodContext methodContext = new MethodContext(classContext, realMethod, methodNode.name, JAsyncInfo.of(methodNode));
        JAsyncInfo info = methodContext.getInfo();
        AsmHelper.printFrameProblem(
                classContext.getInternalName(),
                realMethod,
                methodContext.getFrames(),
                null,
                info.getLogOriginalByteCode()
        );
        methodContext.process();
        AsmHelper.printFrameProblem(
                classContext.getInternalName(),
                realMethod,
                null,
                methodContext.getMap(),
                info.getLogResultByteCode()
        );
        if (info.isLogResultAsm()) {
            log(realMethod, new ASMifier());
        }
        if (info.isVerify()) {
            verifyMethod(methodContext);
        }
        if (nextClassVisitor != null) {
            try {
                realMethod.accept(nextClassVisitor);
            } catch (RuntimeException t) {
                verifyMethod(methodContext);
                throw t;
            }
        }

        for (FieldContext fieldContext : classContext.getFieldContexts(methodContext)) {
            if (nextClassVisitor != null) {
                fieldContext.accept(nextClassVisitor);
            }
        }

        for (MethodContext lambdaContext : classContext.getLambdaContexts(methodContext)) {
            AsmHelper.printFrameProblem(
                    classContext.getInternalName(),
                    lambdaContext.getMv(),
                    null,
                    lambdaContext.getMap(),
                    info.getLogResultByteCode()
            );
            if (info.isLogResultAsm()) {
                log(lambdaContext.getMv(), new ASMifier());
            }
            if (info.isVerify()) {
                verifyMethod(lambdaContext);
            }
            if (nextClassVisitor != null) {
                try {
                    lambdaContext.getMv().accept(nextClassVisitor);
                } catch (RuntimeException t) {
                    verifyMethod(lambdaContext);
                    throw t;
                }
            }
        }
    }
}
