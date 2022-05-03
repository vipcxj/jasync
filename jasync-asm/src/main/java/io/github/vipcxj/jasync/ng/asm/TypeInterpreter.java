package io.github.vipcxj.jasync.ng.asm;


import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.List;

public class TypeInterpreter extends BasicInterpreter {

    public TypeInterpreter() {
        super(Constants.ASM_VERSION);
    }

    @Override
    public BasicValue newValue(Type type) {
        BasicValue value = JAsyncValue.newValue(type);
        return value != null ? value : super.newValue(type);
    }

    @Override
    public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        if (value instanceof JAsyncValue || AsmHelper.isModifyLocalInsn(insn)) {
            return JAsyncValue.copyOperation(insn, value);
        }
        return super.copyOperation(insn, value);
    }

    @Override
    public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
        BasicValue value = JAsyncValue.newOperation(insn);
        return value != null ? value : super.newOperation(insn);
    }

    @Override
    public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values) throws AnalyzerException {
        // AsmHelper.processConstruct(insn, values);
        return super.naryOperation(insn, values);
    }

    public static BasicValue doMerge(BasicValue value1, BasicValue value2) {
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
            if (t != null && u != null) {
                Type newType = AsmHelper.getNearestCommonAncestorType(t, u);
                merged = newType != null ? new JAsyncValue(newType) : null;
            } else {
                merged = null;
            }
        } else {
            assert value1 != null;
            merged = null;
        }
        return merged;
    }

    @Override
    public BasicValue merge(BasicValue value1, BasicValue value2) {
        BasicValue value = doMerge(value1, value2);
        return value != null ? value : super.merge(value1, value2);
    }
}
