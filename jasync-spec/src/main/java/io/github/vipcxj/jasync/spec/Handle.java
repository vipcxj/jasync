package io.github.vipcxj.jasync.spec;

public interface Handle {

    void cancel();

    boolean isCanceled();
}
