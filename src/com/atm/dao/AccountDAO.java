package com.atm.dao;

import com.atm.model.Account;
import com.atm.model.CustomerSummary;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class AccountDAO {
    private static final String SUMMARY = "SELECT u.user_id,u.full_name,u.account_number,u.locked,u.failed_attempts,a.account_id,a.balance FROM users u JOIN accounts a ON a.user_id=u.user_id";
    public void insert(Connection c, long userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO accounts(user_id,balance,account_type) VALUES(?,0.00,'SAVINGS')")) {
            ps.setLong(1, userId); ps.executeUpdate();
        }
    }
    public Account findByUser(Connection c, long userId, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM accounts WHERE user_id=?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? account(rs) : null; }
        }
    }
    public Account lockById(Connection c, long accountId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM accounts WHERE account_id=? FOR UPDATE")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Account missing");
                return account(rs);
            }
        }
    }
    private Account account(ResultSet rs) throws SQLException {
        return new Account(rs.getLong("account_id"), rs.getLong("user_id"), rs.getBigDecimal("balance"), rs.getString("account_type"));
    }
    public void balance(Connection c, long accountId, BigDecimal balance) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE accounts SET balance=? WHERE account_id=?")) {
            ps.setBigDecimal(1, balance); ps.setLong(2, accountId);
            if (ps.executeUpdate() != 1) throw new SQLException("Account missing");
        }
    }
    public CustomerSummary summary(Connection c, long userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SUMMARY + " WHERE u.user_id=?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? summary(rs) : null; }
        }
    }
    public List<CustomerSummary> search(Connection c, String number) throws SQLException {
        List<CustomerSummary> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SUMMARY + (number == null ? " ORDER BY u.user_id" : " WHERE u.account_number=?"))) {
            if (number != null) ps.setString(1, number);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(summary(rs)); }
        }
        return List.copyOf(result);
    }
    private CustomerSummary summary(ResultSet rs) throws SQLException {
        return new CustomerSummary(rs.getLong("user_id"), rs.getLong("account_id"), rs.getString("full_name"),
            rs.getString("account_number"), rs.getBigDecimal("balance"), rs.getBoolean("locked"), rs.getInt("failed_attempts"));
    }
}
