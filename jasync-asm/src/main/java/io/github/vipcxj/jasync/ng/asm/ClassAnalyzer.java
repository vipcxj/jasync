package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassAnalyzer extends ClassVisitor {

    private final ClassContext classContext;

    public ClassAnalyzer(ClassContext classContext, ClassVisitor classVisitor) {
        super(Constants.ASM_VERSION, classVisitor);
        this.classContext = classContext;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (classContext.getChecker().isAsyncMethod(name, descriptor)) {
            return new ChainMethodNode(access, name, descriptor, signature, exceptions, mv, cv, classContext);
        } else {
            return mv;
        }
    }
}
