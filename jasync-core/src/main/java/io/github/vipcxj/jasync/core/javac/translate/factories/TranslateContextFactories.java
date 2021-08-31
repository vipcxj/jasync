package io.github.vipcxj.jasync.core.javac.translate.factories;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.translate.spi.TranslateContextFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public enum TranslateContextFactories {

    INSTANCE;

    private Map<Class<? extends JCTree>, TranslateContextFactory> factories;

    TranslateContextFactories() {
        this.factories = new HashMap<>();
    }

    public TranslateContextFactory getFactory(JCTree tree) {
        TranslateContextFactory factory = this.factories.get(tree.getClass());
        if (factory == null) {
            int priority = Integer.MIN_VALUE;
            for (TranslateContextFactory contextFactory : ServiceLoader.load(TranslateContextFactory.class, getClass().getClassLoader())) {
                if (contextFactory.isSupport(tree) && contextFactory.priority() >= priority) {
                    priority = contextFactory.priority();
                    factory = contextFactory;
                }
            }
            if (factory != null) {
                factories.put(tree.getClass(), factory);
            }
        }
        return factory;
    }
}
