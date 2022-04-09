package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import java.util.List;

import static io.github.vipcxj.jasync.ng.asm.TypeInterpreter.doMerge;

public class MyVerifier extends BasicVerifier {

    public MyVerifier() {
        super(Constants.ASM_VERSION);
    }

    @Override
    public BasicValue newValue(Type type) {
        BasicValue value = JAsyncValue.newValue(type);
        return value != null ? value : super.newValue(type);
    }

    @Override
    public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
        BasicValue value = JAsyncValue.newOperation(insn);
        return value != null ? value : super.newOperation(insn);
    }

    @Override
    public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        if (value instanceof JAsyncValue) {
            return ((JAsyncValue) value).copyOperation(insn);
        }
        return super.copyOperation(insn, value);
    }

    @Override
    public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values) throws AnalyzerException {
        // AsmHelper.processConstruct(insn, values);
        return super.naryOperation(insn, values);
    }

    @Override
    public BasicValue merge(BasicValue value1, BasicValue value2) {
        BasicValue value = doMerge(value1, value2);
        return value != null ? value : super.merge(value1, value2);
    }

    @Override
    protected boolean isArrayValue(BasicValue value) {
        Type type = value.getType();
        return type != null && (type.getSort() == Type.ARRAY || type.equals(NULL_TYPE));
    }

    @Override
    protected BasicValue getElementValue(BasicValue objectArrayValue) throws AnalyzerException {
        if (objectArrayValue == null || objectArrayValue.getType() == null) {
            return super.getElementValue(objectArrayValue);
        }
        if (objectArrayValue.getType().getSort() != Type.ARRAY) {
            if (objectArrayValue.getType().equals(Constants.NULL_DESC)) {
                return newValue(Constants.OBJECT_DESC);
            } else {
                return super.getElementValue(objectArrayValue);
            }
        }
        return newValue(AsmHelper.getComponentType(objectArrayValue.getType(), false));
    }

    @Override
    protected boolean isSubTypeOf(BasicValue value, BasicValue expected) {
        return AsmHelper.isSubTypeOf(value, expected);
    }
}
