package io.github.vipcxj.jasync.runtime.helpers;

import java.util.Iterator;

public class ArrayIterator<E> implements Iterator<E> {

    private final E[] array;
    private final int length;
    private int index;

    public ArrayIterator(E[] array) {
        this.array = array;
        this.length = array.length;
        this.index = 0;
    }

    @Override
    public boolean hasNext() {
        return index < length;
    }

    @Override
    public E next() {
        return array[index++];
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
