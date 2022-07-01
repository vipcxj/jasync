package io.github.vipcxj.jasync.ng.runtime.context;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

public class ArrayStack<T> implements Iterable<T> {
    private Object[] array;
    private int top;
    public ArrayStack(int initCapacity) {
        this.array = new Object[initCapacity];
        this.top = -1;
    }
    private ArrayStack(ArrayStack<T> stack) {
        this.array = stack.array.clone();
        this.top = stack.top;
    }

    public ArrayStack<T> copy() {
        return new ArrayStack<>(this);
    }

    public void push(T value) {
        ensureSize(++this.top);
        array[top] = value;
    }

    public T top() {
        if (top < 0) {
            throw new IllegalStateException("The stack is empty, the top operation is not possible.");
        }
        //noinspection unchecked
        return (T) array[top];
    }

    @SuppressWarnings("UnusedReturnValue")
    public T pop() {
        if (top < 0) {
            throw new IllegalStateException("The stack is empty, the pop operation is not possible.");
        }
        //noinspection unchecked
        return (T) array[top--];
    }

    private void ensureSize(int size) {
        if (size >= array.length) {
            Object[] newArray = new Object[(int) (size * 1.5)];
            System.arraycopy(array, 0, newArray, 0, array.length);
            array = newArray;
        }
    }

    public Stream<T> stream() {
        //noinspection unchecked
        return (Stream<T>) Arrays.stream(array).limit(top + 1);
    }

    @Override
    public Iterator<T> iterator() {
        return new TheIterator();
    }

    class TheIterator implements Iterator<T> {

        private int i = 0;

        @Override
        public boolean hasNext() {
            return i <= top;
        }

        @Override
        public T next() {
            //noinspection unchecked
            return (T) array[i++];
        }
    }
}
