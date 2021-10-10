package io.github.vipcxj.jasync.runtime.context;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.*;

public class ContextMapKeySet implements Set<Object> {

    private final Object[] keys;

    public ContextMapKeySet(Object... keys) {
        this.keys = keys;
    }

    @Override
    public int size() {
        return keys.length;
    }

    @Override
    public boolean isEmpty() {
        return keys.length == 0;
    }

    @Override
    public boolean contains(Object o) {
        for (Object key : keys) {
            if (Objects.equals(o, key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @NonNull
    public Iterator<Object> iterator() {
        return new MyIterator();
    }

    @Override
    @NonNull
    public Object[] toArray() {
        Object[] newKeys = new Object[keys.length];
        System.arraycopy(keys, 0, newKeys, 0, keys.length);
        return newKeys;
    }

    @Override
    @NonNull
    public <T> T[] toArray(T[] a) {
        //noinspection unchecked
        a = a != null && a.length >= keys.length ? a : (T[]) new Object[keys.length];
        System.arraycopy(keys, 0, a, 0, keys.length);
        for (int i = keys.length; i < a.length ; ++i) {
            a[i] = null;
        }
        return a;
    }

    @Override
    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    class MyIterator implements Iterator<Object> {

        private int index;
        private MyIterator() {
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < keys.length;
        }

        @Override
        public Object next() {
            return keys[index++];
        }
    }
}
