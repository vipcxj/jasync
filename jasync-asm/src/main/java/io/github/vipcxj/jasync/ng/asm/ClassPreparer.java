package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassPreparer extends ClassVisitor {

    private final ClassContext classContext;

    public ClassPreparer(ClassContext classContext, ClassVisitor classVisitor) {
        super(Constants.ASM_VERSION, classVisitor);
        this.classContext = classContext;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
