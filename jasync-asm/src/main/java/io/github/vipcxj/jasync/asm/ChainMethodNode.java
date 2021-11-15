package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

public class ChainMethodNode extends MethodVisitor {
    private final MethodVisitor nextVisitor;
    private final MethodNode methodNode;
    private final ClassContext classContext;

    public ChainMethodNode(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions,
            final MethodVisitor nextVisitor,
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
        this.methodNode = (MethodNode) mv;
        this.classContext = classContext;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        MethodContext methodContext = new MethodContext(classContext, methodNode);
        CodePiece codePiece = new CodePiece(methodContext, null);
        codePiece.process();
        if (nextVisitor != null) {
            methodNode.accept(nextVisitor);
            for (MethodContext lambdaContext : classContext.getLambdaContexts()) {
                lambdaContext.getMv().accept(nextVisitor);
            }
        }
    }
}
