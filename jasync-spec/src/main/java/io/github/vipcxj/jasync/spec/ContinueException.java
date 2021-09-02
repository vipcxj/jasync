package io.github.vipcxj.jasync.spec;

public class ContinueException extends RuntimeException {
    private static final long serialVersionUID = 1075004752344694554L;
    private String label;

    public ContinueException(String label) {
        super("", null, false, false);
        this.label = label;
    }

    public boolean matchLabel(String label) {
        if (this.label == null && label == null) {
            return true;
        } else if (this.label == null || label == null) {
            return false;
        } else {
            return this.label.equals(label);
        }
    }
}
