package com.atm.tests;

import com.atm.dao.Database;
import com.atm.model.*;
import com.atm.service.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class BankingIntegrationTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    private static void money(BigDecimal actual, String expected) {
        check(actual.compareTo(new BigDecimal(expected)) == 0, "Expected " + expected + ", got " + actual);
    }
    private static void fails(Class<? extends Throwable> type, Runnable task) {
        try { task.run(); } catch (Throwable e) {
            if (type.isInstance(e)) { checks++; return; }
            throw new AssertionError("Unexpected exception", e);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private static BigDecimal value(String v) { return new BigDecimal(v); }
    private static long countUsers() throws SQLException {
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM users"); ResultSet r = p.executeQuery()) {
            r.next(); return r.getLong(1);
        }
    }
    private static BankingService failingService(boolean registration) {
        return new BankingService(() -> {
            Connection real = Database.open(); AtomicInteger balanceWrites = new AtomicInteger();
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                if (method.getName().equals("prepareStatement") && args[0] instanceof String sql &&
                    ((registration && sql.startsWith("INSERT INTO accounts")) ||
                     (!registration && sql.startsWith("UPDATE accounts SET balance") && balanceWrites.incrementAndGet() == 2)))
                    throw new SQLException("Deliberate test failure after earlier write");
                try { return method.invoke(real, args); }
                catch (InvocationTargetException e) { throw e.getCause(); }
            });
        });
    }
    public static void main(String[] args) throws Exception {
        BankingService service = new BankingService();
        long before = countUsers();
        fails(BankingException.class, () -> failingService(true).register("Rollback Customer", "1234".toCharArray()));
        check(countUsers() == before, "Registration rollback removed the partial user");
        CustomerSummary a = service.register("Integration Alice", "1234".toCharArray());
        CustomerSummary b = service.register("Integration Bob", "5678".toCharArray());
        check(a.accountNumber().matches("[0-9]{12}") && !a.accountNumber().equals(b.accountNumber()), "Unique 12-digit accounts");
        money(a.balance(), "0.00");
        check(service.login(a.accountNumber(), "1234".toCharArray()).userId() == a.userId(), "Login");
        fails(AuthenticationException.class, () -> service.login(a.accountNumber(), "9999".toCharArray()));
        fails(AuthenticationException.class, () -> service.login(a.accountNumber(), "9999".toCharArray()));
        fails(AccountLockedException.class, () -> service.login(a.accountNumber(), "9999".toCharArray()));
        fails(AccountLockedException.class, () -> service.deposit(a.userId(), value("10"), "locked"));
        check(service.customers(a.accountNumber()).getFirst().failedAttempts() == 3, "Failed attempts persisted");
        service.unlock(a.accountNumber());
        service.login(a.accountNumber(), "1234".toCharArray());
        service.changePin(a.userId(), "1234".toCharArray(), "4321".toCharArray());
        fails(AuthenticationException.class, () -> service.login(a.accountNumber(), "1234".toCharArray()));
        service.login(a.accountNumber(), "4321".toCharArray());
        service.deposit(a.userId(), value("200.25"), "initial");
        service.withdraw(a.userId(), value("0.25"), "fraction");
        fails(InsufficientBalanceException.class, () -> service.withdraw(a.userId(), value("201"), "overdraft"));
        money(service.customer(a.userId()).balance(), "200.00");
        fails(BankingException.class, () -> service.transfer(a.userId(), a.accountNumber(), value("1"), "self"));
        int oldLedger = service.adminHistory(a.accountNumber()).size();
        fails(BankingException.class, () -> failingService(false).transfer(a.userId(), b.accountNumber(), value("10"), "forced rollback"));
        money(service.customer(a.userId()).balance(), "200.00");
        money(service.customer(b.userId()).balance(), "0.00");
        check(service.adminHistory(a.accountNumber()).size() == oldLedger, "No ledger entry after rollback");
        String reference = service.transfer(a.userId(), b.accountNumber(), value("25.50"), "paired");
        BankTransaction debit = service.adminHistory(a.accountNumber()).getFirst();
        BankTransaction credit = service.adminHistory(b.accountNumber()).getFirst();
        check(reference.equals(debit.referenceNumber()), "Returned debit reference");
        check(credit.referenceNumber().equals(reference.substring(0, 36) + "-C"), "Linked unique credit reference");
        check(debit.type() == TransactionType.TRANSFER_OUT && credit.type() == TransactionType.TRANSFER_IN, "Both ledger types");
        money(service.customer(a.userId()).balance(), "174.50");
        money(service.customer(b.userId()).balance(), "25.50");
        service.deposit(b.userId(), value("174.50"), "concurrency seed");
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                final boolean forward = i % 2 == 0;
                futures.add(pool.submit(() -> {
                    for (int n = 0; n < 10; n++) service.transfer(forward ? a.userId() : b.userId(),
                        forward ? b.accountNumber() : a.accountNumber(), value("1.00"), "concurrent");
                }));
            }
            for (Future<?> future : futures) future.get(60, TimeUnit.SECONDS);
        }
        money(service.customer(a.userId()).balance(), "174.50");
        money(service.customer(b.userId()).balance(), "200.00");
        check(service.miniStatement(a.userId()).size() == 10, "Mini statement limited to ten");
        Set<String> refs = new HashSet<>();
        for (BankTransaction tx : service.adminHistory(a.accountNumber())) check(refs.add(tx.referenceNumber()), "Unique reference");
        CustomerSummary race = service.register("Withdrawal Race", "1111".toCharArray());
        service.deposit(race.userId(), value("100"), "seed");
        AtomicInteger winners = new AtomicInteger();
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) futures.add(pool.submit(() -> {
                try { service.withdraw(race.userId(), value("80"), "race"); winners.incrementAndGet(); }
                catch (InsufficientBalanceException expected) { /* Only one withdrawal may commit. */ }
            }));
            for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        }
        check(winners.get() == 1, "Concurrent overdraft prevented");
        money(service.customer(race.userId()).balance(), "20.00");
        System.out.println("MySQL banking integration: " + checks + " checks passed");
    }
}
