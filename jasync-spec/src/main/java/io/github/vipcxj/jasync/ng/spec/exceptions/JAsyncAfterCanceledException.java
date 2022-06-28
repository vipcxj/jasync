package io.github.vipcxj.jasync.ng.spec.exceptions;

public class JAsyncAfterCanceledException extends JAsyncException {

    public JAsyncAfterCanceledException(String message) {
        super(message);
    }

    public JAsyncAfterCanceledException() {
        this(null);
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        return message != null ? message : "Some errors happened when invoking the callbacks after the promise canceled. The errors are stored as suspended exceptions.";
    }
}
