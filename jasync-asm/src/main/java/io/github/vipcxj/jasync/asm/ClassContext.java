package io.github.vipcxj.jasync.asm;

import java.util.*;

public class ClassContext {
    private final String name;
    private final Set<String> methods;
    private final Map<MethodContext, List<MethodContext>> lambdaContexts;

    /**
     *
     * @param name class internal name
     */
    public ClassContext(String name, Set<String> methods) {
        this.name = name;
        this.methods = methods;
        this.lambdaContexts = new HashMap<>();
    }

    public boolean containMethod(String name) {
        return methods.contains(name);
    }

    public String getName() {
        return name;
    }

    public void addLambda(MethodContext methodContext, MethodContext lambdaContext) {
        getLambdaContexts(methodContext).add(lambdaContext);
    }

    public List<MethodContext> getLambdaContexts(MethodContext methodContext) {
        return this.lambdaContexts.computeIfAbsent(methodContext, k -> new ArrayList<>());
    }

    public String findLambdaByHead(MethodContext methodContext, String desc, int head) {
        for (MethodContext lambdaContext : getLambdaContexts(methodContext)) {
            if (lambdaContext.getHead() == head && lambdaContext.getMv().desc.equals(desc) && !lambdaContext.isHide()) {
                return lambdaContext.getMv().name;
            }
        }
        return null;
    }
}
