package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class StaticInitMerger extends ClassVisitor {

    private final List<MethodNode> initMethods;

    protected StaticInitMerger(ClassVisitor classVisitor) {
        super(Constants.ASM_VERSION, classVisitor);
        this.initMethods = new ArrayList<>();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if ("<clinit>".equals(name) && cv != null) {
            MethodNode initMethod = new MethodNode(Constants.ASM_VERSION, access, name, descriptor, signature, exceptions);
            initMethods.add(initMethod);
            return initMethod;
        } else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    private void mergeStaticInit(MethodNode target, MethodNode source, LabelMap labelMap) {
        if (source.instructions.size() > 0) {
            target.maxLocals = Math.max(target.maxLocals, source.maxLocals);
            target.maxStack = Math.max(target.maxStack, source.maxStack);
            LabelNode endNode = new LabelNode();
            for (AbstractInsnNode insnNode : source.instructions) {
                if (insnNode.getOpcode() == Opcodes.RETURN) {
                    target.instructions.add(new JumpInsnNode(Opcodes.GOTO, endNode));
                } else {
                    target.instructions.add(insnNode.clone(labelMap));
                }
            }
            target.instructions.add(endNode);
            if (!source.tryCatchBlocks.isEmpty()) {
                for (TryCatchBlockNode tcbNode : source.tryCatchBlocks) {
                    TryCatchBlockNode clonedTcbNode = new TryCatchBlockNode(
                            labelMap.get(tcbNode.start),
                            labelMap.get(tcbNode.end),
                            labelMap.get(tcbNode.handler),
                            tcbNode.type
                    );
                    if (target.tryCatchBlocks == null) {
                        target.tryCatchBlocks = new ArrayList<>();
                    }
                    int i = target.tryCatchBlocks.size();
                    target.tryCatchBlocks.add(clonedTcbNode);
                    clonedTcbNode.updateIndex(i);
                }
            }
            if (!source.localVariables.isEmpty()) {
                for (LocalVariableNode localVariable : source.localVariables) {
                    LocalVariableNode clonedLvNode = new LocalVariableNode(
                            localVariable.name,
                            localVariable.desc,
                            localVariable.signature,
                            labelMap.get(localVariable.start),
                            labelMap.get(localVariable.end),
                            localVariable.index
                    );
                    if (target.localVariables == null) {
                        target.localVariables = new ArrayList<>();
                    }
                    target.localVariables.add(clonedLvNode);
                }
            }
        }
    }

    @Override
    public void visitEnd() {
        if (!initMethods.isEmpty() && cv != null) {
            MethodNode initMethod = new MethodNode(
                    Constants.ASM_VERSION,
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                    "<clinit>",
                    "()V",
                    null, null
            );
            LabelMap labelMap = new LabelMap();
            for (MethodNode method : initMethods) {
                mergeStaticInit(initMethod, method, labelMap);
            }
            initMethod.instructions.add(new InsnNode(Opcodes.RETURN));
            initMethod.accept(cv);
        }
        super.visitEnd();
    }
}
