package io.github.vipcxj.jasync.spec;

public class BreakException extends RuntimeException {

    private static final long serialVersionUID = -6532237818800947165L;
    private String label;

    public BreakException(String label) {
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
