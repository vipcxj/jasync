package io.github.vipcxj.jasync.ng.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Logger {

    private static final Path LOG_PATH = getLogFile();
    private static Path getLogFile() {
        String logPath = System.getProperty("jasync.log.path");
        if (logPath == null) {
            logPath = System.getenv("JASYNC_LOG_PATH");
        }
        Path path;
        if (logPath != null) {
            path = Paths.get(logPath);
            try {
                if (Files.isDirectory(path)) {
                    path = path.resolve("jasync.log");
                }
            } catch (SecurityException ignored) { }
        } else {
            path = Paths.get(System.getProperty("user.home"), "jasync.log");
        }
        System.out.println("The log path: " + path);
        return path;
    }

    private static final LEVEL LOG_LEVEL = getLogLevel();
    private static LEVEL getLogLevel() {
        String logPath = System.getProperty("jasync.log.level");
        if (logPath == null) {
            logPath = System.getenv("JASYNC_LOG_LEVEL");
        }
        if (logPath == null) {
            return LEVEL.INFO;
        }
        try {
            return LEVEL.valueOf(logPath.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LEVEL.INFO;
        }
    }

    private static void writeLog(String msg, LEVEL level) {
        if (level.ordinal() < LOG_LEVEL.ordinal()) {
            return;
        }
        if (msg == null || msg.length() == 0) {
            return;
        }
        String prefix = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss.SSS").format(new Date())
                + " [" + Thread.currentThread().getName() + "]["
                + level + "] ";
        String[] originalLines = msg.split("[\r\n]+");
        if (originalLines.length > 0) {
            List<String> lines = Arrays.stream(originalLines)
                    .skip(1)
                    .map(s -> String.format("%" + prefix.length() + "s", " ") + s)
                    .collect(Collectors.toCollection(LinkedList::new));
            lines.add(0, prefix + originalLines[0]);
            try {
                System.out.print(lines.stream().collect(Collectors.joining(System.lineSeparator())));
                System.out.println();
                if (LOG_PATH != null) {
                    Files.write(LOG_PATH, lines, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void trace(String msg) {
        writeLog(msg, LEVEL.TRACE);
    }

    public static void info(String msg) {
        writeLog(msg, LEVEL.INFO);
    }

    public static void warn(String msg) {
        writeLog(msg, LEVEL.WARN);
    }

    public static void warn(Throwable t) {
        throwable(t, LEVEL.WARN);
    }

    public static void error(String msg) {
        writeLog(msg, LEVEL.ERROR);
    }

    public static void error(Throwable t) {
        throwable(t, LEVEL.ERROR);
    }

    public static void throwable(Throwable t, LEVEL level) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        writeLog(sw.toString(), level);
    }

    enum LEVEL {
        TRACE,
        INFO,
        WARN,
        ERROR
    }
}
