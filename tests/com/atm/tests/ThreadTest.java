package com.atm.tests;

import com.atm.desktop.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;

public final class ThreadTest {
    public static void main(String[] args) throws Exception {
        CountDownLatch tick = new CountDownLatch(1), expired = new CountDownLatch(1);
        AtomicBoolean safeClock = new AtomicBoolean(), safeExpiry = new AtomicBoolean();
        ClockThread clock = new ClockThread(text -> { safeClock.set(SwingUtilities.isEventDispatchThread() && !text.isBlank()); tick.countDown(); });
        SessionMonitorThread monitor = new SessionMonitorThread(Duration.ofMillis(150), () -> { safeExpiry.set(SwingUtilities.isEventDispatchThread()); expired.countDown(); });
        clock.start(); monitor.start(); monitor.setActive(true);
        try {
            if (!tick.await(3, TimeUnit.SECONDS) || !expired.await(3, TimeUnit.SECONDS) || !safeClock.get() || !safeExpiry.get())
                throw new AssertionError("Clock and timeout callbacks must run on the EDT");
        } finally { clock.shutdown(); monitor.shutdown(); clock.join(1000); monitor.join(1000); }
        if (clock.isAlive() || monitor.isAlive()) throw new AssertionError("Threads did not terminate");
        System.out.println("Thread tests: clock, idle expiry, EDT safety, clean shutdown passed");
    }
}
