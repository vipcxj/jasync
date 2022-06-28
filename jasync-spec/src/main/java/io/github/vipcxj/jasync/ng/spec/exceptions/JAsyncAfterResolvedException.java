package io.github.vipcxj.jasync.ng.spec.exceptions;

public class JAsyncAfterResolvedException extends JAsyncException {

    private final Object value;

    public JAsyncAfterResolvedException(Object value, String message) {
        super(message);
        this.value = value;
    }

    public JAsyncAfterResolvedException(Object value) {
        this(value, null);
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        return message != null ? message : "Some errors happened when invoking the callbacks after the promise resolved. The errors are stored as suspended exceptions.";
    }
}
