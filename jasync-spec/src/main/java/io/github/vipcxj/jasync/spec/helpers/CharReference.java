package io.github.vipcxj.jasync.spec.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class CharReference implements Comparable<CharReference>, Serializable {

    private static final long serialVersionUID = -1390365638204487256L;
    private final AtomicInteger atomic;

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
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a * b);
    }

    public char mulAndGetValue(double v) {
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a * b);
    }

    public char divideAndGetValue(long v) {
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a / b);
    }

    public char divideAndGetValue(double v) {
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a / b);
    }

    public char modAndGetValue(long v) {
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a % b);
    }

    public char modAndGetValue(double v) {
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a % b);
    }

    public char leftShiftAndGetValue(long v) {
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a << b);
    }

    public char rightShiftAndGetValue(long v) {
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a >> b);
    }

    public char unsignedRightShiftAndGetValue(long v) {
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a >>> b);
    }

    public char andAndGetValue(long v) {
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a & b);
    }

    public char orAndGetValue(long v) {
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a | b);
    }

    public char xorAndGetValue(long v) {
        return (char) atomic.accumulateAndGet((int) v, (a, b) -> a ^ b);
    }

    @Override
    public int compareTo(CharReference o) {
        return getValue() - o.getValue();
    }
}
