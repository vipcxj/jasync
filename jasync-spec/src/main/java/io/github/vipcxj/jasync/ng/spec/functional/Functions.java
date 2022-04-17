package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JScheduler;
import io.github.vipcxj.jasync.ng.spec.JThunk;

import java.util.function.BiConsumer;

public class Functions {
    public static BiConsumer<JThunk<JContext>, JContext> PROMISE_HANDLER_EXTRACT_CONTEXT = (thunk, context) -> thunk.resolve(context, context);
    public static BiConsumer<JThunk<JScheduler>, JContext> PROMISE_HANDLER_EXTRACT_SCHEDULER = (thunk, context) -> {
        thunk.resolve(context.getScheduler(), context);
    };
}
