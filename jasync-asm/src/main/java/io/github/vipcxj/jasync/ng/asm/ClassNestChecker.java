package io.github.vipcxj.jasync.ng.asm;

import io.github.vipcxj.jasync.ng.utils.Logger;
import io.github.vipcxj.jasync.ng.utils.TypeInfoHelper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ClassNestChecker extends ClassVisitor {

    private String classInternalName;
    private String classBinaryName;
    private String superName;
    private String[] interfaces;
    private String closingClassName;
    private Map<String, String> innerClassesMap;
    private Map<String, String[]> typeInfoMap;

    protected ClassNestChecker(ClassVisitor classVisitor) {
        super(Constants.ASM_VERSION, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.classInternalName = name;
        this.classBinaryName = Type.getObjectType(name).getClassName();
        this.superName = superName != null ? Type.getObjectType(superName).getClassName() : null;
        this.interfaces = Arrays.stream(interfaces).map(i -> Type.getObjectType(i).getClassName()).toArray(String[]::new);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (outerName == null) {
            int i = name.lastIndexOf('$', innerName != null ? (name.length() - innerName.length() - 1) : (name.length() - 1));
            if (i < 0) {
                throw new IllegalArgumentException("[Scanning " + classBinaryName + "] Unable to calculate the outer class name of class " + name + ".");
            }
            outerName = name.substring(0, i);
        }
        if (innerClassesMap == null) {
            innerClassesMap = new HashMap<>();
        }
        innerClassesMap.put(Type.getObjectType(name).getClassName(), Type.getObjectType(outerName).getClassName());
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        closingClassName = Type.getObjectType(owner).getClassName();
        super.visitOuterClass(owner, name, descriptor);
    }

    private String findTopClassName(String name) {
        if (innerClassesMap != null) {
            String outerName = innerClassesMap.get(name);
            while (outerName != null) {
                name = outerName;
                outerName = innerClassesMap.get(name);
            }
        }
        return name;
    }

    public String getClassInternalName() {
        return classInternalName;
    }

    public String getClassBinaryName() {
        return classBinaryName;
    }

    private String getClassQualifiedName(String binaryName) {
        if (innerClassesMap == null || !innerClassesMap.containsKey(binaryName)) {
            return binaryName;
        } else {
            String outBinaryName = innerClassesMap.get(binaryName);
            String outQualifiedName = getClassQualifiedName(outBinaryName);
            String simpleName = binaryName.substring(outBinaryName.length() + 1);
            return outQualifiedName + "." + simpleName;
        }
    }

    public String getClassQualifiedName() {
        return getClassQualifiedName(classBinaryName);
    }

    public String getTopBinaryName() {
        return findTopClassName(closingClassName != null ? closingClassName : classBinaryName);
    }

    private Map<String, String[]> getTypeInfoMap() {
        if (this.typeInfoMap == null) {
            this.typeInfoMap = Globals.getTypeInfoMap();
        }
        return this.typeInfoMap;
    }

    public boolean isSkip() {
        Map<String, String[]> typeInfoMap = getTypeInfoMap();
        if (typeInfoMap == null) {
            Logger.warn("[Scanning " + classBinaryName + "] Unable to find type info map. So skipped.");
            return true;
        }
        String topBinaryName = getTopBinaryName();
        if (!typeInfoMap.containsKey(topBinaryName)) {
            Logger.warn("[Scanning " + classBinaryName + "] The containing class " + topBinaryName + " is not listed in the type info map. It seems that the source file is not processed by the jasync annotation processor. So skipped.");
            return true;
        }
        return false;
    }

    public void updateTypeInfoMap() {
        Map<String, String[]> typeInfoMap = getTypeInfoMap();
        // call after test isSkip. So it should always that typeInfoMap is not null.
        assert typeInfoMap != null;
        if (!typeInfoMap.containsKey(classBinaryName)) {
            String[] typeInfo = TypeInfoHelper.typeInfo(superName, interfaces);
            typeInfoMap.put(classBinaryName, typeInfo);
        }
    }
}
