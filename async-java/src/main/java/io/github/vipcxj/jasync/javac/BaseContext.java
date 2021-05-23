package io.github.vipcxj.jasync.javac;

public abstract class BaseContext implements JavacContext {
    private final JavacContext parent;

    public BaseContext(JavacContext parent) {
        this.parent = parent;
    }

    @Override
    public JavacContext getParent() {
        return parent;
    }
}
