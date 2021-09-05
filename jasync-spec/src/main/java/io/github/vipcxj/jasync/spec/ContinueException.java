package io.github.vipcxj.jasync.spec;

public class ContinueException extends RuntimeException {
    private static final long serialVersionUID = 1075004752344694554L;
    private String label;

    public ContinueException(String label) {
        super("", null, false, false);
        this.label = label;
    }

    public boolean matchLabel(String label) {
        if (this.label == null) {
            return true;
        } else {
            return this.label.equals(label);
        }
    }
}
