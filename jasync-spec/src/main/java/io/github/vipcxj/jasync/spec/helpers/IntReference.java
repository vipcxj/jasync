package io.github.vipcxj.jasync.spec.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class IntReference extends Number implements Comparable<IntReference>, Serializable {

    private static final long serialVersionUID = 7912976749548589491L;
    private final AtomicInteger atomic;

    public IntReference() {
        this(0);
    }

    public IntReference(int v) {
        this.atomic = new AtomicInteger(v);
    }

    public int getValue() {
        return atomic.get();
    }

    public void setValue(int v) {
        atomic.set(v);
    }

    public int setAndGet(int v) {
        setValue(v);
        return v;
    }

    public int getValueAndIncrement() {
        return atomic.getAndIncrement();
    }

    public int getValueAndDecrement() {
        return atomic.getAndDecrement();
    }

    public int incrementAndGetValue() {
        return atomic.incrementAndGet();
    }

    public int decrementAndGetValue() {
        return atomic.decrementAndGet();
    }

    public int addAndGetValue(long v) {
        return atomic.addAndGet((int) v);
    }

    public int addAndGetValue(double v) {
        return atomic.addAndGet((int) v);
    }

    public int minusAndGetValue(long v) {
        return addAndGetValue(-v);
    }

    public int minusAndGetValue(double v) {
        return addAndGetValue(-v);
    }

    public int mulAndGetValue(long v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a * b);
    }

    public int mulAndGetValue(double v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a * b);
    }

    public int divideAndGetValue(long v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a / b);
    }

    public int divideAndGetValue(double v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a / b);
    }

    public int modAndGetValue(long v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a % b);
    }

    public int modAndGetValue(double v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a % b);
    }

    public int leftShiftAndGetValue(long v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a << b);
    }

    public int rightShiftAndGetValue(long v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a >> b);
    }

    public int unsignedRightShiftAndGetValue(long v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a >>> b);
    }

    public int andAndGetValue(long v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a & b);
    }

    public int orAndGetValue(long v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a | b);
    }

    public int xorAndGetValue(long v) {
        return atomic.accumulateAndGet((int) v, (a, b) -> a ^ b);
    }

    @Override
    public int compareTo(IntReference o) {
        return getValue() - o.getValue();
    }

    @Override
    public int intValue() {
        return getValue();
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
