package io.github.vipcxj.jasync.core.log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public enum LogManager {
    INSTANCE;
    public Logger createLogger(Class<?> type) {
        Logger logger = Logger.getLogger(type.getCanonicalName());
        try {
            String path = System.getProperty("jasync.log.filepath");
            if (path != null) {
                FileHandler handler = null;
                try {
                    handler = new FileHandler("jasync.log");
                    handler.setEncoding(StandardCharsets.UTF_8.displayName());
                    handler.setLevel(Level.INFO);
                    handler.setFormatter(new SimpleFormatter());
                } catch (IOException e) {
                    logger.log(Level.WARNING, e, e::getLocalizedMessage);
                }
                if (handler != null) {
                    logger.addHandler(handler);
                }
            }
            String levelEnv = System.getProperty("jasync.log.level");
            if (levelEnv != null) {
                try {
                    Level level = Level.parse(levelEnv);
                    logger.setLevel(level);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, t, t::getLocalizedMessage);
                }
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, t, t::getLocalizedMessage);
        }
        return logger;
    }
}
