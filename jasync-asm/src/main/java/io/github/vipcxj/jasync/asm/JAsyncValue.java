package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.Objects;

public class JAsyncValue extends BasicValue {

    private boolean uninitialized;
    private AbstractInsnNode insnNode;


    /**
     * Constructs a new {@link BasicValue} of the given type.
     *
     * @param type the value type.
     */
    public JAsyncValue(Type type) {
        super(type);
        this.uninitialized = false;
    }

    public JAsyncValue merge(BasicValue other) {
        if (other == null || this.equals(other)) {
            return this;
        }
        return new JAsyncValue(BasicValue.REFERENCE_VALUE.getType());
    }

    public boolean isUninitialized() {
        return uninitialized;
    }

    public void setUninitialized(boolean uninitialized) {
        this.uninitialized = uninitialized;
    }

    public AbstractInsnNode getInsnNode() {
        return insnNode;
    }

    public void setInsnNode(AbstractInsnNode insnNode) {
        this.insnNode = insnNode;
    }

    @Override
    public String toString() {
        String s = super.toString();
        if (isUninitialized()) {
            return s + "(uninitialized)";
        } else {
            return s;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JAsyncValue that = (JAsyncValue) o;
        return uninitialized == that.uninitialized;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uninitialized);
    }
}
