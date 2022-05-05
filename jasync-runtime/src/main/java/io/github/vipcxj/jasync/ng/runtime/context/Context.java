package io.github.vipcxj.jasync.ng.runtime.context;

import io.github.vipcxj.jasync.ng.runtime.schedule.ExecutorServiceScheduler;
import io.github.vipcxj.jasync.ng.spec.*;

import java.util.Set;
import java.util.concurrent.Executors;

public class Context implements JContext {

    private final JStack[] stacks;
    private final Object[][] localsStack;
    private final IntMap<JPortal<?>> portalMap;
    private final ContextMap contextMap;
    private final JScheduler scheduler;
    public final static JContext DEFAULTS = new Context(new ExecutorServiceScheduler(Executors.newWorkStealingPool()));

    public Context(JScheduler scheduler) {
        this(Context0Map.EMPTY, scheduler, null, null, null);
    }

    public Context(
            ContextMap contextMap,
            JScheduler scheduler,
            JStack[] stacks,
            Object[][] localsStack,
            IntMap<JPortal<?>> portalMap
    ) {
        this.contextMap = contextMap;
        this.scheduler = scheduler;
        this.stacks = stacks;
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
            return new Context(newContextMap, scheduler, stacks, localsStack, portalMap);
        } else {
            return this;
        }
    }

    @Override
    public JContext remove(Object key) {
        ContextMap newContextMap = contextMap.remove(key);
        if (newContextMap != contextMap) {
            return new Context(newContextMap, scheduler, stacks, localsStack, portalMap);
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
            return new Context(contextMap, scheduler, stacks, localsStack, portalMap);
        } else {
            return this;
        }
    }

    @Override
    public JContext pushStack(JStack stack) {
        JStack[] newStacks;
        if (stacks == null) {
            newStacks = new JStack[] { stack };
        } else {
            newStacks = new JStack[stacks.length + 1];
            System.arraycopy(stacks, 0, newStacks, 0, stacks.length);
            newStacks[stacks.length] = stack;
        }
        return new Context(contextMap, scheduler, newStacks, localsStack, portalMap);
    }

    @Override
    public JPromise<JStack> popStack() {
        if (stacks == null || stacks.length < 1) {
            throw new IllegalStateException("The stack is empty, unable to pop it.");
        }
        JStack[] newStacks;
        if (stacks.length == 1) {
            newStacks = null;
        } else {
            newStacks = new JStack[stacks.length - 1];
            System.arraycopy(stacks, 0, newStacks, 0, stacks.length - 1);
        }
        Context newContext = new Context(contextMap, scheduler, newStacks, localsStack, portalMap);
        return JPromise.withContext(newContext).thenReturn(stacks[stacks.length - 1]);
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
        return new Context(contextMap, scheduler, stacks, newLocalsStack, portalMap);
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
        return new Context(contextMap, scheduler, stacks, newLocalsStack, portalMap);
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
            return new Context(contextMap, scheduler, stacks, localsStack, newPortalMap);
        } else {
            return this;
        }
    }

    @Override
    public JContext removePortal(int jumpIndex) {
        IntMap<JPortal<?>> newPortalMap = portalMap.remove(jumpIndex);
        if (newPortalMap != portalMap) {
            return new Context(contextMap, scheduler, stacks, localsStack, newPortalMap);
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
