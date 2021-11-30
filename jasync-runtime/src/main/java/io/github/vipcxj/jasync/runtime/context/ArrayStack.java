package io.github.vipcxj.jasync.runtime.context;

public abstract class ArrayStack<T> {
    private T[] array;
    private int top;
    public ArrayStack() {
        this.array = createArray(0);
        this.top = -1;
    }

    protected abstract T[] createArray(int size);

    public void push(T value) {
        ensureSize(++this.top);
        array[top] = value;
    }

    public T pop() {
        if (top < 0) {
            throw new IllegalStateException("The stack is empty, the pop operation is not possible.");
        }
        return array[top--];
    }

    private void ensureSize(int size) {
        if (size >= 0 && array.length == 0) {
            array = createArray(10);
        } else if (size >= array.length) {
            T[] newArray = createArray((int) (size * 1.5));
            System.arraycopy(array, 0, newArray, 0, array.length);
            array = newArray;
        }
    }
}
