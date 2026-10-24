package com.atm.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Validation {
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000.00");
    public static final BigDecimal MAX_BALANCE = new BigDecimal("99999999999999999.99");
    private Validation() {}

    public static String name(String value) {
        if (value == null) throw new BankingException("Name is required.");
        String name = value.strip().replaceAll("\\s+", " ");
        if (name.length() < 2 || name.length() > 100 ||
            !name.matches("[\\p{L}\\p{M}][\\p{L}\\p{M} .'-]*"))
            throw new BankingException("Name must contain 2–100 letters, spaces, apostrophes, periods or hyphens.");
        return name;
    }

    public static String accountNumber(String value) {
        if (value == null || !value.matches("[0-9]{12}"))
            throw new BankingException("Account number must contain exactly 12 digits.");
        return value;
    }

    public static void pin(char[] pin) {
        if (pin == null || pin.length != 4) throw new BankingException("PIN must contain exactly four digits.");
        for (char c : pin) if (c < '0' || c > '9')
            throw new BankingException("PIN must contain exactly four digits.");
    }

    public static BigDecimal amount(String value) {
        if (value == null || value.length() > 30 || !value.matches("[0-9]+(\\.[0-9]{1,2})?"))
            throw new BankingException("Enter a positive amount with at most two decimal places.");
        return amount(new BigDecimal(value));
    }

    public static BigDecimal amount(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.compareTo(MAX_AMOUNT) > 0)
            throw new BankingException("Amount must be between 0.01 and 1,000,000,000.00.");
        try { return value.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new BankingException("Amount must have at most two decimal places."); }
    }

    public static String notes(String value) {
        String notes = value == null ? "" : value.strip();
        if (notes.length() > 255 || notes.chars().anyMatch(Character::isISOControl))
            throw new BankingException("Notes must be at most 255 characters without control characters.");
        return notes;
    }

    public static BigDecimal credit(BigDecimal balance, BigDecimal amount) {
        BigDecimal result = balance.add(amount);
        if (result.compareTo(MAX_BALANCE) > 0) throw new BankingException("Account balance limit exceeded.");
        return result;
    }
}
