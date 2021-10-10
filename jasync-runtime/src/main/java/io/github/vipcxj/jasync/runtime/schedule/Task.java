package io.github.vipcxj.jasync.runtime.schedule;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JThunk;

public interface Task<T> {
    void schedule(JThunk<T> thunk, JContext context);
    void cancel();
}
