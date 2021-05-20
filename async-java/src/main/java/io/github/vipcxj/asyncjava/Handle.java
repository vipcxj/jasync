package io.github.vipcxj.asyncjava;

public interface Handle {

    void cancel();

    boolean isCanceled();
}
