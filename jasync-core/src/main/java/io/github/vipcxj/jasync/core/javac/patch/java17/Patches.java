package io.github.vipcxj.jasync.core.javac.patch.java17;

import io.github.vipcxj.jasync.core.log.LogManager;
import io.github.vipcxj.jasync.utils.patch.IPatch;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

public enum Patches {
    INSTANCE;
    private final Logger logger = LogManager.INSTANCE.createLogger(io.github.vipcxj.jasync.core.javac.patch.java9.Patches.class);
    IPatch getPatch() {
        try {
            Class<?> type = Class.forName("io.github.vipcxj.jasync.patch.javac.java17.Patch");
            return (IPatch) type.getConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            logger.log(Level.SEVERE, e, e::getLocalizedMessage);
            throw new RuntimeException("Unable to find the java 17 patch.");
        }
    }
}
