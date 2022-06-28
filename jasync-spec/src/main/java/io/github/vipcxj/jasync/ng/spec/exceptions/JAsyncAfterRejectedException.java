package io.github.vipcxj.jasync.ng.spec.exceptions;

public class JAsyncAfterRejectedException extends JAsyncException {

    private final Throwable error;

    public JAsyncAfterRejectedException(Throwable error, String message) {
        super(message);
        this.error = error;
    }

    public JAsyncAfterRejectedException(Throwable error) {
        this(error, null);
    }

    public Throwable getError() {
        return error;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        return message != null ? message : "Some errors happened when invoking the callbacks after the promise rejected. The errors are stored as suspended exceptions.";
    }
}
