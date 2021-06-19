package io.github.vipcxj.jasync.runtime.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.vipcxj.jasync.runtime.helpers.AtomicIntegerHelper.*;

public class CharReference implements Comparable<CharReference>, Serializable {

    private static final long serialVersionUID = -1390365638204487256L;
    protected final AtomicInteger atomic;

    public CharReference() {
        this('\u0000');
    }

    public CharReference(char value) {
        this.atomic = new AtomicInteger(value);
    }

    public char getValue() {
        return (char) atomic.get();
    }

    public void setValue(char v) {
        atomic.set(v);
    }

    public char setAndGet(char v) {
        setValue(v);
        return v;
    }

    public char getValueAndIncrement() {
        return (char) atomic.getAndIncrement();
    }

    public char getValueAndDecrement() {
        return (char) atomic.getAndDecrement();
    }

    public char incrementAndGetValue() {
        return (char) atomic.incrementAndGet();
    }

    public char decrementAndGetValue() {
        return (char) atomic.decrementAndGet();
    }

    public char addAndGetValue(long v) {
        return (char) atomic.addAndGet((int) v);
    }

    public char addAndGetValue(double v) {
        return (char) atomic.addAndGet((int) v);
    }

    public char minusAndGetValue(long v) {
        return addAndGetValue(-v);
    }

    public char minusAndGetValue(double v) {
        return addAndGetValue(-v);
    }

    public char mulAndGetValue(long v) {
        return (char) mul(atomic, v);
    }

    public char mulAndGetValue(double v) {
        return (char) mul(atomic, (long) v);
    }

    public char divideAndGetValue(long v) {
        return (char) div(atomic, v);
    }

    public char divideAndGetValue(double v) {
        return (char) div(atomic, (long) v);
    }

    public char modAndGetValue(long v) {
        return (char) mod(atomic, v);
    }

    public char modAndGetValue(double v) {
        return (char) mod(atomic, (long) v);
    }

    public char leftShiftAndGetValue(long v) {
        return (char) leftShift(atomic, v);
    }

    public char rightShiftAndGetValue(long v) {
        return (char) rightShift(atomic, v);
    }

    public char unsignedRightShiftAndGetValue(long v) {
        return (char) unsignedRightShift(atomic, v);
    }

    public char andAndGetValue(long v) {
        return (char) and(atomic, v);
    }

    public char orAndGetValue(long v) {
        return (char) or(atomic, v);
    }

    public char xorAndGetValue(long v) {
        return (char) xor(atomic, v);
    }

    @Override
    public int compareTo(CharReference o) {
        return getValue() - o.getValue();
    }
}
