package io.github.vipcxj.jasync.ng.runtime.context;

public class ContextMutable {
    private final ArrayStack<StackFrame> stackTraces;
    private Throwable lastError;

    public ContextMutable(boolean supportStackTrace) {
        this.stackTraces = supportStackTrace ? new ArrayStack<>(12) : null;
        this.lastError = null;
    }

    private ContextMutable(ContextMutable mutable) {
        this.stackTraces = mutable.stackTraces != null ? mutable.stackTraces.copy() : null;
        this.lastError = mutable.lastError;
    }

    public ContextMutable copy() {
        return new ContextMutable(this);
    }

    public boolean supportStackTrace() {
        return this.stackTraces != null;
    }

    public void pushStackTrace(String declaringClass, String method, String fileName) {
        if (stackTraces != null) {
            stackTraces.push(new StackFrame(declaringClass, method, fileName));
        }
    }

    public void setLineNumber(int lineNumber) {
        if (stackTraces != null) {
            stackTraces.top().setLineNumber(lineNumber);
        }
    }

    public void popStackTrace() {
        if (stackTraces != null) {
            stackTraces.pop();
        }
    }

    public void setLastError(Throwable lastError) {
        this.lastError = lastError;
    }

    public Throwable getLastError() {
        return lastError;
    }

    public ArrayStack<StackFrame> getStackTraces() {
        return stackTraces;
    }
}
