package io.github.vipcxj.jasync.ng.spec.exceptions;

public class JAsyncWrapException extends JAsyncException {

    public JAsyncWrapException(Throwable cause) {
        super("", cause, false, false);
    }
}
