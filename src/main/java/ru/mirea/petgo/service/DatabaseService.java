package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {

    private static final List<String> TABLES = List.of(
            "users", "pets", "walk_requests", "walk_history"
    );

    public List<String> getTableNames() {
        return TABLES;
    }

    public TableData getTableData(String tableName) throws DatabaseException {
        if (!TABLES.contains(tableName)) {
            throw new DatabaseException("Неизвестная таблица: " + tableName, null);
        }

        String sql = "SELECT * FROM " + tableName;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            String[] headers = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                headers[i - 1] = meta.getColumnLabel(i);
            }

            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                String[] row = new String[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    row[i - 1] = value == null ? "—" : value.toString();
                }
                rows.add(row);
            }
            return new TableData(tableName, headers, rows);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка чтения таблицы " + tableName + ": " + e.getMessage(), e);
        }
    }

    public int getRowCount(String tableName) throws DatabaseException {
        if (!TABLES.contains(tableName)) {
            throw new DatabaseException("Неизвестная таблица: " + tableName, null);
        }
        String sql = "SELECT COUNT(*) FROM " + tableName;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подсчёта строк в " + tableName + ": " + e.getMessage(), e);
        }
    }

    public static class TableData {
        private final String tableName;
        private final String[] headers;
        private final List<String[]> rows;

        public TableData(String tableName, String[] headers, List<String[]> rows) {
            this.tableName = tableName;
            this.headers = headers;
            this.rows = rows;
        }

        public String getTableName() { return tableName; }
        public String[] getHeaders() { return headers; }
        public List<String[]> getRows() { return rows; }
    }
}