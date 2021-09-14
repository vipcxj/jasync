package io.github.vipcxj.jasync.spec;

public class BreakException extends JAsyncException {

    private static final long serialVersionUID = -6532237818800947165L;
    private String label;

    public BreakException(String label) {
        super("", null, false, false);
        this.label = label;
    }

    public boolean matchLabel(String label) {
        return matchLabel(label, false);
    }

    public boolean matchLabel(String label, boolean block) {
        if (!block && this.label == null) {
            return true;
        } else if (block && this.label == null) {
            return false;
        } else {
            return this.label.equals(label);
        }
    }
}
