package com.atm.dao;

import com.atm.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class TransactionDAO {
    public void insert(Connection c, long accountId, TransactionType type, BigDecimal amount,
                       String counterparty, String reference, BigDecimal balance, String notes) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO bank_transactions(account_id,type,amount,counterparty_account_number,reference_number,balance_after,notes) VALUES(?,?,?,?,?,?,?)")) {
            ps.setLong(1, accountId); ps.setString(2, type.name()); ps.setBigDecimal(3, amount);
            ps.setString(4, counterparty); ps.setString(5, reference); ps.setBigDecimal(6, balance); ps.setString(7, notes);
            ps.executeUpdate();
        }
    }
    public List<BankTransaction> history(Connection c, long accountId, int limit) throws SQLException {
        List<BankTransaction> result = new ArrayList<>();
        String sql = "SELECT * FROM bank_transactions WHERE account_id=? ORDER BY created_at DESC,transaction_id DESC";
        // A zero limit denotes complete administrator history; customer statements use 10.
        try (PreparedStatement ps = c.prepareStatement(sql + (limit > 0 ? " LIMIT ?" : ""))) {
            ps.setLong(1, accountId); if (limit > 0) ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new BankTransaction(rs.getLong("transaction_id"), accountId,
                    TransactionType.valueOf(rs.getString("type")), rs.getBigDecimal("amount"),
                    rs.getString("counterparty_account_number"), rs.getString("reference_number"),
                    rs.getBigDecimal("balance_after"), rs.getString("notes"), rs.getTimestamp("created_at").toInstant()));
            }
        }
        return List.copyOf(result);
    }
}
