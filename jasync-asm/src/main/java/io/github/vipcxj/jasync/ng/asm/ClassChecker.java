package io.github.vipcxj.jasync.ng.asm;

import io.github.vipcxj.jasync.ng.utils.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.Set;

public class ClassChecker extends ClassVisitor {

    private String className;
    private final Set<String> methods;
    private final Set<String> asyncMethods;


    public ClassChecker(ClassVisitor classVisitor) {
        super(Constants.ASM_VERSION, classVisitor);
        this.methods = new HashSet<>();
        this.asyncMethods = new HashSet<>();
    }

    public boolean hasAsyncMethod() {
        return !asyncMethods.isEmpty();
    }

    public boolean isAsyncMethod(String name, String descriptor) {
        return asyncMethods.contains(name + descriptor);
    }

    public Set<String> getMethods() {
        return methods;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = Type.getObjectType(name).getClassName();
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        methods.add(name);
        Type returnType = Type.getReturnType(descriptor);
        if (returnType.getSort() != Type.OBJECT) {
            return mv;
        }
        String returnTypeName = returnType.getClassName();
        if (Utils.isJPromise(returnTypeName)) {
            return new MethodChecker(name, descriptor, mv);
        } else {
            return mv;
        }
    }

    class MethodChecker extends MethodVisitor {

        private final String name;
        private final String descriptor;
        private boolean useAwait;

        public MethodChecker(String name, String descriptor, MethodVisitor mv) {
            super(Constants.ASM_VERSION, mv);
            this.name = name;
            this.descriptor = descriptor;
            this.useAwait = false;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (!useAwait) {
                if (AsmHelper.isAwait(opcode, owner, name, descriptor)) {
                    useAwait = true;
                    String key = this.name + this.descriptor;
                    Logger.info("Find async method " + key + " in class " + className);
                    asyncMethods.add(key);
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
