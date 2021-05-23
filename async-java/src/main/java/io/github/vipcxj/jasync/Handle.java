package io.github.vipcxj.jasync;

public interface Handle {

    void cancel();

    boolean isCanceled();
}
