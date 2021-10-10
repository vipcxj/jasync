package io.github.vipcxj.jasync.runtime.context;

import io.github.vipcxj.jasync.runtime.promise.ContextPromise;
import io.github.vipcxj.jasync.runtime.schedule.ExecutorServiceScheduler;
import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.JScheduler;

import java.util.Set;
import java.util.concurrent.Executors;

public class Context implements JContext {

    private final ContextMap contextMap;
    private final JScheduler scheduler;
    public final static JContext DEFAULTS = new Context(Context0Map.EMPTY, new ExecutorServiceScheduler(Executors.newWorkStealingPool()));

    public Context(ContextMap contextMap, JScheduler scheduler) {
        this.contextMap = contextMap;
        this.scheduler = scheduler;
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
            return new ContextPromise(new Context(newContextMap, scheduler));
        } else {
            return new ContextPromise(this);
        }
    }

    @Override
    public JPromise2<JContext> remove(Object key) {
        ContextMap newContextMap = contextMap.remove(key);
        if (newContextMap != contextMap) {
            return new ContextPromise(new Context(newContextMap, scheduler));
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
            return new ContextPromise(new Context(contextMap, scheduler));
        } else {
            return new ContextPromise(this);
        }
    }
}
