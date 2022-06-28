package io.github.vipcxj.jasync.ng.runtime.utils;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static io.github.vipcxj.jasync.ng.runtime.utils.CommonUtils.normalCapacity;

public class UnPaddedLockFreeArrayQueue0<T> extends AbstractLockFreeArrayQueue<T> {

    private volatile int head;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<UnPaddedLockFreeArrayQueue0> HEAD = AtomicIntegerFieldUpdater.newUpdater(UnPaddedLockFreeArrayQueue0.class, "head");
    private volatile int tail;
    private volatile int stTail;
    private static final int ST_TAIL_READY = 0;
    private static final int ST_TAIL_READ = 1;
    private static final int ST_TAIL_WRITE = 2;
    private static final int ST_TAIL_GROW = 3;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<UnPaddedLockFreeArrayQueue0> ST_TAIL = AtomicIntegerFieldUpdater.newUpdater(UnPaddedLockFreeArrayQueue0.class, "stTail");
    private volatile Object[] elements;

    public UnPaddedLockFreeArrayQueue0(int initCapacity) {
        int capacity = normalCapacity(initCapacity);
        this.elements = new Object[capacity];
        this.head = this.tail = 0;
        this.stTail = ST_TAIL_READY;
    }

    @Override
    protected int head() {
        return head;
    }

    @Override
    protected boolean tryIncHead(int head) {
        return HEAD.weakCompareAndSet(this, head, head + 1);
    }

    @Override
    protected int tail() {
        return tail;
    }

    @Override
    protected int getAndIncTail() {
        //noinspection NonAtomicOperationOnVolatileField
        return tail++;
    }

    @Override
    protected void resetHeadAndTail() {
        head = tail = 0;
    }

    @Override
    protected Object[] elements() {
        return elements;
    }

    @Override
    protected void elements(Object[] array) {
        elements = array;
    }

    @Override
    public int capacity() {
        return elements.length;
    }

    private void resetState() {
        ST_TAIL.set(this, ST_TAIL_READY);
    }

    @Override
    protected boolean tryStartRead() {
        return ST_TAIL.weakCompareAndSet(this, ST_TAIL_READY, ST_TAIL_READ);
    }

    @Override
    protected void endRead() {
        resetState();
    }

    @Override
    protected boolean tryStartWrite() {
        return ST_TAIL.weakCompareAndSet(this, ST_TAIL_READY, ST_TAIL_WRITE);
    }

    @Override
    protected void endWrite() {
        resetState();
    }

    @Override
    protected boolean tryStartGrow() {
        return ST_TAIL.weakCompareAndSet(this, ST_TAIL_READY, ST_TAIL_GROW);
    }

    @Override
    protected void endGrow() {
        resetState();
    }

    @Override
    protected boolean isStateReady() {
        return stTail == ST_TAIL_READY;
    }

    @Override
    protected boolean isGrowing() {
        return stTail == ST_TAIL_GROW;
    }
}
