package io.github.vipcxj.jasync.javac;

import java.util.List;

public class ClassContext extends BaseContext implements JavacContext {

    private List<ClassContext> classContexts;
    private List<MethodContext> methodContexts;

    public ClassContext(JavacContext parent) {
        super(parent);
    }

}
