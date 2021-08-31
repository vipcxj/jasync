package io.github.vipcxj.jasync.core.javac.translate.factories;


import io.github.vipcxj.jasync.core.javac.translate.spi.TranslateContextFactory;

public abstract class AbstractTranslateContextFactory implements TranslateContextFactory {

    @Override
    public int priority() {
        return 0;
    }
}
