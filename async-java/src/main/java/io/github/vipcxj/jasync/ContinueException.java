package io.github.vipcxj.jasync;

public class ContinueException extends RuntimeException {
    public ContinueException() {
        super("", null, false, false);
    }
}
