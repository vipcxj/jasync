package io.github.vipcxj.jasync.ng.asm;

import io.github.vipcxj.jasync.ng.utils.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.Set;

public class ClassChecker extends ClassVisitor {

    private String className;
    private final Set<String> fields;
    private final Set<String> methods;
    private final Set<String> asyncMethods;


    public ClassChecker(ClassVisitor classVisitor) {
        super(Constants.ASM_VERSION, classVisitor);
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

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = Type.getObjectType(name).getClassName();
        super.visit(version, access, name, signature, superName, interfaces);
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
        Type returnType = Type.getReturnType(descriptor);
        if (returnType.getSort() != Type.OBJECT) {
            return mv;
        }
        String returnTypeName = returnType.getClassName();
        if (Utils.isJPromise(returnTypeName)) {
            String key = name + descriptor;
            asyncMethods.add(key);
            Logger.info("Find async method " + key + " in class " + className);
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
}
