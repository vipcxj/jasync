package io.github.vipcxj.jasync.spec.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class LongReference extends Number implements Comparable<LongReference>, Serializable {

    private static final long serialVersionUID = -6475621842589727141L;
    private final AtomicLong atomic;

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
        return atomic.accumulateAndGet(v, (a, b) -> a * b);
    }

    public long mulAndGetValue(double v) {
        return atomic.accumulateAndGet((long) v, (a, b) -> a * b);
    }

    public long divideAndGetValue(long v) {
        return atomic.accumulateAndGet(v, (a, b) -> a / b);
    }

    public long divideAndGetValue(double v) {
        return atomic.accumulateAndGet((long) v, (a, b) -> a / b);
    }

    public long modAndGetValue(long v) {
        return atomic.accumulateAndGet(v, (a, b) -> a % b);
    }

    public long modAndGetValue(double v) {
        return atomic.accumulateAndGet((long) v, (a, b) -> a % b);
    }

    public long leftShiftAndGetValue(long v) {
        return atomic.accumulateAndGet(v, (a, b) -> a << b);
    }

    public long rightShiftAndGetValue(long v) {
        return atomic.accumulateAndGet(v, (a, b) -> a >> b);
    }

    public long unsignedRightShiftAndGetValue(long v) {
        return atomic.accumulateAndGet(v, (a, b) -> a >>> b);
    }

    public long andAndGetValue(long v) {
        return atomic.accumulateAndGet(v, (a, b) -> a & b);
    }

    public long orAndGetValue(long v) {
        return atomic.accumulateAndGet(v, (a, b) -> a | b);
    }

    public long xorAndGetValue(long v) {
        return atomic.accumulateAndGet(v, (a, b) -> a ^ b);
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
