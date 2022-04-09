package io.github.vipcxj.jasync.ng.core.asm;

import org.objectweb.asm.ClassVisitor;

import java.util.Arrays;

public class TypeScanner extends ClassVisitor {

    private String name;
    private String superName;
    private String[] interfaces;

    public TypeScanner(ClassVisitor classVisitor) {
        super(Constants.ASM_VERSION, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.name = name.replace('/', '.');
        this.superName = superName != null ? superName.replace('/', '.') : null;
        this.interfaces = Arrays.stream(interfaces).map(itf -> itf.replace('/', '.')).toArray(String[]::new);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public String getName() {
        return name;
    }

    public String getSuperName() {
        return superName;
    }

    public String[] getInterfaces() {
        return interfaces;
    }
}
