package io.github.vipcxj.jasync.core.javac.model;

import io.github.vipcxj.jasync.core.javac.AnnotationUtils;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncContext;
import io.github.vipcxj.jasync.spec.annotations.Async;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import java.util.Map;

public class JAsyncInfo {
    private final String debugId;
    private final boolean disabled;
    private final Async.Method method;

    public JAsyncInfo(IJAsyncContext context, AnnotationMirror annotationMirror) {
        Elements elements = context.getEnvironment().getElementUtils();
        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = elements.getElementValuesWithDefaults(annotationMirror);
        this.debugId = AnnotationUtils.getStringAnnotationValue(annotationMirror, elementValues, Constants.ASYNC_DEBUG_ID);
        this.disabled = AnnotationUtils.getBooleanAnnotationValue(annotationMirror, elementValues, Constants.ASYNC_DISABLED);
        this.method = AnnotationUtils.getEnumAnnotationValue(annotationMirror, elementValues, Constants.ASYNC_METHOD, Async.Method.class);
    }

    public boolean isEnabled() {
        return !disabled;
    }

    public String getDebugId() {
        return debugId;
    }

    public Async.Method getMethod() {
        return method;
    }
}
