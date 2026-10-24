package com.atm.service;

import com.atm.dao.*;
import com.atm.model.*;
import com.atm.security.PasswordHasher;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.*;
import java.util.List;
import java.util.UUID;

public final class BankingService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DUMMY_HASH = PasswordHasher.hash("0000".toCharArray());
    private final Database.ConnectionFactory connections;
    private final UserDAO users = new UserDAO();
    private final AccountDAO accounts = new AccountDAO();
    private final TransactionDAO transactions = new TransactionDAO();

    public BankingService() { this(Database::open); }
    public BankingService(Database.ConnectionFactory connections) { this.connections = connections; }
    @FunctionalInterface private interface Work<T> { T run(Connection c) throws SQLException; }

    private <T> T transaction(Work<T> work) {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        } catch (SQLException e) { throw new BankingException("The database operation could not be completed. Please try again.", e); }
    }

    public CustomerSummary register(String fullName, char[] pin) {
        String name = Validation.name(fullName);
        Validation.pin(pin);
        String hash = PasswordHasher.hash(pin);
        for (int retry = 0; retry < 5; retry++) {
            String number = Long.toString(RANDOM.nextLong(100_000_000_000L, 1_000_000_000_000L));
            try {
                return transaction(c -> {
                    long userId = users.insert(c, name, number, hash);
                    accounts.insert(c, userId);
                    return accounts.summary(c, userId);
                });
            } catch (BankingException e) {
                if (!(e.getCause() instanceof SQLException sql) || sql.getErrorCode() != 1062) throw e;
            }
        }
        throw new BankingException("Could not allocate an account number. Please retry.");
    }

    private record AuthenticationResult(CustomerSummary customer, boolean locked) {}

    public CustomerSummary login(String number, char[] pin) {
        Validation.accountNumber(number); Validation.pin(pin);
        AuthenticationResult result = transaction(c -> {
            User user = users.findByNumber(c, number, true);
            if (user == null) { PasswordHasher.verify(pin, DUMMY_HASH); return new AuthenticationResult(null, false); }
            if (user.locked()) return new AuthenticationResult(null, true);
            if (!PasswordHasher.verify(pin, user.pinHash())) return new AuthenticationResult(null, failedPin(c, user));
            users.attempts(c, user.id(), 0, false);
            return new AuthenticationResult(accounts.summary(c, user.id()), false);
        });
        // Fail only after committing the failed-attempt counter.
        if (result.locked()) throw new AccountLockedException();
        if (result.customer() == null) throw new AuthenticationException("Invalid account number or PIN.");
        return result.customer();
    }

    private boolean failedPin(Connection c, User user) throws SQLException {
        int attempts = Math.min(3, user.failedAttempts() + 1);
        users.attempts(c, user.id(), attempts, attempts >= 3);
        return attempts >= 3;
    }
    private User activeUser(Connection c, long userId, boolean lock) throws SQLException {
        User user = users.findById(c, userId, lock);
        if (user == null) throw new AuthenticationException("Please log in again.");
        if (user.locked()) throw new AccountLockedException();
        return user;
    }

    public CustomerSummary customer(long userId) {
        return transaction(c -> { activeUser(c, userId, false); return accounts.summary(c, userId); });
    }

    public String deposit(long userId, BigDecimal input, String inputNotes) {
        BigDecimal amount = Validation.amount(input); String notes = Validation.notes(inputNotes);
        return transaction(c -> {
            activeUser(c, userId, true);
            Account account = accounts.findByUser(c, userId, true);
            BigDecimal after = Validation.credit(account.balance(), amount);
            String reference = UUID.randomUUID().toString();
            accounts.balance(c, account.id(), after);
            transactions.insert(c, account.id(), TransactionType.DEPOSIT, amount, null, reference, after, notes);
            return reference;
        });
    }

    public String withdraw(long userId, BigDecimal input, String inputNotes) {
        BigDecimal amount = Validation.amount(input); String notes = Validation.notes(inputNotes);
        return transaction(c -> {
            activeUser(c, userId, true);
            Account account = accounts.findByUser(c, userId, true);
            if (account.balance().compareTo(amount) < 0) throw new InsufficientBalanceException();
            BigDecimal after = account.balance().subtract(amount);
            String reference = UUID.randomUUID().toString();
            accounts.balance(c, account.id(), after);
            transactions.insert(c, account.id(), TransactionType.WITHDRAWAL, amount, null, reference, after, notes);
            return reference;
        });
    }

    public String transfer(long userId, String destination, BigDecimal input, String inputNotes) {
        Validation.accountNumber(destination);
        BigDecimal amount = Validation.amount(input); String notes = Validation.notes(inputNotes);
        return transaction(c -> {
            User sender = activeUser(c, userId, true);
            if (sender.accountNumber().equals(destination)) throw new BankingException("Choose a different recipient account.");
            User recipient = users.findByNumber(c, destination, false);
            if (recipient == null) throw new BankingException("Recipient account was not found.");
            Account from = accounts.findByUser(c, userId, false);
            Account to = accounts.findByUser(c, recipient.id(), false);
            // Always acquire balance locks in a global order to avoid opposing-transfer deadlocks.
            Account first = accounts.lockById(c, Math.min(from.id(), to.id()));
            Account second = accounts.lockById(c, Math.max(from.id(), to.id()));
            from = first.id() == from.id() ? first : second;
            to = first.id() == to.id() ? first : second;
            if (from.balance().compareTo(amount) < 0) throw new InsufficientBalanceException();
            BigDecimal debit = from.balance().subtract(amount);
            BigDecimal credit = Validation.credit(to.balance(), amount);
            String reference = UUID.randomUUID().toString();
            accounts.balance(c, from.id(), debit);
            accounts.balance(c, to.id(), credit);
            transactions.insert(c, from.id(), TransactionType.TRANSFER_OUT, amount, destination, reference + "-D", debit, notes);
            transactions.insert(c, to.id(), TransactionType.TRANSFER_IN, amount, sender.accountNumber(), reference + "-C", credit, notes);
            return reference + "-D";
        });
    }

    public List<BankTransaction> miniStatement(long userId) {
        return transaction(c -> {
            activeUser(c, userId, false);
            return transactions.history(c, accounts.findByUser(c, userId, false).id(), 10);
        });
    }

    public void changePin(long userId, char[] currentPin, char[] newPin) {
        Validation.pin(currentPin); Validation.pin(newPin);
        if (java.util.Arrays.equals(currentPin, newPin)) throw new BankingException("Choose a different new PIN.");
        int result = transaction(c -> {
            User user = activeUser(c, userId, true);
            if (!PasswordHasher.verify(currentPin, user.pinHash())) return failedPin(c, user) ? 2 : 1;
            users.changePin(c, userId, PasswordHasher.hash(newPin));
            return 0;
        });
        if (result == 2) throw new AccountLockedException();
        if (result == 1) throw new AuthenticationException("Current PIN is incorrect.");
    }

    public List<CustomerSummary> customers(String number) {
        String query = number == null || number.isBlank() ? null : Validation.accountNumber(number);
        return transaction(c -> accounts.search(c, query));
    }
    public List<BankTransaction> adminHistory(String number) {
        Validation.accountNumber(number);
        return transaction(c -> {
            User user = users.findByNumber(c, number, false);
            if (user == null) throw new BankingException("Customer was not found.");
            return transactions.history(c, accounts.findByUser(c, user.id(), false).id(), 0);
        });
    }
    public void unlock(String number) {
        Validation.accountNumber(number);
        transaction(c -> {
            User user = users.findByNumber(c, number, true);
            if (user == null) throw new BankingException("Customer was not found.");
            users.attempts(c, user.id(), 0, false);
            return null;
        });
    }
}
