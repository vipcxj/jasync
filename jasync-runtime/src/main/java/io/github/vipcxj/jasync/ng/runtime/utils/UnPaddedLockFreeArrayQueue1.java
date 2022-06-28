package io.github.vipcxj.jasync.ng.runtime.utils;

import org.jctools.util.UnsafeAccess;

import static io.github.vipcxj.jasync.ng.runtime.utils.CommonUtils.normalCapacity;

public class UnPaddedLockFreeArrayQueue1<T> extends AbstractLockFreeArrayQueue<T> {

    private volatile int head;
    private static final long OFFSET_HEAD = UnsafeAccess.fieldOffset(UnPaddedLockFreeArrayQueue1.class, "head");
    private volatile int tail;
    private volatile int stTail;
    private static final int ST_TAIL_READY = 0;
    private static final int ST_TAIL_READ = 1;
    private static final int ST_TAIL_WRITE = 2;
    private static final int ST_TAIL_GROW = 3;
    private static final long OFFSET_STATE = UnsafeAccess.fieldOffset(UnPaddedLockFreeArrayQueue1.class, "stTail");
    private volatile Object[] elements;

    public UnPaddedLockFreeArrayQueue1(int initCapacity) {
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
        return UnsafeAccess.UNSAFE.compareAndSwapInt(this, OFFSET_HEAD, head, head + 1);
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
        stTail = ST_TAIL_READY;
    }

    @Override
    protected boolean tryStartRead() {
        return UnsafeAccess.UNSAFE.compareAndSwapInt(this, OFFSET_STATE, ST_TAIL_READY, ST_TAIL_READ);
    }

    @Override
    protected void endRead() {
        resetState();
    }

    @Override
    protected boolean tryStartWrite() {
        return UnsafeAccess.UNSAFE.compareAndSwapInt(this, OFFSET_STATE, ST_TAIL_READY, ST_TAIL_WRITE);
    }

    @Override
    protected void endWrite() {
        resetState();
    }

    @Override
    protected boolean tryStartGrow() {
        return UnsafeAccess.UNSAFE.compareAndSwapInt(this, OFFSET_STATE, ST_TAIL_READY, ST_TAIL_GROW);
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
