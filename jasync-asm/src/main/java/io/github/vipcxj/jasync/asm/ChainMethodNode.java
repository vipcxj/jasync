package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.util.*;

import java.io.PrintWriter;

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

    private void log(MethodNode methodNode, Printer printer) {
        PrintWriter printWriter = new PrintWriter(System.out);
        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, printer, printWriter);
        MethodVisitor methodVisitor = traceClassVisitor.visitMethod(
                methodNode.access,
                methodNode.name,
                methodNode.desc,
                methodNode.signature,
                methodNode.exceptions != null ? methodNode.exceptions.toArray(new String[0]) : null
        );
        methodNode.accept(methodVisitor);
        printer.print(printWriter);
        printWriter.flush();
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        MethodContext methodContext = new MethodContext(classContext, methodNode, null);
        methodContext.process();
        JAsyncInfo info = methodContext.getInfo();
        if (info.isLogByteCode()) {
            log(methodNode, new Textifier());
        }
        if (info.isLogAsm()) {
            log(methodNode, new ASMifier());
        }
        if (info.isVerify()) {
            verifyMethod(methodNode);
        }
        if (nextVisitor != null) {
            methodNode.accept(nextVisitor);
        }
        for (MethodContext lambdaContext : classContext.getLambdaContexts(methodContext)) {
            if (info.isLogByteCode()) {
                log(lambdaContext.getMv(), new Textifier());
            }
            if (info.isLogAsm()) {
                log(lambdaContext.getMv(), new ASMifier());
            }
            if (info.isVerify()) {
                verifyMethod(lambdaContext.getMv());
            }
            if (nextClassVisitor != null) {
                lambdaContext.getMv().accept(nextClassVisitor);
            }
        }
    }
}
