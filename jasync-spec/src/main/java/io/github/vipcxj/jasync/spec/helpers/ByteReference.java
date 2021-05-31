package io.github.vipcxj.jasync.spec.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class ByteReference extends Number implements Comparable<ByteReference>, Serializable {

    private static final long serialVersionUID = 3058916963831990968L;
    private final AtomicInteger atomic;

    public ByteReference() {
        this((byte) 0);
    }

    public ByteReference(byte v) {
        this.atomic = new AtomicInteger(v);
    }

    public byte getValue() {
        return (byte) atomic.get();
    }

    public void setValue(byte v) {
        atomic.set(v);
    }

    public byte setAndGet(byte v) {
        setValue(v);
        return v;
    }

    public byte getValueAndIncrement() {
        return (byte) atomic.getAndIncrement();
    }

    public byte getValueAndDecrement() {
        return (byte) atomic.getAndDecrement();
    }

    public byte incrementAndGetValue() {
        return (byte) atomic.incrementAndGet();
    }

    public byte decrementAndGetValue() {
        return (byte) atomic.decrementAndGet();
    }

    public byte addAndGetValue(long v) {
        return (byte) atomic.addAndGet((int) v);
    }

    public byte addAndGetValue(double v) {
        return (byte) atomic.addAndGet((int) v);
    }

    public byte minusAndGetValue(long v) {
        return addAndGetValue(-v);
    }

    public byte minusAndGetValue(double v) {
        return addAndGetValue(-v);
    }

    public byte mulAndGetValue(long v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a * b);
    }

    public byte mulAndGetValue(double v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a * b);
    }

    public byte divideAndGetValue(long v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a / b);
    }

    public byte divideAndGetValue(double v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a / b);
    }

    public byte modAndGetValue(long v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a % b);
    }

    public byte modAndGetValue(double v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a % b);
    }

    public byte leftShiftAndGetValue(long v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a << b);
    }

    public byte rightShiftAndGetValue(long v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a >> b);
    }

    public byte unsignedRightShiftAndGetValue(long v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a >>> b);
    }

    public byte andAndGetValue(long v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a & b);
    }

    public byte orAndGetValue(long v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a | b);
    }

    public byte xorAndGetValue(long v) {
        return (byte) atomic.accumulateAndGet((int) v, (a, b) -> a ^ b);
    }

    @Override
    public byte byteValue() {
        return getValue();
    }

    @Override
    public short shortValue() {
        return getValue();
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

    @Override
    public int compareTo(ByteReference o) {
        return getValue() - o.getValue();
    }
}
