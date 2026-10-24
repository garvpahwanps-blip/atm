package com.atm.tests;

import com.atm.desktop.AdminDashboard;
import java.awt.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;

public final class DesktopSmokeTest {
    private static AdminDashboard frame;
    private static <T extends Component> T find(Container root, Class<T> type, String text) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c) && (text == null || (c instanceof AbstractButton b && text.equals(b.getText())))) return type.cast(c);
            if (c instanceof Container nested) { T found = find(nested, type, text); if (found != null) return found; }
        }
        return null;
    }
    private static void waitFor(Callable<Boolean> condition, String description) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        while (System.nanoTime() < end) {
            AtomicReference<Boolean> ok = new AtomicReference<>(false);
            SwingUtilities.invokeAndWait(() -> { try { ok.set(condition.call()); } catch (Exception e) { throw new RuntimeException(e); } });
            if (ok.get()) return;
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out: " + description);
    }
    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("Graphical display required; use xvfb-run on Linux");
        String file = System.getenv("ATM_TEST_ADMIN_PASSWORD_FILE");
        if (file == null) throw new IllegalArgumentException("Set ATM_TEST_ADMIN_PASSWORD_FILE to a private admin password file for this test");
        String password = Files.readString(Path.of(file)).strip();
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame = new AdminDashboard(Duration.ofSeconds(3)); frame.setVisible(true);
                find(frame, JPasswordField.class, null).setText(password);
                find(frame, JButton.class, "Sign in").doClick();
            });
            waitFor(() -> find(frame, JTable.class, null).getRowCount() > 0, "load customer JTable after authenticated login");
            SwingUtilities.invokeAndWait(() -> {
                find(frame, JTable.class, null).setRowSelectionInterval(0, 0);
                find(frame, JButton.class, "View history").doClick();
            });
            waitFor(() -> {
                for (Component c : frame.getContentPane().getComponents())
                    if (c instanceof JLabel label && label.getText().contains("transaction(s) for")) return true;
                return false;
            }, "selected customer history");
            waitFor(() -> {
                for (Component c : frame.getContentPane().getComponents())
                    if (c instanceof JLabel label && label.getText().contains("Session expired")) return true;
                return false;
            }, "desktop automatic logout");
        } finally { if (frame != null) SwingUtilities.invokeAndWait(frame::dispose); }
        for (Thread thread : Thread.getAllStackTraces().keySet())
            if (thread.isAlive() && (thread.getName().equals("atm-clock") || thread.getName().equals("atm-session-monitor")))
                throw new AssertionError("Leaked dashboard thread");
        System.out.println("Swing smoke: real frame, admin login, customer table, history, idle logout, clean close passed");
    }
}
