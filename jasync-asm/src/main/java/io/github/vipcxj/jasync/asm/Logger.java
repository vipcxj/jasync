package io.github.vipcxj.jasync.asm;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Logger {

    private static final Path path = getLogFile();

    private static Path getLogFile() {
        String os = System.getProperty("os.name").toLowerCase();
        Path path;
        if (os.contains("win")) {
            String home = System.getenv("userprofile");
            path = Paths.get(home + "\\jasync.log");
        } else {
            path = Paths.get("~/jasync.log");
        }
        if (Files.notExists(path)) {
            try {
                return Files.createFile(path);
            } catch (FileAlreadyExistsException ignored) {
                return path;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return path;
        }
    }

    private static void writeLog(String msg, String level) {
        if (msg == null || msg.length() == 0) {
            return;
        }
        String prefix = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss.SSS").format(new Date())
                + " [" + Thread.currentThread().getName() + "]["
                + level + "] ";
        List<String> lines = Arrays.stream(msg.split("[\r\n]+"))
                .skip(1)
                .map(s -> s + String.format("%" + prefix.length() + "s", " "))
                .collect(Collectors.toCollection(LinkedList::new));
        lines.add(0, prefix + msg);
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
