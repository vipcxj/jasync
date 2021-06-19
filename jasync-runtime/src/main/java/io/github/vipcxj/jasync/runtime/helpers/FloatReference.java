package io.github.vipcxj.jasync.runtime.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.vipcxj.jasync.runtime.helpers.AtomicIntegerHelper.*;

public class FloatReference extends Number implements Comparable<FloatReference>, Serializable {

    private static final long serialVersionUID = -1122333854311216852L;
    private final AtomicInteger bits;

    public FloatReference() {
        this(0.0f);
    }

    public FloatReference(float v) {
        bits = new AtomicInteger(Float.floatToRawIntBits(v));
    }

    public float getValue() {
        return Float.intBitsToFloat(bits.get());
    }

    public void setValue(float v) {
        bits.set(Float.floatToRawIntBits(v));
    }

    public float setAndGet(float v) {
        setValue(v);
        return v;
    }

    public float getValueAndIncrement() {
        return floatInc(bits, true);
    }

    public float getValueAndDecrement() {
        return floatDec(bits, true);
    }

    public float incrementAndGetValue() {
        return floatInc(bits, false);
    }

    public float decrementAndGetValue() {
        return floatDec(bits, false);
    }

    public float addAndGetValue(double v) {
        return floatAdd(bits, (float) v);
    }

    public float minusAndGetValue(double v) {
        return addAndGetValue(-v);
    }

    public float mulAndGetValue(double v) {
        return floatMul(bits, (float) v);
    }

    public float divideAndGetValue(double v) {
        return floatDiv(bits, (float) v);
    }

    public float modAndGetValue(double v) {
        return floatMod(bits, (float) v);
    }

    @Override
    public int intValue() {
        return (int) getValue();
    }

    @Override
    public long longValue() {
        return (long) getValue();
    }

    @Override
    public float floatValue() {
        return getValue();
    }

    @Override
    public double doubleValue() {
        return getValue();
    }

    @Override
    public int compareTo(FloatReference o) {
        return Float.compare(getValue(), o.getValue());
    }
}
