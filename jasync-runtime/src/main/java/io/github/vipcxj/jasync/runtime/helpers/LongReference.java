package io.github.vipcxj.jasync.runtime.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.vipcxj.jasync.runtime.helpers.AtomicLongHelper.*;

public class LongReference extends Number implements Comparable<LongReference>, Serializable {

    private static final long serialVersionUID = -6475621842589727141L;
    protected final AtomicLong atomic;

    public LongReference() {
        this(0L);
    }

    public LongReference(long v) {
        this.atomic = new AtomicLong(v);
    }

    public long getValue() {
        return atomic.get();
    }

    public void setValue(long v) {
        atomic.set(v);
    }

    public long setAndGet(long v) {
        setValue(v);
        return v;
    }

    public long getValueAndIncrement() {
        return atomic.getAndIncrement();
    }

    public long getValueAndDecrement() {
        return atomic.getAndDecrement();
    }

    public long incrementAndGetValue() {
        return atomic.incrementAndGet();
    }

    public long decrementAndGetValue() {
        return atomic.decrementAndGet();
    }

    public long addAndGetValue(long v) {
        return atomic.addAndGet(v);
    }

    public long addAndGetValue(double v) {
        return atomic.addAndGet((long) v);
    }

    public long minusAndGetValue(long v) {
        return addAndGetValue(-v);
    }

    public long minusAndGetValue(double v) {
        return addAndGetValue(-v);
    }

    public long mulAndGetValue(long v) {
        return mul(atomic, v);
    }

    public long mulAndGetValue(double v) {
        return mul(atomic, (long) v);
    }

    public long divideAndGetValue(long v) {
        return div(atomic, v);
    }

    public long divideAndGetValue(double v) {
        return div(atomic, (long) v);
    }

    public long modAndGetValue(long v) {
        return mod(atomic, v);
    }

    public long modAndGetValue(double v) {
        return mod(atomic, (long) v);
    }

    public long leftShiftAndGetValue(long v) {
        return leftShift(atomic, v);
    }

    public long rightShiftAndGetValue(long v) {
        return rightShift(atomic, v);
    }

    public long unsignedRightShiftAndGetValue(long v) {
        return unsignedRightShift(atomic, v);
    }

    public long andAndGetValue(long v) {
        return and(atomic, v);
    }

    public long orAndGetValue(long v) {
        return or(atomic, v);
    }

    public long xorAndGetValue(long v) {
        return xor(atomic, v);
    }

    @Override
    public int compareTo(LongReference o) {
        long x = getValue();
        long y = o.getValue();
        //noinspection UseCompareMethod
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    @Override
    public int intValue() {
        return (int) getValue();
    }

    @Override
    public long longValue() {
        return getValue();
    }

    @Override
    public float floatValue() {
        return getValue();
    }

    @Override
    public double doubleValue() {
        return getValue();
    }
}
