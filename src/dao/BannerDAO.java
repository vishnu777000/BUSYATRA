package dao;

import config.DBConfig;
import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class BannerDAO {

    private final ThreadLocal<String> lastError = new ThreadLocal<>();
    private final ThreadLocal<String> schemaLookupError = new ThreadLocal<>();
    private static final String NULL_SENTINEL = "__NULL__";

    private final Map<String, Boolean> tableCache = new ConcurrentHashMap<>();
    private final Map<String, String> columnCache = new ConcurrentHashMap<>();
    private final Map<String, String> typeCache = new ConcurrentHashMap<>();
    private volatile boolean bannerTableEnsured = false;

    public String getLastError() {
        String err = lastError.get();
        return (err == null || err.isBlank()) ? "Banner operation failed." : err;
    }

    private void setLastError(String message) {
        lastError.set(message);
    }

    private void resetSchemaLookupError() {
        schemaLookupError.remove();
    }

    private void markSchemaLookupFailure(Exception e) {
        if (schemaLookupError.get() == null) {
            schemaLookupError.set(DBConfig.userFriendlyMessage(e));
        }
    }

    private String getSchemaLookupError() {
        return schemaLookupError.get();
    }

    private void clearSchemaCache() {
        tableCache.clear();
        columnCache.clear();
        typeCache.clear();
        bannerTableEnsured = false;
    }

    private void ensureBannerTable() {
        if (bannerTableEnsured) return;

        String sql =
                "CREATE TABLE IF NOT EXISTS banners (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "title VARCHAR(255) NULL," +
                "image_path VARCHAR(1024) NOT NULL," +
                "active TINYINT(1) NOT NULL DEFAULT 0" +
                ")";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
            tableCache.put("banners", true);
            bannerTableEnsured = true;
        } catch (Exception e) {
            markSchemaLookupFailure(e);
            
        }
    }

    private boolean tableExists(String tableName) {
        if (tableCache.containsKey(tableName)) return tableCache.get(tableName);

        String sql =
                "SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ? LIMIT 1";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                tableCache.put(tableName, exists);
                return exists;
            }
        } catch (Exception e) {
            markSchemaLookupFailure(e);
            return false;
        }
    }

    private String cachedColumn(String key, Supplier<String> resolver) {
        if (columnCache.containsKey(key)) {
            String cached = columnCache.get(key);
            return NULL_SENTINEL.equals(cached) ? null : cached;
        }
        String resolved = resolver.get();
        if (getSchemaLookupError() == null) {
            columnCache.put(key, resolved == null ? NULL_SENTINEL : resolved);
        }
        return resolved;
    }

    private String firstExistingColumn(String table, String... candidates) {
        for (String candidate : candidates) {
            if (columnExists(table, candidate)) return candidate;
        }
        return null;
    }

    private boolean columnExists(String tableName, String columnName) {
        String key = tableName + "." + columnName;
        if (columnCache.containsKey(key)) {
            return !NULL_SENTINEL.equals(columnCache.get(key));
        }

        String sql =
                "SELECT 1 FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                columnCache.put(key, exists ? columnName : NULL_SENTINEL);
                return exists;
            }
        } catch (Exception e) {
            markSchemaLookupFailure(e);
            return false;
        }
    }

    private String findColumnByLike(String table, String... patterns) {
        String sql =
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name LIKE ? " +
                "ORDER BY ordinal_position LIMIT 1";

        for (String pattern : patterns) {
            try (Connection con = DBConfig.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, table);
                ps.setString(2, pattern);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("column_name");
                }
            } catch (Exception e) {
                markSchemaLookupFailure(e);
                
            }
        }

        return null;
    }

    private String columnDataType(String table, String column) {
        if (column == null) return null;

        String key = table + "." + column + ".type";
        if (typeCache.containsKey(key)) {
            String cached = typeCache.get(key);
            return NULL_SENTINEL.equals(cached) ? null : cached;
        }

        String sql =
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                String type = rs.next() ? rs.getString("data_type") : null;
                typeCache.put(key, type == null ? NULL_SENTINEL : type);
                return type;
            }
        } catch (Exception e) {
            markSchemaLookupFailure(e);
            return null;
        }
    }

    private boolean isNumericType(String type) {
        if (type == null) return false;
        String t = type.toLowerCase();
        return t.contains("int") ||
                t.equals("bit") ||
                t.equals("decimal") ||
                t.equals("numeric") ||
                t.equals("float") ||
                t.equals("double");
    }

    private String titleCol() {
        ensureBannerTable();
        return cachedColumn("banners.title", () -> {
            String col = firstExistingColumn("banners", "title", "banner_title", "name", "heading");
            return col != null ? col : findColumnByLike("banners", "%title%", "%name%", "%heading%");
        });
    }

    private String idCol() {
        ensureBannerTable();
        return cachedColumn("banners.id", () -> {
            String col = firstExistingColumn("banners", "id", "banner_id", "bid");
            return col != null ? col : findColumnByLike("banners", "banner%id", "%banner%id%", "%id");
        });
    }

    private String imageCol() {
        ensureBannerTable();
        return cachedColumn("banners.image", () -> {
            String col = firstExistingColumn("banners", "image_path", "image", "banner_image", "path", "file_path", "image_url", "banner_url");
            return col != null ? col : findColumnByLike("banners", "%image%", "%path%", "%file%", "%url%");
        });
    }

    private String activeCol() {
        ensureBannerTable();
        return cachedColumn("banners.active", () -> {
            String col = firstExistingColumn("banners", "active", "is_active", "enabled", "status", "banner_status");
            return col != null ? col : findColumnByLike("banners", "%active%", "%enabled%", "%status%");
        });
    }

    private String createdAtCol() {
        ensureBannerTable();
        return cachedColumn("banners.created_at", () -> {
            String col = firstExistingColumn("banners", "created_at", "created_on", "uploaded_at", "added_at", "added_on");
            return col != null ? col : findColumnByLike("banners", "%created%", "%upload%", "%added%");
        });
    }

    private boolean isStatusColumn(String column) {
        return column != null && column.toLowerCase().contains("status");
    }

    private boolean isTemporalType(String type) {
        if (type == null) return false;
        String t = type.toLowerCase();
        return t.contains("date") || t.contains("time") || t.equals("year");
    }

    private String activeTrueExpr(String col) {
        if (col == null) return "1=1";

        if (isNumericType(columnDataType("banners", col))) {
            return "COALESCE(" + col + ",0) <> 0";
        }

        return "UPPER(TRIM(COALESCE(" + col + ",''))) IN ('1','ACTIVE','TRUE','YES','Y','ENABLED')";
    }

    private String activeLabelExpr(String col) {
        if (col == null) return "'NO'";
        return "CASE WHEN " + activeTrueExpr(col) + " THEN 'YES' ELSE 'NO' END";
    }

    private Object activeDbValue(String col) {
        if (col == null) return null;
        if (isNumericType(columnDataType("banners", col))) return 1;
        return isStatusColumn(col) ? "ACTIVE" : "1";
    }

    private Object inactiveDbValue(String col) {
        if (col == null) return null;
        if (isNumericType(columnDataType("banners", col))) return 0;
        return isStatusColumn(col) ? "INACTIVE" : "0";
    }

    private void bindValue(PreparedStatement ps, int index, Object value, String columnType) throws Exception {
        if (value == null) {
            ps.setObject(index, null);
            return;
        }

        if (isTemporalType(columnType)) {
            if (value instanceof Timestamp) {
                ps.setTimestamp(index, (Timestamp) value);
            } else if (value instanceof Date) {
                ps.setTimestamp(index, new Timestamp(((Date) value).getTime()));
            } else {
                ps.setString(index, String.valueOf(value));
            }
            return;
        }

        if (isNumericType(columnType)) {
            if (value instanceof Number) {
                ps.setInt(index, ((Number) value).intValue());
            } else {
                ps.setInt(index, Integer.parseInt(String.valueOf(value)));
            }
            return;
        }

        ps.setString(index, String.valueOf(value));
    }

    private BannerSchema resolveSchema() {
        resetSchemaLookupError();
        ensureBannerTable();

        BannerSchema schema = readSchema();
        if ((schema.id == null || schema.image == null || !schema.exists) && getSchemaLookupError() == null) {
            clearSchemaCache();
            resetSchemaLookupError();
            ensureBannerTable();
            schema = readSchema();
        }
        return schema;
    }

    private BannerSchema readSchema() {
        BannerSchema schema = new BannerSchema();
        schema.exists = tableExists("banners");
        if (!schema.exists) {
            return schema;
        }
        schema.id = idCol();
        schema.title = titleCol();
        schema.image = imageCol();
        schema.active = activeCol();
        schema.createdAt = createdAtCol();
        return schema;
    }

    public boolean addBanner(String title, String path) {
        setLastError(null);
        BannerSchema schema = resolveSchema();
        if (!schema.exists) {
            setLastError(getSchemaLookupError() != null ? getSchemaLookupError() : "Banner table not available.");
            return false;
        }
        if (schema.image == null) {
            setLastError(getSchemaLookupError() != null ? getSchemaLookupError() : "Banner image column not found.");
            return false;
        }
        if (path == null || path.isBlank()) {
            setLastError("Select a valid banner image.");
            return false;
        }

        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        List<String> types = new ArrayList<>();

        if (schema.title != null) {
            cols.add(schema.title);
            vals.add(title);
            types.add(columnDataType("banners", schema.title));
        }

        cols.add(schema.image);
        vals.add(path.trim().replace("\\", "/"));
        types.add(columnDataType("banners", schema.image));

        if (schema.active != null) {
            cols.add(schema.active);
            vals.add(inactiveDbValue(schema.active));
            types.add(columnDataType("banners", schema.active));
        }

        if (schema.createdAt != null) {
            cols.add(schema.createdAt);
            vals.add(new Timestamp(System.currentTimeMillis()));
            types.add(columnDataType("banners", schema.createdAt));
        }

        StringBuilder sql = new StringBuilder("INSERT INTO banners (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append(cols.get(i));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < vals.size(); i++) {
                bindValue(ps, i + 1, vals.get(i), types.get(i));
            }

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    private String[] buildRow(ResultSet rs, boolean includeActive) throws Exception {
        if (includeActive) {
            return new String[]{
                    rs.getString("id"),
                    rs.getString("title_value"),
                    rs.getString("image_value"),
                    rs.getString("active_value")
            };
        }

        return new String[]{
                rs.getString("id"),
                rs.getString("title_value"),
                rs.getString("image_value")
        };
    }

    public List<String[]> getAllBanners() {
        setLastError(null);

        List<String[]> list = new ArrayList<>();
        BannerSchema schema = resolveSchema();
        if (!schema.exists || schema.id == null || schema.image == null) {
            if (getSchemaLookupError() != null) {
                setLastError(getSchemaLookupError());
            }
            return list;
        }

        String sql =
                "SELECT " + schema.id + " AS id, " +
                (schema.title != null ? schema.title : "''") + " AS title_value, " +
                schema.image + " AS image_value, " +
                activeLabelExpr(schema.active) + " AS active_value " +
                "FROM banners ORDER BY " + schema.id + " DESC LIMIT 300";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(buildRow(rs, true));
            }
        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return list;
    }

    public List<String[]> getActiveBanners() {
        setLastError(null);

        List<String[]> list = new ArrayList<>();
        BannerSchema schema = resolveSchema();
        if (!schema.exists || schema.id == null || schema.image == null) {
            if (getSchemaLookupError() != null) {
                setLastError(getSchemaLookupError());
            }
            return list;
        }

        String sql =
                "SELECT " + schema.id + " AS id, " +
                (schema.title != null ? schema.title : "''") + " AS title_value, " +
                schema.image + " AS image_value " +
                "FROM banners " +
                (schema.active != null ? "WHERE " + activeTrueExpr(schema.active) + " " : "") +
                "ORDER BY " + schema.id + " DESC LIMIT 100";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(buildRow(rs, false));
            }
        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return list;
    }

    public boolean setActiveBanner(int id) {
        setLastError(null);
        BannerSchema schema = resolveSchema();
        if (!schema.exists) {
            setLastError(getSchemaLookupError() != null ? getSchemaLookupError() : "Banner table not available.");
            return false;
        }
        if (schema.id == null || schema.active == null) {
            setLastError(getSchemaLookupError() != null ? getSchemaLookupError() : "Banner ID/active columns not found.");
            return false;
        }

        String type = columnDataType("banners", schema.active);
        String disableSql = "UPDATE banners SET " + schema.active + "=?";
        String enableSql = "UPDATE banners SET " + schema.active + "=? WHERE " + schema.id + "=?";

        try (Connection con = DBConfig.getConnection()) {
            con.setAutoCommit(false);

            try (
                    PreparedStatement disable = con.prepareStatement(disableSql);
                    PreparedStatement enable = con.prepareStatement(enableSql)
            ) {
                bindValue(disable, 1, inactiveDbValue(schema.active), type);
                disable.executeUpdate();

                bindValue(enable, 1, activeDbValue(schema.active), type);
                enable.setInt(2, id);
                int updated = enable.executeUpdate();

                con.commit();
                return updated > 0;
            } catch (Exception e) {
                con.rollback();
                setLastError(DBConfig.userFriendlyMessage(e));
                DBConnectionUtil.logIfUnexpected(e);
                return false;
            }
        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    public boolean deleteBanner(int id) {
        setLastError(null);
        BannerSchema schema = resolveSchema();
        if (!schema.exists) {
            setLastError(getSchemaLookupError() != null ? getSchemaLookupError() : "Banner table not available.");
            return false;
        }
        if (schema.id == null) {
            setLastError(getSchemaLookupError() != null ? getSchemaLookupError() : "Banner ID column not found.");
            return false;
        }

        String sql = "DELETE FROM banners WHERE " + schema.id + "=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    public List<String> getHomepageBannerImages() {
        setLastError(null);

        List<String> list = new ArrayList<>();
        BannerSchema schema = resolveSchema();
        if (!schema.exists || schema.image == null) {
            return list;
        }

        String sql =
                "SELECT " + schema.image + " AS image_value FROM banners " +
                (schema.active != null ? "WHERE " + activeTrueExpr(schema.active) + " " : "") +
                "ORDER BY " + (schema.id != null ? schema.id : schema.image) + " DESC LIMIT 20";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String image = rs.getString("image_value");
                if (image != null && !image.isBlank()) {
                    list.add(image);
                }
            }
        } catch (Exception e) {
            
        }

        return list;
    }

    private static class BannerSchema {
        boolean exists;
        String id;
        String title;
        String image;
        String active;
        String createdAt;
    }
}
