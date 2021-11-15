package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodContext {
    private final ClassContext classContext;
    private final MethodNode mv;
    private final BranchAnalyzer.Node<BasicValue>[] frames;
    private final Map<LabelNode, LabelNode> cloneLabels;
    private final List<Integer> localsToUpdate;
    private final List<Integer> stacksToUpdate;



    public MethodContext(ClassContext classContext, MethodNode mv) {
        this.classContext = classContext;
        this.mv = mv;
        this.localsToUpdate = new ArrayList<>();
        this.stacksToUpdate = new ArrayList<>();
        this.cloneLabels = new HashMap<>();
        collectLabels();
        BranchAnalyzer analyzer = new BranchAnalyzer();
        try {
            analyzer.analyze(classContext.getName(), mv);
            this.frames = analyzer.getNodes();
        } catch (AnalyzerException e) {
            throw new RuntimeException(e);
        }
    }

    private void collectLabels() {
        if (mv.instructions != null) {
            for (AbstractInsnNode instruction : mv.instructions) {
                if (instruction instanceof LabelNode) {
                    LabelNode labelNode = (LabelNode) instruction;
                    cloneLabels.put(labelNode, new LabelNode(new Label()));
                }
            }
        }
    }

    public MethodContext createChild(MethodNode methodNode) {
        return new MethodContext(classContext, methodNode);
    }

    public MethodNode getMv() {
        return mv;
    }

    public BranchAnalyzer.Node<BasicValue>[] getFrames() {
        return frames;
    }

    public boolean isStatic() {
        return (mv.access & Opcodes.ACC_STATIC) != 0;
    }

    public String nextLambdaName() {
        return classContext.nextLambdaName(mv.name);
    }

    public Type classType() {
        return Type.getObjectType(classContext.getName());
    }

    public <T extends AbstractInsnNode> T cloneInsn(T node) {
        //noinspection unchecked
        return (T) node.clone(cloneLabels);
    }

    public void replaceLabel(LabelNode from, LabelNode to) {
        cloneLabels.put(from, to);
    }

    public void updateLocals(int locals) {
        localsToUpdate.add(locals);
    }

    public void updateStacks(int stacks) {
        stacksToUpdate.add(stacks);
    }

    public void updateMax() {
        mv.maxLocals = Math.max(mv.maxLocals, localsToUpdate.stream().mapToInt(i -> i).max().orElse(0));
        mv.maxStack = Math.max(mv.maxStack, stacksToUpdate.stream().mapToInt(i -> i).max().orElse(0));
    }

    public void addLambdaContext(MethodContext methodContext) {
        this.classContext.addLambda(methodContext);
    }




}
