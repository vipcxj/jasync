package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassAnalyzer extends ClassVisitor {

    private final ClassChecker checker;
    private ClassContext classContext;

    public ClassAnalyzer(ClassChecker checker, ClassVisitor classVisitor) {
        super(Constants.ASM_VERSION, classVisitor);
        this.checker = checker;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.classContext = new ClassContext(name, checker);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (checker.isAsyncMethod(name, descriptor)) {
            return new ChainMethodNode(access, name, descriptor, signature, exceptions, mv, cv, classContext);
        } else {
            return mv;
        }
    }
}
