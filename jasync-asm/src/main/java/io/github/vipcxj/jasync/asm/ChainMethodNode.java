package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;

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

    private void verifyMethod(MethodNode methodNode) {
        BasicVerifier verifier = new BasicVerifier();
        Analyzer<BasicValue> analyzer = new Analyzer<>(verifier);
        try {
            analyzer.analyze(classContext.getName(), methodNode);
        } catch (AnalyzerException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        MethodContext methodContext = new MethodContext(classContext, methodNode, null);
        methodContext.process();
        if (nextVisitor != null) {
            verifyMethod(methodNode);
            methodNode.accept(nextVisitor);
        }
        if (nextClassVisitor != null) {
            for (MethodContext lambdaContext : classContext.getLambdaContexts()) {
                verifyMethod(lambdaContext.getMv());
                lambdaContext.getMv().accept(nextClassVisitor);
            }
        }
    }
}
