package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

public class JAsyncClassWriter extends ClassWriter {

    public JAsyncClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        Type commonSuperType = AsmHelper.getNearestCommonAncestorType(Type.getObjectType(type1), Type.getObjectType(type2));
        return commonSuperType != null ? commonSuperType.getInternalName() : Constants.OBJECT_NAME;
    }
}
