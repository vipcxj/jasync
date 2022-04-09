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

    private static final Path path = getLogFile();

    private static Path getLogFile() {
        String logPath = System.getenv("JASYNC_LOG");
        Path path = null;
        if (logPath != null) {
            path = Paths.get(logPath);
            try {
                if (Files.isDirectory(path)) {
                    path = path.resolve("jasync.log");
                }
            } catch (SecurityException ignored) { }
        }
        System.out.println("The log path: " + path);
        return path;
    }

    private static void writeLog(String msg, String level) {
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
                if (path != null) {
                    Files.write(path, lines, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void info(String msg) {
        writeLog(msg, "INFO");
    }

    public static void warn(String msg) {
        writeLog(msg, "ERROR");
    }

    public static void error(String msg) {
        writeLog(msg, "ERROR");
    }

    public static void error(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        writeLog(sw.toString(), "ERROR");
    }
}
