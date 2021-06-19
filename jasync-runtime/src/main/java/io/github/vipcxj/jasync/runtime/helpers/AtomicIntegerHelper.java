package io.github.vipcxj.jasync.runtime.helpers;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerHelper {

    public static float floatInc(AtomicInteger atomic, boolean post) {
        int prev, next;
        do {
            prev = atomic.get();
            next = Float.floatToRawIntBits(Float.intBitsToFloat(prev) + 1);
        } while (!atomic.compareAndSet(prev, next));
        return Float.intBitsToFloat(post ? prev : next);
    }

    public static float floatDec(AtomicInteger atomic, boolean post) {
        int prev, next;
        do {
            prev = atomic.get();
            next = Float.floatToRawIntBits(Float.intBitsToFloat(prev) - 1);
        } while (!atomic.compareAndSet(prev, next));
        return Float.intBitsToFloat(post ? prev : next);
    }

    public static float floatAdd(AtomicInteger atomic, float v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = Float.floatToRawIntBits(Float.intBitsToFloat(prev) + v);
        } while (!atomic.compareAndSet(prev, next));
        return Float.intBitsToFloat(next);
    }

    public static int mul(AtomicInteger atomic, long v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = prev * (int) v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static float floatMul(AtomicInteger atomic, float v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = Float.floatToRawIntBits(Float.intBitsToFloat(prev) * v);
        } while (!atomic.compareAndSet(prev, next));
        return Float.intBitsToFloat(next);
    }

    public static int div(AtomicInteger atomic, long v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = prev / (int) v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static float floatDiv(AtomicInteger atomic, float v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = Float.floatToRawIntBits(Float.intBitsToFloat(prev) / v);
        } while (!atomic.compareAndSet(prev, next));
        return Float.intBitsToFloat(next);
    }

    public static int mod(AtomicInteger atomic, long v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = prev % (int) v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static float floatMod(AtomicInteger atomic, float v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = Float.floatToRawIntBits(Float.intBitsToFloat(prev) % v);
        } while (!atomic.compareAndSet(prev, next));
        return Float.intBitsToFloat(next);
    }

    public static int leftShift(AtomicInteger atomic, long v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = prev << v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static int rightShift(AtomicInteger atomic, long v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = prev >> v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static int unsignedRightShift(AtomicInteger atomic, long v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = prev >>> v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static int and(AtomicInteger atomic, long v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = prev & (int) v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static int or(AtomicInteger atomic, long v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = prev | (int) v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }

    public static int xor(AtomicInteger atomic, long v) {
        int prev, next;
        do {
            prev = atomic.get();
            next = prev ^ (int) v;
        } while (!atomic.compareAndSet(prev, next));
        return next;
    }
}
