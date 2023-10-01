package io.github.vipcxj.jasync.ng.runtime.context;

import io.github.vipcxj.jasync.ng.runtime.utils.StackTraceUtils;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPortal;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JScheduler;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Context implements JContext {

    private final Object[][] localsStack;
    private final IntMap<JPortal<?>> portalMap;
    private final ContextMap contextMap;
    private final JScheduler scheduler;
    private final ContextMutable mutable;

    public Context(JScheduler scheduler, boolean supportStackTrace) {
        this(new ContextMutable(supportStackTrace), Context0Map.EMPTY, scheduler, null, null);
    }

    public Context(
            ContextMutable mutable,
            ContextMap contextMap,
            JScheduler scheduler,
            Object[][] localsStack,
            IntMap<JPortal<?>> portalMap
    ) {
        this.mutable = mutable;
        this.contextMap = contextMap;
        this.scheduler = scheduler;
        this.localsStack = localsStack;
        this.portalMap = portalMap == null ? IntMap.empty() : portalMap;
    }

    @Override
    public long id() {
        return mutable.getId();
    }

    @Override
    public <T> T get(Object key) {
        return contextMap.get(key);
    }

    @Override
    public boolean hasKey(Object key) {
        return contextMap.hasKey(key);
    }

    @Override
    public Set<Object> keys() {
        return contextMap.keys();
    }

    @Override
    public int size() {
        return contextMap.size();
    }

    @Override
    public JContext set(Object key, Object value) {
        ContextMap newContextMap = contextMap.put(key, value);
        if (newContextMap != contextMap) {
            return new Context(mutable, newContextMap, scheduler, localsStack, portalMap);
        } else {
            return this;
        }
    }

    @Override
    public JContext remove(Object key) {
        ContextMap newContextMap = contextMap.remove(key);
        if (newContextMap != contextMap) {
            return new Context(mutable, newContextMap, scheduler, localsStack, portalMap);
        } else {
            return this;
        }
    }

    @Override
    public JScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public JContext setScheduler(JScheduler scheduler) {
        if (this.scheduler != scheduler) {
            return new Context(mutable, contextMap, scheduler, localsStack, portalMap);
        } else {
            return this;
        }
    }

    @Override
    public JContext pushLocals(Object... args) {
        Object[][] newLocalsStack;
        if (localsStack == null) {
            newLocalsStack = new Object[][] { args };
        } else {
            newLocalsStack = new Object[localsStack.length + 1][];
            System.arraycopy(localsStack, 0, newLocalsStack, 0, localsStack.length);
            newLocalsStack[localsStack.length] = args;
        }
        return new Context(mutable, contextMap, scheduler, newLocalsStack, portalMap);
    }

    @Override
    public JContext popLocals() {
        if (localsStack == null || localsStack.length < 1) {
            throw new IllegalStateException("The locals stack is empty, unable to pop it.");
        }
        Object[][] newLocalsStack;
        if (localsStack.length == 1) {
            newLocalsStack = null;
        } else {
            newLocalsStack = new Object[localsStack.length - 1][];
            System.arraycopy(localsStack, 0, newLocalsStack, 0, localsStack.length - 1);
        }
        return new Context(mutable, contextMap, scheduler, newLocalsStack, portalMap);
    }

    @Override
    public Object[] getLocals() {
        if (localsStack == null || localsStack.length < 1) {
            throw new IllegalStateException("The locals stack is empty, unable to get it.");
        }
        return localsStack[0];
    }

    @Override
    public JContext setPortal(int jumpIndex, JPortal<?> portal) {
        IntMap<JPortal<?>> newPortalMap = portalMap.set(jumpIndex, portal);
        if (newPortalMap != portalMap) {
            return new Context(mutable, contextMap, scheduler, localsStack, newPortalMap);
        } else {
            return this;
        }
    }

    @Override
    public JContext removePortal(int jumpIndex) {
        IntMap<JPortal<?>> newPortalMap = portalMap.remove(jumpIndex);
        if (newPortalMap != portalMap) {
            return new Context(mutable, contextMap, scheduler, localsStack, newPortalMap);
        } else {
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> JPromise<T> jump(int jumpIndex) {
        JPortal<?> portal = portalMap.get(jumpIndex);
        return portal != null ? (JPromise<T>) portal.jump() : JPromise.empty();
    }

    @Override
    public JContext pushStackFrame(String declaringClass, String method, String fileName) {
        mutable.pushStackTrace(declaringClass, method, fileName);
        return this;
    }

    @Override
    public JContext setLineNumber(int lineNumber) {
        mutable.setLineNumber(lineNumber);
        return this;
    }

    @Override
    public JContext popStackFrame() {
        mutable.popStackTrace();
        return this;
    }

    private final static Pattern PT_LAMBDA_NAME = Pattern.compile("lambda\\$(.*)\\$\\d+");

    private String fixMethodName(String methodName) {
        Matcher matcher = PT_LAMBDA_NAME.matcher(methodName);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return methodName;
        }
    }

    @Override
    public JContext fork() {
        return new Context(mutable.copy(), contextMap, scheduler, localsStack, portalMap);
    }

    @Override
    public int getSharedLockCount() {
        return mutable.getSharedLockCount();
    }

    @Override
    public void incSharedLockCount() {
        mutable.incSharedLockCount();
    }

    @Override
    public void decSharedLockCount() {
        mutable.decSharedLockCount();
    }

    @Override
    public void fixException(Throwable throwable) {
        if (!mutable.supportStackTrace()) {
            return;
        }
        if (throwable == mutable.getLastError()) {
            return;
        }
        mutable.setLastError(throwable);
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            int count = (int) mutable.getStackTraces().stream().filter(st -> st.getLineNumber() >= 0).count();
            StackTraceElement[] newStackTrace = new StackTraceElement[count + 1];
            StackTraceElement first = stackTrace[0];
            newStackTrace[0] = StackTraceUtils.updateStackFrame(
                    first, null, null, null, null,
                    fixMethodName(first.getMethodName()),
                    null, Integer.MIN_VALUE);
            int i = count;
            for (StackFrame stackFrame : mutable.getStackTraces()) {
                if (stackFrame.getLineNumber() >= 0) {
                    newStackTrace[i--] = StackTraceUtils.createStackFrame(
                            null, null, null,
                            stackFrame.getClassQualifiedName(), stackFrame.getMethod(),
                            stackFrame.getFileName(), stackFrame.getLineNumber()
                    );
                }
            }
            throwable.setStackTrace(newStackTrace);
        }
    }
}
