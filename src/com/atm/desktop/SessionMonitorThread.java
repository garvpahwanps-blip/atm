package com.atm.desktop;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;

public final class SessionMonitorThread extends Thread {
    private volatile boolean running = true;
    private final AtomicBoolean active = new AtomicBoolean();
    private final AtomicLong lastActivity = new AtomicLong(System.nanoTime());
    private final long timeoutNanos;
    private final Runnable expire;
    public SessionMonitorThread(Duration timeout, Runnable expire) {
        super("atm-session-monitor");
        if (timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("Positive timeout required");
        this.timeoutNanos = timeout.toNanos(); this.expire = expire; setDaemon(true);
    }
    public void touch() { lastActivity.set(System.nanoTime()); }
    public void setActive(boolean value) { touch(); active.set(value); }
    @Override public void run() {
        try {
            while (running) {
                if (active.get() && System.nanoTime() - lastActivity.get() >= timeoutNanos && active.compareAndSet(true, false))
                    SwingUtilities.invokeLater(() -> { if (running && !active.get()) expire.run(); });
                Thread.sleep(50);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finally { running = false; }
    }
    public void shutdown() { running = false; interrupt(); }
}
