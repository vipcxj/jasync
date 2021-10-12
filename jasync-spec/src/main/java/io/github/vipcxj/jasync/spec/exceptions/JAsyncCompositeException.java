package io.github.vipcxj.jasync.spec.exceptions;

import io.github.vipcxj.jasync.spec.JPromise2;

import java.util.Map;

public class JAsyncCompositeException extends JAsyncException {
    private final Map<JPromise2<?>, Throwable> errors;

    public JAsyncCompositeException(Map<JPromise2<?>, Throwable> errors) {
        this.errors = errors;
    }

    @Override
    public String getMessage() {
        return "All promises are rejected. Please use JAsyncCompositeException.getErrors() to inspect the real errors.";
    }

    public Map<JPromise2<?>, Throwable> getErrors() {
        return errors;
    }
}
