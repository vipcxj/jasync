package io.github.vipcxj.jasync.asm;

import java.util.HashSet;
import java.util.Set;

public class ClassContext {
    private final String name;
    private int index = 0;
    private final Set<String> fields;
    private final Set<String> methods;

    /**
     *
     * @param name class internal name
     */
    public ClassContext(String name) {
        this.name = name;
        this.fields = new HashSet<>();
        this.methods = new HashSet<>();
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

    public boolean hasField(String name) {
        return fields.contains(name);
    }
}
