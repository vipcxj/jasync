package io.github.vipcxj.jasync.ng.asm;

import java.util.*;

public class ClassContext {
    private final String name;
    private final ClassChecker checker;
    private final Map<MethodContext, List<MethodContext>> lambdaContexts;
    private final Map<MethodContext, List<FieldContext>> fieldContexts;

    /**
     *
     * @param name class internal name
     */
    public ClassContext(String name, ClassChecker checker) {
        this.name = name;
        this.checker = checker;
        this.lambdaContexts = new HashMap<>();
        this.fieldContexts = new HashMap<>();
    }

    public boolean containMethod(String name) {
        return checker.getMethods().contains(name);
    }

    public String getName() {
        return name;
    }

    public void addLambda(MethodContext methodContext, MethodContext lambdaContext) {
        getLambdaContexts(methodContext).add(lambdaContext);
    }

    public void addFieldContext(MethodContext methodContext, FieldContext fieldContext) {
        getFieldContexts(methodContext).add(fieldContext);
    }

    public boolean hasFieldContext(MethodContext methodContext, String name) {
        List<FieldContext> fieldContexts = getFieldContexts(methodContext);
        for (FieldContext fieldContext : fieldContexts) {
            if (Objects.equals(fieldContext.getName(), name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate a unique field name in this class context. Only original fields are take into account.
     * @param name the name seed
     * @return the generated name
     */
    public String generateUniqueFieldName(String name) {
        return checker.generateUniqueFieldName(name);
    }

    public List<MethodContext> getLambdaContexts(MethodContext methodContext) {
        return this.lambdaContexts.computeIfAbsent(methodContext, k -> new ArrayList<>());
    }

    public List<FieldContext> getFieldContexts(MethodContext methodContext) {
        return this.fieldContexts.computeIfAbsent(methodContext, k -> new ArrayList<>());
    }

    public String findLambdaByHead(MethodContext methodContext, String desc, int head, MethodContext.MethodType type) {
        for (MethodContext lambdaContext : getLambdaContexts(methodContext)) {
            if (lambdaContext.getHead() == head && lambdaContext.getMv().desc.equals(desc) && lambdaContext.getType() == type) {
                return lambdaContext.getMv().name;
            }
        }
        return null;
    }
}
