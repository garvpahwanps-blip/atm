package com.atm.security;

import java.io.Console;
import java.util.Arrays;

public final class AdminPasswordTool {
    private AdminPasswordTool() {}
    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Run in a real terminal; password input is never echoed.");
        char[] password = console.readPassword("New administrator password (12+ characters): ");
        char[] confirmation = console.readPassword("Confirm password: ");
        try {
            if (password == null || password.length < 12 || !Arrays.equals(password, confirmation))
                throw new IllegalArgumentException("Passwords must match and contain at least 12 characters.");
            console.printf("admin.passwordHash=%s%n", PasswordHasher.hash(password));
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
}
