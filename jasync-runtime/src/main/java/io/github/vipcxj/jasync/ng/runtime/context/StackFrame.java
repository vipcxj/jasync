package io.github.vipcxj.jasync.ng.runtime.context;

public class StackFrame {
    private final String classQualifiedName;
    private final String method;
    private final String fileName;
    private int lineNumber;

    public StackFrame(String classQualifiedName, String method, String fileName) {
        this.classQualifiedName = classQualifiedName;
        this.method = method;
        this.fileName = fileName;
        this.lineNumber = -1;
    }

    public String getClassQualifiedName() {
        return classQualifiedName;
    }

    public String getMethod() {
        return method;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
