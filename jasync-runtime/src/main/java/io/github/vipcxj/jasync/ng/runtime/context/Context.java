package io.github.vipcxj.jasync.ng.runtime.context;

import io.github.vipcxj.jasync.ng.runtime.schedule.ExecutorServiceScheduler;
import io.github.vipcxj.jasync.ng.spec.*;

import java.util.Set;
import java.util.concurrent.Executors;

public class Context implements JContext {

    private final Object[][] localsStack;
    private final IntMap<JPortal<?>> portalMap;
    private final ContextMap contextMap;
    private final JScheduler scheduler;
    public final static JContext DEFAULTS = new Context(new ExecutorServiceScheduler(Executors.newWorkStealingPool()));

    public Context(JScheduler scheduler) {
        this(Context0Map.EMPTY, scheduler, null, null);
    }

    public Context(
            ContextMap contextMap,
            JScheduler scheduler,
            Object[][] localsStack,
            IntMap<JPortal<?>> portalMap
    ) {
        this.contextMap = contextMap;
        this.scheduler = scheduler;
        this.localsStack = localsStack;
        this.portalMap = portalMap == null ? IntMap.empty() : portalMap;
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
            return new Context(newContextMap, scheduler, localsStack, portalMap);
        } else {
            return this;
        }
    }

    @Override
    public JContext remove(Object key) {
        ContextMap newContextMap = contextMap.remove(key);
        if (newContextMap != contextMap) {
            return new Context(newContextMap, scheduler, localsStack, portalMap);
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
            return new Context(contextMap, scheduler, localsStack, portalMap);
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
        return new Context(contextMap, scheduler, newLocalsStack, portalMap);
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
        return new Context(contextMap, scheduler, newLocalsStack, portalMap);
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
            return new Context(contextMap, scheduler, localsStack, newPortalMap);
        } else {
            return this;
        }
    }

    @Override
    public JContext removePortal(int jumpIndex) {
        IntMap<JPortal<?>> newPortalMap = portalMap.remove(jumpIndex);
        if (newPortalMap != portalMap) {
            return new Context(contextMap, scheduler, localsStack, newPortalMap);
        } else {
            return this;
        }
    }

    @Override
    public <T> JPromise<T> jump(int jumpIndex) {
        JPortal<?> portal = portalMap.get(jumpIndex);
        //noinspection unchecked
        return portal != null ? (JPromise<T>) portal.jump() : JPromise.empty();
    }
}
