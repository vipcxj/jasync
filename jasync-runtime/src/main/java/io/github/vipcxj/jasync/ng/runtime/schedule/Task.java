package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public interface Task<T> {
    void schedule(JThunk<T> thunk, JContext context);
    void cancel();
}
