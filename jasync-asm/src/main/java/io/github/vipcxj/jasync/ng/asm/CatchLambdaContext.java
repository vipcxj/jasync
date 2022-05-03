package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CatchLambdaContext extends AbstractLambdaContext {

    private final AbstractInsnNode[] successors;
    private final int validLocals;

    protected CatchLambdaContext(
            MethodContext methodContext,
            Arguments arguments,
            BranchAnalyzer.Node<BasicValue> node,
            int validLocals
    ) {
        super(methodContext, MethodContext.MethodType.CATCH_BODY, arguments, node);
        this.validLocals = validLocals;
        this.successors = methodContext.collectSuccessors(node, (in, n) -> in.clone(labelMap));
    }

    @Override
    protected int validLocals() {
        return validLocals;
    }

    @Override
    protected void addInitCodes() {
        int errorOffset = arguments.argumentLocalOffset(methodContext.isStatic(), -1);
        // load error to stack
        // stack: [] -> error
        lambdaNode.visitVarInsn(Opcodes.ALOAD, errorOffset);
        lambdaMap.add(mappedIndex);
    }

    @Override
    protected void addBodyCodes() {
        LinkedList<AbstractInsnNode> insnList = new LinkedList<>();
        List<Integer> insnListMap = new ArrayList<>();
        processSuccessors(successors, insnList, insnListMap);
        addInsnNodes(insnList, insnListMap);
    }
}
