package io.github.vipcxj.jasync.ng.runtime.utils;

import org.jctools.util.UnsafeAccess;

import static io.github.vipcxj.jasync.ng.runtime.utils.CommonUtils.normalCapacity;

@SuppressWarnings("unused")
abstract class LockFreeArrayQueuePad0<T> extends AbstractLockFreeArrayQueue<T> {
    byte b000,b001,b002,b003,b004,b005,b006,b007;//  8b
    byte b010,b011,b012,b013,b014,b015,b016,b017;// 16b
    byte b020,b021,b022,b023,b024,b025,b026,b027;// 24b
    byte b030,b031,b032,b033,b034,b035,b036,b037;// 32b
    byte b040,b041,b042,b043,b044,b045,b046,b047;// 40b
    byte b050,b051,b052,b053,b054,b055,b056,b057;// 48b
    byte b060,b061,b062,b063,b064,b065,b066,b067;// 56b
    byte b070,b071,b072,b073,b074,b075,b076,b077;// 64b
    byte b100,b101,b102,b103,b104,b105,b106,b107;// 72b
    byte b110,b111,b112,b113,b114,b115,b116,b117;// 80b
    byte b120,b121,b122,b123,b124,b125,b126,b127;// 88b
    byte b130,b131,b132,b133,b134,b135,b136,b137;// 96b
    byte b140,b141,b142,b143,b144,b145,b146,b147;//104b
    byte b150,b151,b152,b153,b154,b155,b156,b157;//112b
    byte b160,b161,b162,b163,b164,b165,b166,b167;//120b
    // byte b170,b171,b172,b173,b174,b175,b176,b177;//128b
    //    * drop 8b as object header acts as padding and is >= 8b *
}

abstract class LockFreeArrayQueueHead<T> extends LockFreeArrayQueuePad0<T> {
    volatile int head;
    private static final long OFFSET_HEAD = UnsafeAccess.fieldOffset(LockFreeArrayQueueHead.class, "head");

    @Override
    protected int head() {
        return head;
    }

    @Override
    protected boolean tryIncHead(int head) {
        return UnsafeAccess.UNSAFE.compareAndSwapInt(this, OFFSET_HEAD, head, head + 1);
    }
}

@SuppressWarnings("unused")
abstract class LockFreeArrayQueuePad1<T> extends LockFreeArrayQueueHead<T> {
    byte b000,b001,b002,b003,b004,b005,b006,b007;//  8b
    byte b010,b011,b012,b013,b014,b015,b016,b017;// 16b
    byte b020,b021,b022,b023,b024,b025,b026,b027;// 24b
    byte b030,b031,b032,b033,b034,b035,b036,b037;// 32b
    byte b040,b041,b042,b043,b044,b045,b046,b047;// 40b
    byte b050,b051,b052,b053,b054,b055,b056,b057;// 48b
    byte b060,b061,b062,b063,b064,b065,b066,b067;// 56b
    byte b070,b071,b072,b073,b074,b075,b076,b077;// 64b
    byte b100,b101,b102,b103,b104,b105,b106,b107;// 72b
    byte b110,b111,b112,b113,b114,b115,b116,b117;// 80b
    byte b120,b121,b122,b123,b124,b125,b126,b127;// 88b
    byte b130,b131,b132,b133,b134,b135,b136,b137;// 96b
    byte b140,b141,b142,b143,b144,b145,b146,b147;//104b
    byte b150,b151,b152,b153,b154,b155,b156,b157;//112b
    byte b160,b161,b162,b163,b164,b165,b166,b167;//120b
    byte b170,b171,b172,b173,b174,b175,b176,b177;//128b
}

abstract class LockFreeArrayQueueTail<T> extends LockFreeArrayQueuePad1<T> {
    volatile int tail;

    @Override
    protected int tail() {
        return tail;
    }

    @Override
    protected int getAndIncTail() {
        //noinspection NonAtomicOperationOnVolatileField
        return tail++;
    }
}

@SuppressWarnings("unused")
abstract class LockFreeArrayQueuePad2<T> extends LockFreeArrayQueueTail<T> {
    byte b000,b001,b002,b003,b004,b005,b006,b007;//  8b
    byte b010,b011,b012,b013,b014,b015,b016,b017;// 16b
    byte b020,b021,b022,b023,b024,b025,b026,b027;// 24b
    byte b030,b031,b032,b033,b034,b035,b036,b037;// 32b
    byte b040,b041,b042,b043,b044,b045,b046,b047;// 40b
    byte b050,b051,b052,b053,b054,b055,b056,b057;// 48b
    byte b060,b061,b062,b063,b064,b065,b066,b067;// 56b
    byte b070,b071,b072,b073,b074,b075,b076,b077;// 64b
    byte b100,b101,b102,b103,b104,b105,b106,b107;// 72b
    byte b110,b111,b112,b113,b114,b115,b116,b117;// 80b
    byte b120,b121,b122,b123,b124,b125,b126,b127;// 88b
    byte b130,b131,b132,b133,b134,b135,b136,b137;// 96b
    byte b140,b141,b142,b143,b144,b145,b146,b147;//104b
    byte b150,b151,b152,b153,b154,b155,b156,b157;//112b
    byte b160,b161,b162,b163,b164,b165,b166,b167;//120b
    byte b170,b171,b172,b173,b174,b175,b176,b177;//128b
}

abstract class LockFreeArrayQueueState<T> extends LockFreeArrayQueuePad2<T> {
    volatile int stTail;
    private static final int ST_TAIL_READY = 0;
    private static final int ST_TAIL_READ = 1;
    private static final int ST_TAIL_WRITE = 2;
    private static final int ST_TAIL_GROW = 3;
    private static final long OFFSET_STATE = UnsafeAccess.fieldOffset(LockFreeArrayQueueState.class, "stTail");

    @Override
    protected void resetHeadAndTail() {
        head = tail = 0;
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

@SuppressWarnings("unused")
abstract class LockFreeArrayQueuePad3<T> extends LockFreeArrayQueueState<T> {
    byte b000,b001,b002,b003,b004,b005,b006,b007;//  8b
    byte b010,b011,b012,b013,b014,b015,b016,b017;// 16b
    byte b020,b021,b022,b023,b024,b025,b026,b027;// 24b
    byte b030,b031,b032,b033,b034,b035,b036,b037;// 32b
    byte b040,b041,b042,b043,b044,b045,b046,b047;// 40b
    byte b050,b051,b052,b053,b054,b055,b056,b057;// 48b
    byte b060,b061,b062,b063,b064,b065,b066,b067;// 56b
    byte b070,b071,b072,b073,b074,b075,b076,b077;// 64b
    byte b100,b101,b102,b103,b104,b105,b106,b107;// 72b
    byte b110,b111,b112,b113,b114,b115,b116,b117;// 80b
    byte b120,b121,b122,b123,b124,b125,b126,b127;// 88b
    byte b130,b131,b132,b133,b134,b135,b136,b137;// 96b
    byte b140,b141,b142,b143,b144,b145,b146,b147;//104b
    byte b150,b151,b152,b153,b154,b155,b156,b157;//112b
    byte b160,b161,b162,b163,b164,b165,b166,b167;//120b
    byte b170,b171,b172,b173,b174,b175,b176,b177;//128b
}

public class PaddedLockFreeArrayQueue<T> extends LockFreeArrayQueuePad3<T> {

    volatile Object[] elements;

    public PaddedLockFreeArrayQueue(int initCapacity) {
        int capacity = normalCapacity(initCapacity);
        this.elements = new Object[capacity];
        this.head = this.tail = 0;
        this.stTail = 0;
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
}
