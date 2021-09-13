package io.github.vipcxj.jasync.spec;

public class ReturnException extends JAsyncException {

    private static final long serialVersionUID = -2412690211307531094L;
    private final Object value;

    public ReturnException(Object value) {
        super("", null, false, false);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
