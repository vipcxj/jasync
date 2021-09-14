package io.github.vipcxj.jasync.runtime.helpers;

import java.util.concurrent.atomic.AtomicReference;

public class ObjectReference<T> implements java.io.Serializable {

    private static final long serialVersionUID = -2169601381202499045L;
    public static final int FLAG_OTHER = 0;
    public static final int FLAG_BYTE = 1;
    public static final int FLAG_SHORT = 2;
    public static final int FLAG_CHAR = 3;
    public static final int FLAG_INT = 4;
    public static final int FLAG_LONG = 5;
    public static final int FLAG_FLOAT = 6;
    public static final int FLAG_DOUBLE = 7;
    public static final int FLAG_STRING = 8;
    private final AtomicReference<T> atomic;
    private final int flag;

    public ObjectReference(int flag) {
        this(null, flag);
    }

    public ObjectReference(T v, int flag) {
        this.atomic = new AtomicReference<>(v);
        this.flag = flag;
    }

    public T getValue() {
        return atomic.get();
    }

    public void setValue(T v) {
        atomic.set(v);
    }

    public T setAndGet(T v) {
        setValue(v);
        return v;
    }

    public int getFlag() {
        return flag;
    }

    private boolean isInteger(Object value) {
        return value instanceof Integer || value instanceof Long || value instanceof Byte || value instanceof Short || value instanceof Character;
    }

    private Long castLong(Object value) {
        if (value instanceof Integer) {
            return (long) (Integer) value;
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Byte) {
            return (long) (Byte) value;
        } else if (value instanceof Short) {
            return (long) (Short) value;
        } else if (value instanceof Character) {
            return (long) (Character) value;
        } else {
            throw new IllegalArgumentException("Invalid type: " + (value != null ? value.getClass() : null) + ".");
        }
    }

    private boolean isInteger() {
        return flag != FLAG_OTHER && flag != FLAG_FLOAT && flag != FLAG_DOUBLE && flag != FLAG_STRING;
    }

    private boolean isDecimal(Object value) {
        return value instanceof Float || value instanceof Double;
    }

    private Double castDouble(Object value) {
        if (value instanceof Float) {
            return (double) (Float) value;
        } else if (value instanceof Double) {
            return (Double) value;
        } else {
            throw new IllegalArgumentException("Invalid type: " + (value != null ? value.getClass() : null) + ".");
        }
    }

    private boolean isDecimal() {
        return flag == FLAG_FLOAT || flag == FLAG_DOUBLE;
    }

    private boolean isString() {
        return flag == FLAG_STRING;
    }

    private void assertInteger(String op) {
        if (!isInteger()) {
            throw new UnsupportedOperationException("The operation " + op + " only support on integer value.");
        }
    }

    private void assertNumber(String op) {
        if (flag == FLAG_OTHER || flag == FLAG_STRING) {
            throw new UnsupportedOperationException("The operation " + op + " only support on number value.");
        }
    }

    private void assertNumberOrString(String op) {
        if (flag == FLAG_OTHER) {
            throw new UnsupportedOperationException("The operation " + op + " only support on number or string value.");
        }
    }

    private void assertNonNull(T value) {
        if (value == null) {
            throw new NullPointerException("The value should not be null.");
        }
    }

    private long toLong(T value) {
        assertNonNull(value);
        switch (flag) {
            case FLAG_BYTE:
                return (Byte) value;
            case FLAG_CHAR:
                return (Character) value;
            case FLAG_SHORT:
                return (Short) value;
            case FLAG_INT:
                return (Integer) value;
            case FLAG_LONG:
                return (Long) value;
            default:
                throw new IllegalArgumentException("Unable to convert to the long value.");
        }
    }

    private double toDouble(T value) {
        assertNonNull(value);
        switch (flag) {
            case FLAG_BYTE:
                return (Byte) value;
            case FLAG_CHAR:
                return (Character) value;
            case FLAG_SHORT:
                return (Short) value;
            case FLAG_INT:
                return (Integer) value;
            case FLAG_LONG:
                return (Long) value;
            case FLAG_FLOAT:
                return (Float) value;
            case FLAG_DOUBLE:
                return (Double) value;
            default:
                throw new IllegalArgumentException("Unable to convert to the double value.");
        }
    }

    private String toString(T value) {
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private T toT(long value) {
        switch (flag) {
            case FLAG_BYTE:
                return (T) (Byte) (byte) value;
            case FLAG_CHAR:
                return (T) (Character) (char) value;
            case FLAG_SHORT:
                return (T) (Short) (short) value;
            case FLAG_INT:
                return (T) (Integer) (int) value;
            case FLAG_LONG:
                return (T) (Long) value;
            case FLAG_FLOAT:
                return (T) (Float) (float) value;
            case FLAG_DOUBLE:
                return (T) (Double) (double) value;
            default:
                throw new IllegalArgumentException("Unable to convert to the original value.");
        }
    }

    @SuppressWarnings("unchecked")
    private T toT(double value) {
        switch (flag) {
            case FLAG_BYTE:
                return (T) (Byte) (byte) value;
            case FLAG_CHAR:
                return (T) (Character) (char) value;
            case FLAG_SHORT:
                return (T) (Short) (short) value;
            case FLAG_INT:
                return (T) (Integer) (int) value;
            case FLAG_LONG:
                return (T) (Long) (long) value;
            case FLAG_FLOAT:
                return (T) (Float) (float) value;
            case FLAG_DOUBLE:
                return (T) (Double) value;
            default:
                throw new IllegalArgumentException("Unable to convert to the original value.");
        }
    }

    private T toT(String value) {
        //noinspection unchecked
        return (T) value;
    }

    private T plus(T a, T b) {
        return plus(a, b, false);
    }

    private T plus(T a, T b, boolean useDouble) {
        if (isInteger() && !useDouble) {
            long la = toLong(a);
            long lb = toLong(b);
            return toT(la + lb);
        } else if (isDecimal() || isInteger()) {
            double da = toDouble(a);
            double db = toDouble(b);
            return toT(da + db);
        } else if (isString()) {
            String sa = toString(a);
            String sb = toString(b);
            return toT("" + sa + sb);
        }
        throw new IllegalArgumentException("The current value is not number or string, plus is not permit.");
    }

    private T minus(T a, T b) {
        return minus(a, b, false);
    }

    private T minus(T a, T b, boolean useDouble) {
        if (isInteger() && !useDouble) {
            long la = toLong(a);
            long lb = toLong(b);
            return toT(la - lb);
        } else if (isDecimal() || isInteger()) {
            double da = toDouble(a);
            double db = toDouble(b);
            return toT(da - db);
        }
        throw new IllegalArgumentException("The current value is not number, minus is not permit.");
    }

    private T multiply(T a, T b) {
        return multiply (a, b, false);
    }

    private T multiply(T a, T b, boolean useDouble) {
        if (isInteger() && !useDouble) {
            long la = toLong(a);
            long lb = toLong(b);
            return toT(la * lb);
        } else if (isDecimal() || isInteger()) {
            double da = toDouble(a);
            double db = toDouble(b);
            return toT(da * db);
        }
        throw new IllegalArgumentException("The current value is not number, multiply is not permit.");
    }

    private T divide(T a, T b) {
        return divide (a, b, false);
    }

    private T divide(T a, T b, boolean useDouble) {
        if (isInteger() && !useDouble) {
            long la = toLong(a);
            long lb = toLong(b);
            return toT(la / lb);
        } else if (isDecimal() || isInteger()) {
            double da = toDouble(a);
            double db = toDouble(b);
            return toT(da / db);
        }
        throw new IllegalArgumentException("The current value is not number, divide is not permit.");
    }

    private T mod(T a, T b) {
        return mod (a, b, false);
    }

    private T mod(T a, T b, boolean useDouble) {
        if (isInteger() && !useDouble) {
            long la = toLong(a);
            long lb = toLong(b);
            return toT(la % lb);
        } else if (isDecimal() || isInteger()) {
            double da = toDouble(a);
            double db = toDouble(b);
            return toT(da % db);
        }
        throw new IllegalArgumentException("The current value is not number, mod is not permit.");
    }

    private T leftShift(T a, T b) {
        if (isInteger()) {
            long la = toLong(a);
            long lb = toLong(b);
            return toT(la << lb);
        }
        throw new IllegalArgumentException("The current value is not integer, left shift is not permit.");
    }

    private T rightShift(T a, T b) {
        if (isInteger()) {
            long la = toLong(a);
            long lb = toLong(b);
            return toT(la >> lb);
        }
        throw new IllegalArgumentException("The current value is not integer, right shift is not permit.");
    }

    private T unsignedRightShift(T a, T b) {
        if (isInteger()) {
            long la = toLong(a);
            long lb = toLong(b);
            return toT(la >>> lb);
        }
        throw new IllegalArgumentException("The current value is not integer, unsigned right shift is not permit.");
    }

    private T logicAnd(T a, T b) {
        if (isInteger()) {
            long la = toLong(a);
            long lb = toLong(b);
            return toT(la & lb);
        }
        throw new IllegalArgumentException("The current value is not integer, logic and is not permit.");
    }

    private T logicOr(T a, T b) {
        if (isInteger()) {
            long la = toLong(a);
            long lb = toLong(b);
            return toT(la | lb);
        }
        throw new IllegalArgumentException("The current value is not integer, logic or is not permit.");
    }

    private T logicXor(T a, T b) {
        if (isInteger()) {
            long la = toLong(a);
            long lb = toLong(b);
            return toT(la ^ lb);
        }
        throw new IllegalArgumentException("The current value is not integer, logic xor is not permit.");
    }

    public T getValueAndIncrement() {
        assertNumber("post increment");
        T prev, next;
        do {
            prev = atomic.get();
            next = plus(prev, toT(1));
        } while (!atomic.compareAndSet(prev, next));
        return prev;
    }

    public T getValueAndDecrement() {
        assertNumber("post decrement");
        T prev, next;
        do {
            prev = atomic.get();
            next = minus(prev, toT(1));
        } while (!atomic.compareAndSet(prev, next));
        return prev;
    }

    public T incrementAndGetValue() {
        assertNumber("pre increment");
        T prev, next;
        do {
            prev = atomic.get();
            next = plus(prev, toT(1));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T decrementAndGetValue() {
        assertNumber("pre decrement");
        T prev, next;
        do {
            prev = atomic.get();
            next = minus(prev, toT(1));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T addAndGetValue(long v) {
        assertNumber("plus and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = plus(prev, toT(v));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T addAndGetValue(double v) {
        assertNumber("plus and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = plus(prev, toT(v), true);
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T addAndGetValue(Object v) {
        if (isString()) {
            T prev, next;
            do {
                prev = atomic.get();
                next = plus(prev, toT(v != null ? v.toString() : null));
            } while (!atomic.compareAndSet(prev, next));
            return next;
        } else {
            assertNumber("plus and assign");
            if (v == null) {
                throw new NullPointerException();
            }
            T arg;
            if (isInteger(v)) {
                arg = toT(castLong(v));
            } else if (isDecimal(v)) {
                arg = toT(castDouble(v));
            } else {
                throw new IllegalArgumentException("This is impossible.");
            }
            T prev, next;
            do {
                prev = atomic.get();
                next = plus(prev, arg);
            } while (!atomic.compareAndSet(prev, next));
            return next;
        }
    }

    public T minusAndGetValue(long v) {
        assertNumber("minus and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = minus(prev, toT(v));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T minusAndGetValue(double v) {
        assertNumber("minus and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = minus(prev, toT(v), true);
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T mulAndGetValue(long v) {
        assertNumber("multiply and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = multiply(prev, toT(v));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T mulAndGetValue(double v) {
        assertNumber("multiply and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = multiply(prev, toT(v), true);
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T divideAndGetValue(long v) {
        assertNumber("divide and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = divide(prev, toT(v));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T divideAndGetValue(double v) {
        assertNumber("divide and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = divide(prev, toT(v), true);
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T modAndGetValue(long v) {
        assertNumber("mod and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = mod(prev, toT(v));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T modAndGetValue(double v) {
        assertNumber("mod and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = mod(prev, toT(v), true);
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T leftShiftAndGetValue(long v) {
        assertNumber("left shift and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = leftShift(prev, toT(v));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T rightShiftAndGetValue(long v) {
        assertNumber("right shift and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = rightShift(prev, toT(v));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T unsignedRightShiftAndGetValue(long v) {
        assertNumber("unsigned right shift and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = unsignedRightShift(prev, toT(v));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T andAndGetValue(long v) {
        assertNumber("logic and and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = logicAnd(prev, toT(v));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T orAndGetValue(long v) {
        assertNumber("logic or and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = logicOr(prev, toT(v));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public T xorAndGetValue(long v) {
        assertNumber("logic xor and assign");
        T prev, next;
        do {
            prev = atomic.get();
            next = logicXor(prev, toT(v));
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

}
