package io.github.vipcxj.jasync.spec.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

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
        return Float.intBitsToFloat(
                bits.getAndAccumulate(1, (a, b) -> Float.floatToRawIntBits(Float.intBitsToFloat(a) + b))
        );
    }

    public float getValueAndDecrement() {
        return Float.intBitsToFloat(
                bits.getAndAccumulate(1, (a, b) -> Float.floatToRawIntBits(Float.intBitsToFloat(a) - b))
        );
    }

    public float incrementAndGetValue() {
        return Float.intBitsToFloat(
                bits.accumulateAndGet(1, (a, b) -> Float.floatToRawIntBits(Float.intBitsToFloat(a) + b))
        );
    }

    public float decrementAndGetValue() {
        return Float.intBitsToFloat(
                bits.accumulateAndGet(1, (a, b) -> Float.floatToRawIntBits(Float.intBitsToFloat(a) - b))
        );
    }

    public float addAndGetValue(double v) {
        return Float.intBitsToFloat(
                bits.accumulateAndGet(
                        Float.floatToRawIntBits((float) v),
                        (a, b) -> Float.floatToRawIntBits(Float.intBitsToFloat(a) + Float.intBitsToFloat(b))
                )
        );
    }

    public float minusAndGetValue(double v) {
        return addAndGetValue(-v);
    }

    public float mulAndGetValue(double v) {
        return Float.intBitsToFloat(
                bits.accumulateAndGet(
                        Float.floatToRawIntBits((float) v),
                        (a, b) -> Float.floatToRawIntBits(Float.intBitsToFloat(a) * Float.intBitsToFloat(b))
                )
        );
    }

    public float divideAndGetValue(double v) {
        return Float.intBitsToFloat(
                bits.accumulateAndGet(
                        Float.floatToRawIntBits((float) v),
                        (a, b) -> Float.floatToRawIntBits(Float.intBitsToFloat(a) / Float.intBitsToFloat(b))
                )
        );
    }

    public float modAndGetValue(double v) {
        return Float.intBitsToFloat(
                bits.accumulateAndGet(
                        Float.floatToRawIntBits((float) v),
                        (a, b) -> Float.floatToRawIntBits(Float.intBitsToFloat(a) % Float.intBitsToFloat(b))
                )
        );
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
