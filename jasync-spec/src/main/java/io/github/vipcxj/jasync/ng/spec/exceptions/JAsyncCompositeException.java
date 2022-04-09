package io.github.vipcxj.jasync.ng.spec.exceptions;

import io.github.vipcxj.jasync.ng.spec.JPromise;

import java.util.Map;

public class JAsyncCompositeException extends JAsyncException {
    private final Map<JPromise<?>, Throwable> errors;

    public JAsyncCompositeException(Map<JPromise<?>, Throwable> errors) {
        this.errors = errors;
    }

    @Override
    public String getMessage() {
        return "All promises are rejected. Please use JAsyncCompositeException.getErrors() to inspect the real errors.";
    }

    public Map<JPromise<?>, Throwable> getErrors() {
        return errors;
    }
}
