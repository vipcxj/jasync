package io.github.vipcxj.jasync.runtime.context;

import io.github.vipcxj.jasync.runtime.promise.ContextPromise;
import io.github.vipcxj.jasync.runtime.schedule.ExecutorServiceScheduler;
import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.JScheduler;
import io.github.vipcxj.jasync.spec.JStack;

import java.util.*;
import java.util.concurrent.Executors;

public class Context implements JContext {

    private final JStack[] stacks;
    private final ContextMap contextMap;
    private final JScheduler scheduler;
    public final static JContext DEFAULTS = new Context(Context0Map.EMPTY, new ExecutorServiceScheduler(Executors.newWorkStealingPool()), null);

    public Context(ContextMap contextMap, JScheduler scheduler, JStack[] stacks) {
        this.contextMap = contextMap;
        this.scheduler = scheduler;
        this.stacks = stacks;
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
    public JPromise2<JContext> put(Object key, Object value) {
        ContextMap newContextMap = contextMap.put(key, value);
        if (newContextMap != contextMap) {
            return new ContextPromise(new Context(newContextMap, scheduler, stacks));
        } else {
            return new ContextPromise(this);
        }
    }

    @Override
    public JPromise2<JContext> remove(Object key) {
        ContextMap newContextMap = contextMap.remove(key);
        if (newContextMap != contextMap) {
            return new ContextPromise(new Context(newContextMap, scheduler, stacks));
        } else {
            return new ContextPromise(this);
        }
    }

    @Override
    public JScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public JPromise2<JContext> setScheduler(JScheduler scheduler) {
        if (this.scheduler != scheduler) {
            return new ContextPromise(new Context(contextMap, scheduler, stacks));
        } else {
            return new ContextPromise(this);
        }
    }

    @Override
    public JPromise2<JContext> pushStack(JStack stack) {
        JStack[] newStacks;
        if (stacks == null) {
            newStacks = new JStack[] { stack };
        } else {
            newStacks = new JStack[stacks.length + 1];
            System.arraycopy(stacks, 0, newStacks, 0, stacks.length);
            newStacks[stacks.length] = stack;
        }
        return new ContextPromise(new Context(contextMap, scheduler, newStacks));
    }

    @Override
    public JPromise2<JStack> popStack() {
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
        return new ContextPromise(new Context(contextMap, scheduler, newStacks)).thenReturn(stacks[stacks.length - 1]);
    }
}