package io.github.vipcxj.jasync.ng.asm;

import java.util.concurrent.TimeUnit;

public class TimeUtils {

    private long time;
    private long escaped;

    public void start() {
        time = System.nanoTime();
    }

    public void stop() {
        escaped = System.nanoTime() - time;
    }

    public long escaped(TimeUnit tu) {
        return tu.convert(escaped, TimeUnit.NANOSECONDS);
    }
}
