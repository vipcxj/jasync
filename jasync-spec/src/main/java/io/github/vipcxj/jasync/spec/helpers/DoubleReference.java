package io.github.vipcxj.jasync.spec.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

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
        return Double.longBitsToDouble(
                bits.getAndAccumulate(1, (a, b) -> Double.doubleToRawLongBits(Double.longBitsToDouble(a) + b))
        );
    }

    public double getValueAndDecrement() {
        return Double.longBitsToDouble(
                bits.getAndAccumulate(1, (a, b) -> Double.doubleToRawLongBits(Double.longBitsToDouble(a) - b))
        );
    }

    public double incrementAndGetValue() {
        return Double.longBitsToDouble(
                bits.accumulateAndGet(1, (a, b) -> Double.doubleToRawLongBits(Double.longBitsToDouble(a) + b))
        );
    }

    public double decrementAndGetValue() {
        return Double.longBitsToDouble(
                bits.accumulateAndGet(1, (a, b) -> Double.doubleToRawLongBits(Double.longBitsToDouble(a) - b))
        );
    }

    public double addAndGetValue(double v) {
        return Double.longBitsToDouble(
                bits.accumulateAndGet(
                        Double.doubleToRawLongBits(v),
                        (a, b) -> Double.doubleToRawLongBits(Double.longBitsToDouble(a) + Double.longBitsToDouble(b))
                )
        );
    }

    public double minusAndGetValue(double v) {
        return addAndGetValue(-v);
    }

    public double mulAndGetValue(double v) {
        return Double.longBitsToDouble(
                bits.accumulateAndGet(
                        Double.doubleToRawLongBits(v),
                        (a, b) -> Double.doubleToRawLongBits(Double.longBitsToDouble(a) * Double.longBitsToDouble(b))
                )
        );
    }

    public double divideAndGetValue(double v) {
        return Double.longBitsToDouble(
                bits.accumulateAndGet(
                        Double.doubleToRawLongBits(v),
                        (a, b) -> Double.doubleToRawLongBits(Double.longBitsToDouble(a) / Double.longBitsToDouble(b))
                )
        );
    }

    public double modAndGetValue(double v) {
        return Double.longBitsToDouble(
                bits.accumulateAndGet(
                        Double.doubleToRawLongBits(v),
                        (a, b) -> Double.doubleToRawLongBits(Double.longBitsToDouble(a) % Double.longBitsToDouble(b))
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
