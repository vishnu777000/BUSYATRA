package util;

import config.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SchemaCache {

    private static final ConcurrentMap<String, Boolean> TABLE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Set<String>> COLUMN_CACHE = new ConcurrentHashMap<>();

    private SchemaCache() {
    }

    public static boolean tableExists(String tableName) {
        String normalizedTable = normalize(tableName);
        if (normalizedTable.isBlank()) return false;

        Boolean cached = TABLE_CACHE.get(normalizedTable);
        if (cached != null) return cached;

        String sql =
                "SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ? LIMIT 1";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, normalizedTable);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                TABLE_CACHE.put(normalizedTable, exists);
                return exists;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean columnExists(String tableName, String columnName) {
        String normalizedTable = normalize(tableName);
        String normalizedColumn = normalize(columnName);
        if (normalizedTable.isBlank() || normalizedColumn.isBlank()) return false;

        Set<String> columns = COLUMN_CACHE.get(normalizedTable);
        if (columns == null) {
            columns = loadColumns(normalizedTable);
            COLUMN_CACHE.put(normalizedTable, columns);
        }
        return columns.contains(normalizedColumn);
    }

    public static String firstExistingColumn(String tableName, String... candidates) {
        if (candidates == null) return null;
        for (String candidate : candidates) {
            if (columnExists(tableName, candidate)) return candidate;
        }
        return null;
    }

    public static void markColumnPresent(String tableName, String columnName) {
        String normalizedTable = normalize(tableName);
        String normalizedColumn = normalize(columnName);
        if (normalizedTable.isBlank() || normalizedColumn.isBlank()) return;

        TABLE_CACHE.put(normalizedTable, true);
        COLUMN_CACHE.compute(normalizedTable, (key, existing) -> {
            Set<String> updated = existing == null ? new HashSet<>() : new HashSet<>(existing);
            updated.add(normalizedColumn);
            return updated;
        });
    }

    public static void invalidate(String tableName) {
        String normalizedTable = normalize(tableName);
        if (normalizedTable.isBlank()) return;
        TABLE_CACHE.remove(normalizedTable);
        COLUMN_CACHE.remove(normalizedTable);
    }

    private static Set<String> loadColumns(String tableName) {
        Set<String> columns = new HashSet<>();
        if (!tableExists(tableName)) return columns;

        String sql =
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String column = normalize(rs.getString(1));
                    if (!column.isBlank()) {
                        columns.add(column);
                    }
                }
            }
        } catch (Exception ignored) {
            columns.clear();
        }

        return columns;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }
}
