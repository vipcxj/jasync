package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ClassChecker extends ClassVisitor {

    private String className;

    public ClassChecker(ClassVisitor classVisitor) {
        super(Constants.ASM_VERSION, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = Type.getObjectType(name).toString();
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (Constants.ANN_ASYNC_DESC.equals(descriptor)) {
            System.out.println("Find async class: " + className);
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodChecker(name, descriptor, signature, super.visitMethod(access, name, descriptor, signature, exceptions));
    }

    class MethodChecker extends MethodVisitor {

        private final String name;
        private final String descriptor;
        private final String signature;

        public MethodChecker(String name, String descriptor, String signature, MethodVisitor methodVisitor) {
            super(Constants.ASM_VERSION, methodVisitor);
            this.name = name;
            this.descriptor = descriptor;
            this.signature = signature;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (Constants.ANN_ASYNC_DESC.equals(descriptor)) {
                System.out.println("Find async method " + name + ": " + signature + " in class " + className);
            }
            return super.visitAnnotation(descriptor, visible);
        }
    }
}
