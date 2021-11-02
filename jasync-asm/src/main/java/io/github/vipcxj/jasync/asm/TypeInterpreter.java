package io.github.vipcxj.jasync.asm;


import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

public class TypeInterpreter extends BasicInterpreter {

    public TypeInterpreter() {
        super(Constants.ASM_VERSION);
    }

    @Override
    public BasicValue newValue(Type type) {
        if (type != null && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)) {
            return new JAsyncValue(type);
        }
        return super.newValue(type);
    }

    @Override
    public BasicValue merge(BasicValue value1, BasicValue value2) {
        BasicValue merged;
        if (value1 instanceof JAsyncValue) {
            return ((JAsyncValue) value1).merge(value2);
        }
        if (value2 instanceof JAsyncValue) {
            return ((JAsyncValue) value2).merge(value1);
        }
        if (value1 != value2 && value1 != null && value2 != null && !value1.equals(value2))
        {
            Type t = value1.getType();
            Type u = value2.getType();
            if (t != null && u != null
                    && (t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY)
                    && (u.getSort() == Type.OBJECT || u.getSort() == Type.ARRAY))
            {
                JAsyncValue jAsyncValue = new JAsyncValue(BasicValue.REFERENCE_VALUE.getType());
                jAsyncValue.getFroms().add(t);
                jAsyncValue.getFroms().add(u);
                merged = jAsyncValue;
            } else {
                merged = super.merge(value1, value2);
            }
        } else {
            merged = super.merge(value1, value2);
        }
        System.out.println("merge "
                + (value1 != null ? value1.getType() : "null")
                + " and "
                + (value2 != null ? value2.getType() : "null")
                + " to "
                + (merged != null ? merged.getType() : "null"));
        return merged;
    }
}
