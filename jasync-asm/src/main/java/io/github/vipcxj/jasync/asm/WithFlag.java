package io.github.vipcxj.jasync.asm;

public class WithFlag<T> {
    private final T data;
    private final boolean flag;

    private WithFlag(T data, boolean flag) {
        this.data = data;
        this.flag = flag;
    }

    public T getData() {
        return data;
    }

    public boolean isFlag() {
        return flag;
    }

    public static <T> WithFlag<T> of(T data, boolean flag) {
        return new WithFlag<>(data, flag);
    }
}
