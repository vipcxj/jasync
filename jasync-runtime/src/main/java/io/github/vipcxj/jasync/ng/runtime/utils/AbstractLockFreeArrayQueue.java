package io.github.vipcxj.jasync.ng.runtime.utils;

import java.util.AbstractQueue;
import java.util.Iterator;

import static io.github.vipcxj.jasync.ng.runtime.utils.CommonUtils.normalCapacity;

public abstract class AbstractLockFreeArrayQueue<T> extends AbstractQueue<T> implements AutoCapacityQueue<T> {

    protected abstract int head();
    protected abstract boolean tryIncHead(int head);
    protected abstract int tail();
    protected abstract int getAndIncTail();
    protected abstract void resetHeadAndTail();
    protected abstract Object[] elements();
    protected abstract void elements(Object[] array);
    protected abstract boolean tryStartRead();
    protected abstract void endRead();
    protected abstract boolean tryStartWrite();
    protected abstract void endWrite();
    protected abstract boolean tryStartGrow();
    protected abstract void endGrow();
    protected abstract boolean isStateReady();
    protected abstract boolean isGrowing();

    @Override
    public int size() {
        return tail() - head();
    }

    @Override
    public boolean offer(T t) {
        while (true) {
            int capacity = capacity();
            if (tail() - head() == capacity) {
                if (!grow(capacity)) {
                    if (isGrowing()) {
                        Thread.yield();
                    }
                    continue;
                }
            }
            if (isStateReady() && tryStartWrite()) {
                try {
                    capacity = capacity();
                    if (tail() - head() == capacity) {
                        continue;
                    }
                    elements()[pos(getAndIncTail())] = t;
                    return true;
                } finally {
                    endWrite();
                }
            }
        }
    }

    private int pos(int value) {
        return value & (capacity() - 1);
    }

    private boolean grow(int capacity) {
        if (capacity == Integer.MAX_VALUE) {
            throw new RuntimeException("Unable to grow the capacity. Because the capacity has reached to the max value: " + capacity);
        }
        if (isStateReady() && tryStartGrow()) {
            try {
                if (capacity() != capacity) {
                    return false;
                }
                Object[] elements = elements();
                Object[] newElements = new Object[capacity << 1];
                System.arraycopy(elements, 0, newElements, 0, capacity);
                System.arraycopy(elements, 0, newElements, capacity, capacity);
                elements(newElements);
                return true;
            } finally {
                endGrow();
            }
        } else {
            return false;
        }
    }

    @Override
    public T poll() {
        while (true) {
            int head = head();
            int tail = tail();
            if (head == tail) {
                return null;
            }
            if (head < tail - 1) {
                // The elements only grow. The pos must gotten here, or may cause indexoutofbound error. because elements[pos(head)] push elements first, then call pos(head)
                int pos = pos(head);
                //noinspection unchecked
                T v = (T) elements()[pos];
                if (head == head() && tryIncHead(head)) {
                    return v;
                }
            } else if (isStateReady() && tryStartRead()) {
                try {
                    if (tryIncHead(head)) {
                        //noinspection unchecked
                        return (T) elements()[pos(head)];
                    }
                } finally {
                    endRead();
                }
            }
        }
    }

    @Override
    public T peek() {
        while (true) {
            int head = head();
            if (head == tail()) {
                return null;
            }
            //noinspection unchecked
            T value = (T) elements()[pos(head)];
            if (head == head()) {
                return value;
            }
        }
    }

    /**
     * Get the element at index.
     * Just for test.
     * @param index the pos of element to get.
     * @return the target element.
     */
    T get(int index) {
        //noinspection unchecked
        return (T) elements()[index];
    }

    @Override
    public void clear(int maxCapacity) {
        maxCapacity = normalCapacity(maxCapacity);
        while (true) {
            if (tryStartWrite()) {
                try {
                    if (capacity() > maxCapacity) {
                        elements(new Object[maxCapacity]);
                    }
                    resetHeadAndTail();
                } finally {
                    endWrite();
                }
                return;
            }
        }
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }
}
