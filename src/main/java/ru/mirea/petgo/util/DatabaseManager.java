package ru.mirea.petgo.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseManager {
    private static final Properties PROPERTIES = loadProperties();

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver not found", e);
        }
    }

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                setting("DB_URL", "db.url"),
                setting("DB_USER", "db.user"),
                setting("DB_PASSWORD", "db.password"));
    }

    private static String setting(String environmentVariable, String propertyName) {
        String override = System.getenv(environmentVariable);
        if (override != null) {
            return override;
        }

        String value = PROPERTIES.getProperty(propertyName);
        if (value == null) {
            throw new IllegalStateException("Missing property: " + propertyName);
        }
        return value;
    }

    private static Properties loadProperties() {
        try (InputStream input = DatabaseManager.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("application.properties not found in classpath");
            }

            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load application.properties", e);
        }
    }
}
