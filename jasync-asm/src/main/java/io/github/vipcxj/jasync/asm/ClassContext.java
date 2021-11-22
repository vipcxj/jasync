package io.github.vipcxj.jasync.asm;

import java.util.*;

public class ClassContext {
    private final String name;
    private int index = 0;
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

    public String nextLambdaName(String ownerMethod) {
        String name = createLambdaName(ownerMethod);
        while (methods.contains(name)) {
            name = createLambdaName(ownerMethod);
        }
        return name;
    }

    private String createLambdaName(String ownerMethod) {
        return "lambda$" + ownerMethod + "$" + index++;
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
}
