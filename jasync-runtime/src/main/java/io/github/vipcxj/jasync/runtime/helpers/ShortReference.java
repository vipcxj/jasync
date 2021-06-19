package io.github.vipcxj.jasync.runtime.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.vipcxj.jasync.runtime.helpers.AtomicIntegerHelper.*;

public class ShortReference extends Number implements Comparable<ShortReference>, Serializable {

    private static final long serialVersionUID = -688688459722646956L;
    private final AtomicInteger atomic;

    public ShortReference() {
        this((short) 0);
    }

    public ShortReference(short v) {
        this.atomic = new AtomicInteger(v);
    }

    public short getValue() {
        return (short) atomic.get();
    }

    public void setValue(short v) {
        atomic.set(v);
    }

    public short setAndGet(short v) {
        setValue(v);
        return v;
    }

    public short getValueAndIncrement() {
        return (short) atomic.getAndIncrement();
    }

    public short getValueAndDecrement() {
        return (short) atomic.getAndDecrement();
    }

    public short incrementAndGetValue() {
        return (short) atomic.incrementAndGet();
    }

    public short decrementAndGetValue() {
        return (short) atomic.decrementAndGet();
    }

    public short addAndGetValue(long v) {
        return (short) atomic.addAndGet((int) v);
    }

    public short addAndGetValue(double v) {
        return (short) atomic.addAndGet((int) v);
    }

    public short minusAndGetValue(long v) {
        return addAndGetValue(-v);
    }

    public short minusAndGetValue(double v) {
        return addAndGetValue(-v);
    }

    public short mulAndGetValue(long v) {
        return (short) mul(atomic, v);
    }

    public short mulAndGetValue(double v) {
        return (short) mul(atomic, (long) v);
    }

    public short divideAndGetValue(long v) {
        return (short) div(atomic, v);
    }

    public short divideAndGetValue(double v) {
        return (short) div(atomic, (long) v);
    }

    public short modAndGetValue(long v) {
        return (short) mod(atomic, v);
    }

    public short modAndGetValue(double v) {
        return (short) mod(atomic, (long) v);
    }

    public short leftShiftAndGetValue(long v) {
        return (short) leftShift(atomic, v);
    }

    public short rightShiftAndGetValue(long v) {
        return (short) rightShift(atomic, v);
    }

    public short unsignedRightShiftAndGetValue(long v) {
        return (short) unsignedRightShift(atomic, v);
    }

    public short andAndGetValue(long v) {
        return (short) and(atomic, v);
    }

    public short orAndGetValue(long v) {
        return (short) or(atomic, v);
    }

    public short xorAndGetValue(long v) {
        return (short) xor(atomic, v);
    }

    @Override
    public int compareTo(ShortReference o) {
        return getValue() - o.getValue();
    }

    @Override
    public byte byteValue() {
        return (byte) getValue();
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
}
