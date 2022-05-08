package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

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
        Analyzer<BasicValue> analyzer = new BranchAnalyzer(verifier);
        MethodNode methodNode = methodContext.getMv();
        try {
            analyzer.analyzeAndComputeMaxs(classContext.getName(), methodNode);
        } catch (AnalyzerException e) {
            AsmHelper.printFrameProblem(
                    classContext.getName(),
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
    private void patchTryCatchFrame() {
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

    @Override
    public void visitEnd() {
        super.visitEnd();
        patchTryCatchFrame();
        MethodContext methodContext = new MethodContext(classContext, methodNode);
        JAsyncInfo info = methodContext.getInfo();
        AsmHelper.printFrameProblem(
                classContext.getName(),
                methodNode,
                methodContext.getFrames(),
                null,
                info.getLogOriginalByteCode()
        );
        methodContext.process();
        AsmHelper.printFrameProblem(
                classContext.getName(),
                methodNode,
                null,
                methodContext.getMap(),
                info.getLogResultByteCode()
        );
        if (info.isLogResultAsm()) {
            log(methodNode, new ASMifier());
        }
        if (info.isVerify()) {
            verifyMethod(methodContext);
        }
        if (nextVisitor != null) {
            try {
                methodNode.accept(nextVisitor);
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
                    classContext.getName(),
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
