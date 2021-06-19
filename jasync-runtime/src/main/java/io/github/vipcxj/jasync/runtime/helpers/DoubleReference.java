package io.github.vipcxj.jasync.runtime.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.vipcxj.jasync.runtime.helpers.AtomicLongHelper.*;

public class DoubleReference extends Number implements Comparable<DoubleReference>, Serializable {

    private static final long serialVersionUID = 4411304060598953199L;
    private final AtomicLong bits;

    public DoubleReference() {
        this(0.0);
    }

    public DoubleReference(double v) {
        bits = new AtomicLong(Double.doubleToRawLongBits(v));
    }

    public double getValue() {
        return Double.longBitsToDouble(bits.get());
    }

    public void setValue(double v) {
        bits.set(Double.doubleToRawLongBits(v));
    }

    public double setAndGet(double v) {
        setValue(v);
        return v;
    }

    public double getValueAndIncrement() {
        return doubleInc(bits, true);
    }

    public double getValueAndDecrement() {
        return doubleDec(bits, true);
    }

    public double incrementAndGetValue() {
        return doubleInc(bits, false);
    }

    public double decrementAndGetValue() {
        return doubleDec(bits, false);
    }

    public double addAndGetValue(double v) {
        return doubleAdd(bits, v);
    }

    public double minusAndGetValue(double v) {
        return addAndGetValue(-v);
    }

    public double mulAndGetValue(double v) {
        return doubleMul(bits, v);
    }

    public double divideAndGetValue(double v) {
        return doubleDiv(bits, v);
    }

    public double modAndGetValue(double v) {
        return doubleMod(bits, v);
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
        return (float) getValue();
    }

    @Override
    public double doubleValue() {
        return getValue();
    }

    @Override
    public int compareTo(DoubleReference o) {
        return Double.compare(getValue(), o.getValue());
    }
}
