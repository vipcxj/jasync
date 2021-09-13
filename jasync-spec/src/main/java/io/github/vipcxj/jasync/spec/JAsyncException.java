package io.github.vipcxj.jasync.spec;

public class JAsyncException extends RuntimeException {

    public JAsyncException() {
    }

    public JAsyncException(String message) {
        super(message);
    }

    public JAsyncException(String message, Throwable cause) {
        super(message, cause);
    }

    public JAsyncException(Throwable cause) {
        super(cause);
    }

    public JAsyncException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
