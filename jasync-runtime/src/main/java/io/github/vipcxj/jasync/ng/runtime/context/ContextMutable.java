package io.github.vipcxj.jasync.ng.runtime.context;

import java.util.concurrent.atomic.AtomicLong;

public class ContextMutable {
    private static final AtomicLong ROUTINE_ID_GEN = new AtomicLong();
    private final ArrayStack<StackFrame> stackTraces;
    private Throwable lastError;
    private final long id;
    private int sharedLockCount;

    public ContextMutable(boolean supportStackTrace) {
        this.id = ROUTINE_ID_GEN.incrementAndGet();
        this.stackTraces = supportStackTrace ? new ArrayStack<>(12) : null;
        this.lastError = null;
    }

    private ContextMutable(ContextMutable mutable) {
        this.id = ROUTINE_ID_GEN.incrementAndGet();
        this.stackTraces = mutable.stackTraces != null ? mutable.stackTraces.copy() : null;
        this.lastError = mutable.lastError;
        this.sharedLockCount = 0;
    }

    public ContextMutable copy() {
        return new ContextMutable(this);
    }

    public long getId() {
        return id;
    }

    public int getSharedLockCount() {
        return sharedLockCount;
    }
    public void incSharedLockCount() {
        ++sharedLockCount;
    }
    public void decSharedLockCount() {
        --sharedLockCount;
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
