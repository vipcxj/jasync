package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class JAsyncValue extends BasicValue {

    private final Set<Type> froms;

    /**
     * Constructs a new {@link BasicValue} of the given type.
     *
     * @param type the value type.
     */
    public JAsyncValue(Type type) {
        super(type);
        this.froms = new HashSet<>();
    }

    public JAsyncValue merge(BasicValue other) {
        if (other == null || this.equals(other)) {
            return this;
        }
        JAsyncValue merged = new JAsyncValue(BasicValue.REFERENCE_VALUE.getType());
        if (froms.isEmpty()) {
            merged.froms.add(getType());
        } else {
            merged.froms.addAll(froms);
        }
        if (other instanceof JAsyncValue)  {
            JAsyncValue asyncValue = (JAsyncValue) other;
            if (asyncValue.froms.isEmpty()) {
                merged.froms.add(asyncValue.getType());
            } else {
                merged.froms.addAll(asyncValue.froms);
            }
        } else {
            merged.froms.add(other.getType());
        }
        return merged;
    }

    public Set<Type> getFroms() {
        return froms;
    }

    @Override
    public String toString() {
        if (froms.isEmpty()) {
            return getType().toString();
        } else {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (Type from : froms) {
                if (i == 0) {
                    sb.append("(").append(from);
                } else if (i < froms.size() - 1) {
                    sb.append(from).append(" | ");
                } else {
                    sb.append(from).append(")");
                }
                ++i;
            }
            return sb.toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!froms.isEmpty()) {
            if (o instanceof JAsyncValue) {
                JAsyncValue other = (JAsyncValue) o;
                return super.equals(o) && froms.equals(other.froms);
            }
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return froms.isEmpty() ? super.hashCode() : Objects.hash(super.hashCode(), froms);
    }
}
