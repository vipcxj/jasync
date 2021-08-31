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
    private final boolean logResultTree;
    private final boolean logResultPosTree;
    private final boolean experiment;
    private final Async.Method method;

    public JAsyncInfo(IJAsyncContext context, AnnotationMirror annotationMirror) {
        Elements elements = context.getEnvironment().getElementUtils();
        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = elements.getElementValuesWithDefaults(annotationMirror);
        this.debugId = AnnotationUtils.getStringAnnotationValue(annotationMirror, elementValues, Constants.ASYNC_DEBUG_ID);
        this.logResultTree = AnnotationUtils.getBooleanAnnotationValue(annotationMirror, elementValues, Constants.ASYNC_LOG_RESULT_TREE);
        this.logResultPosTree = AnnotationUtils.getBooleanAnnotationValue(annotationMirror, elementValues, Constants.ASYNC_LOG_RESULT_POS_TREE);
        this.disabled = AnnotationUtils.getBooleanAnnotationValue(annotationMirror, elementValues, Constants.ASYNC_DISABLED);
        this.experiment = AnnotationUtils.getBooleanAnnotationValue(annotationMirror, elementValues, Constants.ASYNC_EXPERIMENT);
        this.method = AnnotationUtils.getEnumAnnotationValue(annotationMirror, elementValues, Constants.ASYNC_METHOD, Async.Method.class);
    }

    public boolean isEnabled() {
        return !disabled;
    }

    public boolean isLogResultTree() {
        return logResultTree;
    }

    public boolean isLogResultPosTree() {
        return logResultPosTree;
    }

    public boolean isExperiment() {
        return experiment;
    }

    public String getDebugId() {
        return debugId;
    }

    public Async.Method getMethod() {
        return method;
    }
}
