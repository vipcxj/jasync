package io.github.vipcxj.jasync.core.javac.patch.java9;

import io.github.vipcxj.jasync.utils.patch.IPatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;

public enum Patches {
    INSTANCE;
    private final Logger logger = LogManager.getLogger();
    IPatch getPatch() {
        try {
            Class<?> type = Class.forName("io.github.vipcxj.jasync.patch.javac.java9.Patch");
            return (IPatch) type.getConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            logger.throwing(e);
            throw new RuntimeException("Unable to find the java 9 patch.");
        }
    }
}
