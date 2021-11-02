package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class ChainMethodNode extends MethodVisitor {
    private final String owner;
    private final MethodVisitor nextVisitor;
    private final MethodNode methodNode;

    public ChainMethodNode(
            final String owner,
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions,
            final MethodVisitor nextVisitor
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
        this.owner = owner;
        this.nextVisitor = nextVisitor;
        this.methodNode = (MethodNode) mv;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        BranchAnalyzer analyzer = new BranchAnalyzer();
        try {
            analyzer.analyze(owner, methodNode);
            analyzer.printInst();
        } catch (AnalyzerException e) {
            e.printStackTrace();
        }
        if (nextVisitor != null) {
            methodNode.accept(nextVisitor);
        }
    }
}
