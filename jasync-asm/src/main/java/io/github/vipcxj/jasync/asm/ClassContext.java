package io.github.vipcxj.jasync.asm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClassContext {
    private final String name;
    private int index = 0;
    private final Set<String> methods;
    private final List<MethodContext> lambdaContexts;

    /**
     *
     * @param name class internal name
     */
    public ClassContext(String name, Set<String> methods) {
        this.name = name;
        this.methods = methods;
        this.lambdaContexts = new ArrayList<>();
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

    public void addLambda(MethodContext methodContext) {
        this.lambdaContexts.add(methodContext);
    }

    public List<MethodContext> getLambdaContexts() {
        return lambdaContexts;
    }
}
