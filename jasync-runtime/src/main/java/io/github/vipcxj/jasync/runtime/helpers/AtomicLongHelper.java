package io.github.vipcxj.jasync.runtime.helpers;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongHelper {

    public static double doubleInc(AtomicLong atomic, boolean post) {
        long prev, next;
        do {
            prev = atomic.get();
            next = Double.doubleToRawLongBits(Double.longBitsToDouble(prev) + 1);
        } while (!atomic.compareAndSet(prev, next));
        return Double.doubleToRawLongBits(post ? prev : next);
    }

    public static double doubleDec(AtomicLong atomic, boolean post) {
        long prev, next;
        do {
            prev = atomic.get();
            next = Double.doubleToRawLongBits(Double.longBitsToDouble(prev) - 1);
        } while (!atomic.compareAndSet(prev, next));
        return Double.doubleToRawLongBits(post ? prev : next);
    }

    public static double doubleAdd(AtomicLong atomic, double v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = Double.doubleToRawLongBits(Double.longBitsToDouble(prev) + v);
        } while (!atomic.compareAndSet(prev, next));
        return Double.longBitsToDouble(next);
    }

    public static long mul(AtomicLong atomic, long v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = prev * v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static double doubleMul(AtomicLong atomic, double v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = Double.doubleToRawLongBits(Double.longBitsToDouble(prev) * v);
        } while (!atomic.compareAndSet(prev, next));
        return Double.longBitsToDouble(next);
    }

    public static long div(AtomicLong atomic, long v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = prev / v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static double doubleDiv(AtomicLong atomic, double v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = Double.doubleToRawLongBits(Double.longBitsToDouble(prev) / v);
        } while (!atomic.compareAndSet(prev, next));
        return Double.longBitsToDouble(next);
    }

    public static long mod(AtomicLong atomic, long v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = prev % v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static double doubleMod(AtomicLong atomic, double v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = Double.doubleToRawLongBits(Double.longBitsToDouble(prev) % v);
        } while (!atomic.compareAndSet(prev, next));
        return Double.longBitsToDouble(next);
    }

    public static long leftShift(AtomicLong atomic, long v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = prev << v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static long rightShift(AtomicLong atomic, long v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = prev >> v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static long unsignedRightShift(AtomicLong atomic, long v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = prev >>> v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static long and(AtomicLong atomic, long v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = prev & v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static long or(AtomicLong atomic, long v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = prev | v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static long xor(AtomicLong atomic, long v) {
        long prev, next;
        do {
            prev = atomic.get();
            next = prev ^ v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }
}
