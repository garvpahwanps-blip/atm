package com.atm.security;

import com.atm.dao.Database;
import java.time.Duration;

/** A process-wide throttle for the single configured administrator. */
public final class AdminAuthenticator {
    private static int failures;
    private static long blockedUntil;
    private AdminAuthenticator() {}

    public static synchronized boolean authenticate(String name, char[] password) {
        long now = System.nanoTime();
        if (blockedUntil != 0 && now < blockedUntil) return false;
        String hash = Database.configuration().getProperty("admin.passwordHash", "");
        boolean verified = PasswordHasher.verify(password, hash);
        if ("admin".equals(name) && verified) { failures = 0; blockedUntil = 0; return true; }
        if (++failures >= 5) { blockedUntil = now + Duration.ofMinutes(1).toNanos(); failures = 0; }
        return false;
    }

    public static int sessionMinutes() {
        try { return Math.max(1, Math.min(60, Integer.parseInt(Database.configuration().getProperty("admin.sessionMinutes", "10")))); }
        catch (NumberFormatException e) { return 10; }
    }
}
