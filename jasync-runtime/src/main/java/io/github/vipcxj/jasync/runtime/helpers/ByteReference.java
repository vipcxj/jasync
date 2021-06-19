package io.github.vipcxj.jasync.runtime.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.vipcxj.jasync.runtime.helpers.AtomicIntegerHelper.*;

public class ByteReference extends Number implements Comparable<ByteReference>, Serializable {

    private static final long serialVersionUID = 3058916963831990968L;
    protected final AtomicInteger atomic;

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
        return (byte) mul(atomic, (int) v);
    }

    public byte mulAndGetValue(double v) {
        return (byte) mul(atomic, (int) v);
    }

    public byte divideAndGetValue(long v) {
        return (byte) div(atomic, (int) v);
    }

    public byte divideAndGetValue(double v) {
        return (byte) div(atomic, (int) v);
    }

    public byte modAndGetValue(long v) {
        return (byte) mod(atomic, (int) v);
    }

    public byte modAndGetValue(double v) {
        return (byte) mod(atomic, (int) v);
    }

    public byte leftShiftAndGetValue(long v) {
        return (byte) leftShift(atomic, v);
    }

    public byte rightShiftAndGetValue(long v) {
        return (byte) rightShift(atomic, v);
    }

    public byte unsignedRightShiftAndGetValue(long v) {
        return (byte) unsignedRightShift(atomic, v);
    }

    public byte andAndGetValue(long v) {
        return (byte) and(atomic, v);
    }

    public byte orAndGetValue(long v) {
        return (byte) or(atomic, v);
    }

    public byte xorAndGetValue(long v) {
        return (byte) xor(atomic, v);
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
