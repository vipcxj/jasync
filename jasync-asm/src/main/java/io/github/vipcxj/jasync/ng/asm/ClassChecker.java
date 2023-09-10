package io.github.vipcxj.jasync.ng.asm;

import io.github.vipcxj.jasync.ng.utils.Logger;
import org.objectweb.asm.*;

import java.util.HashSet;
import java.util.Set;

public class ClassChecker extends ClassVisitor {

    private final ClassNestChecker nestChecker;
    private final Set<String> fields;
    private final Set<String> methods;
    private final Set<String> asyncMethods;
    private String source;

    public ClassChecker(ClassNestChecker nestChecker, ClassVisitor classVisitor) {
        super(Constants.ASM_VERSION, classVisitor);
        this.nestChecker = nestChecker;
        this.fields = new HashSet<>();
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

    public String getSource() {
        return source;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        this.source = source;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        fields.add(name);
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        methods.add(name);
        // We only process methods with body.
        if ((access & Opcodes.ACC_ABSTRACT) != 0 || (access & Opcodes.ACC_NATIVE) != 0) {
            return mv;
        }
        Type returnType = Type.getReturnType(descriptor);
        if (returnType.getSort() != Type.OBJECT) {
            return mv;
        }
        String returnTypeName = returnType.getClassName();
        if (Utils.isJPromise(returnTypeName)) {
            String key = name + descriptor;
            asyncMethods.add(key);
            Logger.info("[Scanning " + nestChecker.getClassBinaryName() + "] Find async method " + key + ".");
        }
        return mv;
    }

    public String generateUniqueFieldPrefix(String name) {
        for (String field : fields) {
            if (field.startsWith(name)) {
                return generateUniqueFieldPrefix("_" + name);
            }
        }
        return name;
    }

    public String generateUniqueFieldName(String name) {
        if (fields.contains(name)) {
            return generateUniqueFieldPrefix("_" + name);
        }
        return name;
    }

    public ClassNestChecker getNestChecker() {
        return nestChecker;
    }
}
