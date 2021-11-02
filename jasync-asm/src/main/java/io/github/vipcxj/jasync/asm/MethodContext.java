package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.*;

public class MethodContext {
    private final ClassContext classContext;
    private final MethodNode mv;
    private final BranchAnalyzer.Node<BasicValue>[] frames;
    private final Map<LabelNode, LabelNode> cloneLabels;
    private final Set<String> varNames;
    private final List<Label> labels;
    private int varIndex;
    private int locals;
    private int stacks;


    public MethodContext(ClassContext classContext, MethodNode mv, BranchAnalyzer.Node<BasicValue>[] frames) {
        this.classContext = classContext;
        this.mv = mv;
        this.frames = frames;
        this.varIndex = 0;
        this.locals = mv.maxLocals;
        this.stacks = mv.maxStack;
        this.varNames = new HashSet<>();
        collectVarNames();
        this.labels = new ArrayList<>();
        this.cloneLabels = new HashMap<>();
        collectLabels();
    }

    private void collectVarNames() {
        List<LocalVariableNode> localVariables = mv.localVariables;
        if (localVariables != null) {
            for (LocalVariableNode localVariable : localVariables) {
                varNames.add(localVariable.name);
            }
        }
    }

    private void collectLabels() {
        if (mv.instructions != null) {
            for (AbstractInsnNode instruction : mv.instructions) {
                if (instruction instanceof LabelNode) {
                    LabelNode labelNode = (LabelNode) instruction;
                    labels.add(labelNode.getLabel());
                    cloneLabels.put(labelNode, new LabelNode(new Label()));
                }
            }
        }
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

    public boolean labelBefore(Label a, Label b, boolean includeEquals) {
        if (includeEquals && a.equals(b)) {
            return true;
        }
        if (a == Constants.LABEL_START) {
            return true;
        }
        if (b == Constants.LABEL_END) {
            return false;
        }
        for (Label label : labels) {
            if (label.equals(a)) {
                return true;
            }
            if (label.equals(b)) {
                return false;
            }
        }
        throw new IllegalArgumentException("Invalid labels.");
    }




}
