package com.atm.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {
    @FunctionalInterface
    public interface ConnectionFactory { Connection open() throws SQLException; }
    private Database() {}

    public static Properties configuration() {
        String location = System.getProperty("atm.config");
        if (location == null || location.isBlank()) location = System.getenv("ATM_CONFIG");
        if (location == null || location.isBlank()) location = "resources/database.properties";
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of(location))) { properties.load(input); }
        catch (IOException e) { throw new IllegalStateException("Cannot read local database configuration. Set -Datm.config to its absolute path.", e); }
        return properties;
    }

    public static Connection open() throws SQLException {
        // Tomcat's common class loader cannot discover drivers inside WEB-INF/lib.
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("Add the MySQL Connector/J JAR to lib and rebuild.", e); }
        Properties p = configuration();
        String url = p.getProperty("db.url", "");
        if (!url.startsWith("jdbc:mysql:")) throw new SQLException("A MySQL JDBC URL is required.");
        Connection connection = DriverManager.getConnection(url, p.getProperty("db.user", ""), p.getProperty("db.password", ""));
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        return connection;
    }
}
