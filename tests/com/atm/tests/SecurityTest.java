package com.atm.tests;

import com.atm.security.PasswordHasher;
import com.atm.service.*;
import java.math.BigDecimal;

public final class SecurityTest {
    private static int checks;
    public static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    public static void rejects(Runnable task) {
        try { task.run(); } catch (BankingException e) { checks++; return; }
        throw new AssertionError("Invalid input was accepted");
    }
    public static void main(String[] args) {
        String a = PasswordHasher.hash("0123".toCharArray());
        String b = PasswordHasher.hash("0123".toCharArray());
        check(!a.equals(b), "Independent random salts");
        check(PasswordHasher.verify("0123".toCharArray(), a), "Correct PIN");
        check(!PasswordHasher.verify("9999".toCharArray(), a), "Incorrect PIN");
        check(!PasswordHasher.verify("0123".toCharArray(), "pbkdf2-sha256$1$bad$bad"), "Reject weak hash");
        check(!PasswordHasher.verify("0123".toCharArray(), a + "x"), "Reject corrupt hash");
        check(!PasswordHasher.verify("0123".toCharArray(), "plaintext"), "Reject plaintext");
        Validation.pin("0000".toCharArray());
        rejects(() -> Validation.pin("１２３４".toCharArray()));
        rejects(() -> Validation.pin("123".toCharArray()));
        rejects(() -> Validation.pin("abcd".toCharArray()));
        check(Validation.amount("10.25").equals(new BigDecimal("10.25")), "Exact decimal");
        for (String bad : new String[]{"0", "-1", "1.001", "1e3", "NaN", "1000000001", " 2", ""})
            rejects(() -> Validation.amount(bad));
        rejects(() -> Validation.amount(new BigDecimal("1.111")));
        rejects(() -> Validation.accountNumber("123"));
        rejects(() -> Validation.name("<script>"));
        rejects(() -> Validation.notes("x\nInjected"));
        rejects(() -> Validation.credit(Validation.MAX_BALANCE, new BigDecimal("0.01")));
        check(Validation.name("  Ada   Lovelace ").equals("Ada Lovelace"), "Normalize name");
        System.out.println("Security and validation: " + checks + " checks passed");
    }
}
