package com.atm.desktop;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

public final class ClockThread extends Thread {
    private volatile boolean running = true;
    private final Consumer<String> display;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss z");
    public ClockThread(Consumer<String> display) { super("atm-clock"); this.display = display; setDaemon(true); }
    @Override public void run() {
        try {
            while (running) {
                String time = FORMAT.format(ZonedDateTime.now());
                SwingUtilities.invokeLater(() -> { if (running) display.accept(time); });
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finally { running = false; }
    }
    public void shutdown() { running = false; interrupt(); }
}
