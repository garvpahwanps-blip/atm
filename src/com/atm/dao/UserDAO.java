package com.atm.dao;

import com.atm.model.User;
import java.sql.*;

public final class UserDAO {
    public User findByNumber(Connection c, String number, boolean forUpdate) throws SQLException {
        return find(c, "SELECT * FROM users WHERE account_number=?" + (forUpdate ? " FOR UPDATE" : ""), number);
    }
    public User findById(Connection c, long id, boolean forUpdate) throws SQLException {
        return find(c, "SELECT * FROM users WHERE user_id=?" + (forUpdate ? " FOR UPDATE" : ""), id);
    }
    private User find(Connection c, String sql, Object key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (key instanceof Long id) ps.setLong(1, id); else ps.setString(1, (String) key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new User(rs.getLong("user_id"), rs.getString("full_name"),
                    rs.getString("account_number"), rs.getString("pin_hash"), rs.getInt("failed_attempts"),
                    rs.getBoolean("locked"), rs.getTimestamp("created_at").toInstant()) : null;
            }
        }
    }
    public long insert(Connection c, String name, String number, String hash) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users(full_name,account_number,pin_hash) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setString(2, number); ps.setString(3, hash);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("No generated user ID");
                return rs.getLong(1);
            }
        }
    }
    public void attempts(Connection c, long id, int attempts, boolean locked) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE users SET failed_attempts=?,locked=? WHERE user_id=?")) {
            ps.setInt(1, attempts); ps.setBoolean(2, locked); ps.setLong(3, id);
            if (ps.executeUpdate() != 1) throw new SQLException("User missing");
        }
    }
    public void changePin(Connection c, long id, String hash) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE users SET pin_hash=?,failed_attempts=0 WHERE user_id=?")) {
            ps.setString(1, hash); ps.setLong(2, id);
            if (ps.executeUpdate() != 1) throw new SQLException("User missing");
        }
    }
}
