package dao;

import config.DBConfig;
import model.Role;
import util.DBConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class UserDAO {

    private final ThreadLocal<String> lastError = new ThreadLocal<>();
    private final ThreadLocal<String> schemaLookupError = new ThreadLocal<>();
    private static final Map<String, String> COL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> TABLE_CACHE = new ConcurrentHashMap<>();
    private static final Object LOGIN_SCHEMA_LOCK = new Object();
    private static volatile LoginSchema LOGIN_SCHEMA_CACHE;

    public String getLastError() {
        String err = lastError.get();
        return (err == null || err.isBlank()) ? "Unknown registration/login error" : err;
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
        COL_CACHE.clear();
        TABLE_CACHE.clear();
    }

    public void warmLoginMetadata() {
        setLastError(null);
        resetSchemaLookupError();
        resolveLoginSchema();
    }

    private boolean refreshSchemaIfNeeded(String id, String mail, String pass) {
        if ((id != null && mail != null) || getSchemaLookupError() != null) {
            return false;
        }
        clearSchemaCache();
        resetSchemaLookupError();
        return true;
    }

    private String findFirstExistingColumn(String table, String... candidates) {
        String sql =
                "SELECT 1 FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";

        for (String candidate : candidates) {
            try (Connection con = DBConfig.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, table);
                ps.setString(2, candidate);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return candidate;
                }
            } catch (Exception e) {
                markSchemaLookupFailure(e);
                
            }
        }

        return null;
    }

    private String cachedColumn(String key, Supplier<String> resolver) {
        String cached = COL_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        String resolved = resolver.get();
        if (resolved != null && getSchemaLookupError() == null) {
            COL_CACHE.put(key, resolved);
        }
        return resolved;
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
                    if (rs.next()) {
                        return rs.getString("column_name");
                    }
                }
            } catch (Exception e) {
                markSchemaLookupFailure(e);
                
            }
        }
        return null;
    }

    private String columnDataType(String table, String column) {
        if (column == null) return null;

        String sql =
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("data_type");
            }
        } catch (Exception e) {
            markSchemaLookupFailure(e);
            
        }
        return null;
    }

    private boolean tableExists(String table) {
        if (TABLE_CACHE.containsKey(table)) {
            return TABLE_CACHE.get(table);
        }
        String sql =
                "SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ? LIMIT 1";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                TABLE_CACHE.put(table, exists);
                return exists;
            }
        } catch (Exception e) {
            markSchemaLookupFailure(e);
            return false;
        }
    }

    private boolean isNumericType(String type) {
        if (type == null) return false;
        String t = type.toLowerCase();
        return t.contains("int") || t.equals("decimal") || t.equals("numeric")
                || t.equals("float") || t.equals("double") || t.equals("bit");
    }

    private String nameCol() {
        return cachedColumn("users.name", () -> {
            String col = findFirstExistingColumn("users", "name", "full_name", "username", "user_name", "first_name", "display_name");
            return col != null ? col : findColumnByLike("users", "%display%name%", "%full%name%", "%user%name%", "%name%");
        });
    }

    private String roleCol() {
        return cachedColumn("users.role", () -> {
            String col = findFirstExistingColumn("users", "role", "user_role", "type", "user_type", "role_id");
            return col != null ? col : findColumnByLike("users", "%role%", "%type%");
        });
    }

    private String statusCol() {
        return cachedColumn("users.status", () -> {
            String col = findFirstExistingColumn("users", "status", "user_status", "is_active", "active", "enabled");
            return col != null ? col : findColumnByLike("users", "%status%", "%active%", "%enabled%");
        });
    }

    private String createdAtCol() {
        return cachedColumn("users.createdAt",
                () -> findFirstExistingColumn("users", "created_at", "created_on", "createdDate"));
    }

    private String userProfileNameCol() {
        return cachedColumn("user_profiles.name",
                () -> findFirstExistingColumn("user_profiles", "name", "full_name", "username", "user_name"));
    }

    private String emailCol() {
        return cachedColumn("users.email", () -> {
            String col = findFirstExistingColumn("users", "email", "email_id", "mail", "emailid", "user_email", "useremail", "email_address");
            return col != null ? col : findColumnByLike("users", "%email%", "%mail%");
        });
    }

    private String idCol() {
        return cachedColumn("users.id", () -> {
            String col = findFirstExistingColumn("users", "id", "user_id", "uid", "userid", "id_user");
            return col != null ? col : findColumnByLike("users", "user%id", "%user%id%", "%id");
        });
    }

    private String passwordCol() {
        return cachedColumn("users.password", () -> {
            String col = findFirstExistingColumn("users", "password", "pass", "pwd", "passwd", "user_password", "pass_word", "userpass", "user_pass", "userpassword", "password_hash");
            return col != null ? col : findColumnByLike("users", "%password%", "%pass%", "%pwd%", "%secret%");
        });
    }

    private String activeStatusValue(String statusColumn) {
        String col = statusColumn == null ? "" : statusColumn.toLowerCase();
        if (col.contains("active") || col.contains("enabled") || col.contains("flag")) {
            return "1";
        }
        return "ACTIVE";
    }

    private String inactiveStatusValue(String statusColumn) {
        String col = statusColumn == null ? "" : statusColumn.toLowerCase();
        if (col.contains("active") || col.contains("enabled") || col.contains("flag")) {
            return "0";
        }
        return "BLOCKED";
    }

    private boolean isActiveStatus(String rawStatus) {
        if (rawStatus == null) return true;
        String s = rawStatus.trim().toUpperCase();
        return "1".equals(s) ||
                "ACTIVE".equals(s) ||
                "TRUE".equals(s) ||
                "YES".equals(s) ||
                "Y".equals(s);
    }

    private String normalizeStatusLabel(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return "ACTIVE";
        }

        String s = rawStatus.trim().toUpperCase();
        if (isActiveStatus(s)) {
            return "ACTIVE";
        }
        if ("0".equals(s)
                || "FALSE".equals(s)
                || "NO".equals(s)
                || "N".equals(s)
                || "INACTIVE".equals(s)
                || "DISABLED".equals(s)
                || "BLOCKED".equals(s)) {
            return "BLOCKED";
        }
        return s;
    }

    private String readCol(ResultSet rs, String alias, String fallback) {
        try {
            String v = rs.getString(alias);
            return (v == null || v.isBlank()) ? fallback : v;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) return Role.USER;

        String value = rawRole.trim().toUpperCase();
        switch (value) {
            case "1": return Role.ADMIN;
            case "2": return Role.USER;
            case "3": return Role.MANAGER;
            case "4": return Role.CLERK;
            case "BOOKING_CLERK": return Role.CLERK;
            default:
                return Role.isValidRole(value) ? value : Role.USER;
        }
    }

    private int numericRoleValue(String role) {
        String normalized = normalizeRole(role);
        switch (normalized) {
            case Role.ADMIN: return 1;
            case Role.MANAGER: return 3;
            case Role.CLERK: return 4;
            case Role.USER:
            default: return 2;
        }
    }

    private String[] buildUser(ResultSet rs) {
        return new String[]{
                readCol(rs, "id", "0"),
                readCol(rs, "name", "User"),
                readCol(rs, "email", ""),
                normalizeRole(readCol(rs, "role", Role.USER)),
                normalizeStatusLabel(readCol(rs, "status", "ACTIVE")),
                readCol(rs, "created_at", "")
        };
    }

    private String userDisplayNameExpr(String userAlias, String idColumn, String emailColumn, String nameColumn) {
        String fallbackExpr = nameColumn != null
                ? userAlias + "." + nameColumn
                : userAlias + "." + emailColumn;

        if (idColumn == null || !tableExists("user_profiles")) {
            return fallbackExpr;
        }

        String profileName = userProfileNameCol();
        if (profileName == null) {
            return fallbackExpr;
        }

        return "COALESCE(NULLIF(TRIM(up." + profileName + "),''), " + fallbackExpr + ")";
    }

    private String userProfileJoinClause(String userAlias, String idColumn) {
        if (idColumn == null || !tableExists("user_profiles")) {
            return "";
        }

        String profileName = userProfileNameCol();
        if (profileName == null) {
            return "";
        }

        return " LEFT JOIN user_profiles up ON up.user_id = " + userAlias + "." + idColumn + " ";
    }

    private LoginSchema resolveLoginSchema() {
        LoginSchema cached = LOGIN_SCHEMA_CACHE;
        if (cached != null) {
            return cached;
        }

        synchronized (LOGIN_SCHEMA_LOCK) {
            cached = LOGIN_SCHEMA_CACHE;
            if (cached != null) {
                return cached;
            }

            String sql =
                    "SELECT table_name, column_name FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name IN ('users', 'user_profiles')";

            try (Connection con = DBConfig.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                Set<String> userColumns = new HashSet<>();
                Set<String> profileColumns = new HashSet<>();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    String columnName = rs.getString("column_name");
                    if (tableName == null || columnName == null) {
                        continue;
                    }

                    String normalized = columnName.trim().toLowerCase();
                    if ("users".equalsIgnoreCase(tableName)) {
                        userColumns.add(normalized);
                    } else if ("user_profiles".equalsIgnoreCase(tableName)) {
                        profileColumns.add(normalized);
                    }
                }

                LoginSchema schema = new LoginSchema();
                schema.idCol = chooseColumn(userColumns,
                        new String[]{"id", "user_id", "uid", "userid", "id_user"},
                        "user%id", "%user%id%", "%id");
                schema.nameCol = chooseColumn(userColumns,
                        new String[]{"name", "full_name", "username", "user_name", "first_name", "display_name"},
                        "%display%name%", "%full%name%", "%user%name%", "%name%");
                schema.emailCol = chooseColumn(userColumns,
                        new String[]{"email", "email_id", "mail", "emailid", "user_email", "useremail", "email_address"},
                        "%email%", "%mail%");
                schema.roleCol = chooseColumn(userColumns,
                        new String[]{"role", "user_role", "type", "user_type", "role_id"},
                        "%role%", "%type%");
                schema.statusCol = chooseColumn(userColumns,
                        new String[]{"status", "user_status", "is_active", "active", "enabled"},
                        "%status%", "%active%", "%enabled%");
                schema.passwordCol = chooseColumn(userColumns,
                        new String[]{"password", "pass", "pwd", "passwd", "user_password", "pass_word", "userpass", "user_pass", "userpassword", "password_hash"},
                        "%password%", "%pass%", "%pwd%", "%secret%");
                schema.profileNameCol = chooseColumn(profileColumns,
                        new String[]{"name", "full_name", "username", "user_name"},
                        "%name%");
                schema.hasUserProfiles = !profileColumns.isEmpty() && profileColumns.contains("user_id");

                LOGIN_SCHEMA_CACHE = schema;
                primeSchemaCaches(schema);
                return schema;
            } catch (Exception e) {
                markSchemaLookupFailure(e);
                return null;
            }
        }
    }

    private void primeSchemaCaches(LoginSchema schema) {
        if (schema == null) {
            return;
        }

        cacheResolvedColumn("users.id", schema.idCol);
        cacheResolvedColumn("users.name", schema.nameCol);
        cacheResolvedColumn("users.email", schema.emailCol);
        cacheResolvedColumn("users.role", schema.roleCol);
        cacheResolvedColumn("users.status", schema.statusCol);
        cacheResolvedColumn("users.password", schema.passwordCol);
        cacheResolvedColumn("user_profiles.name", schema.profileNameCol);

        if (schema.hasUserProfiles) {
            TABLE_CACHE.put("user_profiles", true);
        }
    }

    private void cacheResolvedColumn(String key, String value) {
        if (value != null && !value.isBlank()) {
            COL_CACHE.put(key, value);
        }
    }

    private String chooseColumn(Set<String> columns, String[] exactCandidates, String... likePatterns) {
        for (String candidate : exactCandidates) {
            String normalized = candidate == null ? null : candidate.trim().toLowerCase();
            if (normalized != null && columns.contains(normalized)) {
                return normalized;
            }
        }

        for (String pattern : likePatterns) {
            String resolved = firstMatchingPattern(columns, pattern);
            if (resolved != null) {
                return resolved;
            }
        }

        return null;
    }

    private String firstMatchingPattern(Set<String> columns, String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return null;
        }

        StringBuilder regex = new StringBuilder();
        for (char ch : pattern.trim().toLowerCase().toCharArray()) {
            if (ch == '%') {
                regex.append(".*");
            } else if (ch == '_') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(ch)));
            }
        }

        String compiled = regex.toString();
        for (String column : columns) {
            if (column != null && column.matches(compiled)) {
                return column;
            }
        }
        return null;
    }

    public String[] loginUser(String email, String password) {
        setLastError(null);
        resetSchemaLookupError();

        LoginSchema schema = resolveLoginSchema();
        if (schema == null) {
            setLastError(getSchemaLookupError());
            return null;
        }

        if (schema.idCol == null || schema.emailCol == null || schema.passwordCol == null) {
            if (getSchemaLookupError() != null) {
                setLastError(getSchemaLookupError());
                return null;
            }
            setLastError("Users table is missing required login columns (id/email/password).");
            return null;
        }

        String displayNameExpr = schema.nameCol != null ? "u." + schema.nameCol : "u." + schema.emailCol;

        if (schema.hasUserProfiles && schema.profileNameCol != null) {
            displayNameExpr =
                    "COALESCE(NULLIF(TRIM(up." + schema.profileNameCol + "),''), " + displayNameExpr + ")";
        }

        String profileJoin = schema.hasUserProfiles && schema.profileNameCol != null
                ? "LEFT JOIN user_profiles up ON up.user_id = u." + schema.idCol + " "
                : "";

        String sqlExact =
                "SELECT u." + schema.idCol + " AS id, " + displayNameExpr + " AS name, u." + schema.emailCol + " AS email, " +
                (schema.roleCol != null ? "u." + schema.roleCol : "'USER'") + " AS role, " +
                (schema.statusCol != null ? "u." + schema.statusCol : "'ACTIVE'") + " AS status " +
                "FROM users u " +
                profileJoin +
                "WHERE u." + schema.emailCol + "=? " +
                "AND u." + schema.passwordCol + "=?";

        String sqlFallback =
                "SELECT u." + schema.idCol + " AS id, " + displayNameExpr + " AS name, u." + schema.emailCol + " AS email, " +
                (schema.roleCol != null ? "u." + schema.roleCol : "'USER'") + " AS role, " +
                (schema.statusCol != null ? "u." + schema.statusCol : "'ACTIVE'") + " AS status " +
                "FROM users u " +
                profileJoin +
                "WHERE LOWER(TRIM(u." + schema.emailCol + "))=LOWER(TRIM(?)) " +
                "AND TRIM(u." + schema.passwordCol + ")=TRIM(?)";

        try (Connection con = DBConfig.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sqlExact)) {
                ps.setString(1, email);
                ps.setString(2, password);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String statusValue = readCol(rs, "status", "ACTIVE");
                        if (!isActiveStatus(statusValue)) {
                            setLastError("User account is not active.");
                            return null;
                        }
                        return new String[]{
                                readCol(rs, "id", "0"),
                                readCol(rs, "name", "User"),
                                readCol(rs, "email", email),
                                normalizeRole(readCol(rs, "role", Role.USER)),
                                statusValue
                        };
                    }
                }
            }

            
            try (PreparedStatement ps = con.prepareStatement(sqlFallback)) {
                ps.setString(1, email);
                ps.setString(2, password);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String statusValue = readCol(rs, "status", "ACTIVE");
                        if (!isActiveStatus(statusValue)) {
                            setLastError("User account is not active.");
                            return null;
                        }
                        return new String[]{
                                readCol(rs, "id", "0"),
                                readCol(rs, "name", "User"),
                                readCol(rs, "email", email),
                                normalizeRole(readCol(rs, "role", Role.USER)),
                                statusValue
                        };
                    }
                }
            }
        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            if (!DBConfig.isConnectionUnavailable(e)) {
                e.printStackTrace();
            }
        }

        if (getLastError().startsWith("Unknown")) {
            setLastError("Invalid email or password.");
        }
        return null;
    }

    private static class LoginSchema {
        String idCol;
        String nameCol;
        String emailCol;
        String roleCol;
        String statusCol;
        String passwordCol;
        String profileNameCol;
        boolean hasUserProfiles;
    }

    public boolean emailExists(String email) {
        setLastError(null);
        resetSchemaLookupError();

        String mail = emailCol();
        if (mail == null) return false;

        String sql = "SELECT 1 FROM users WHERE " + mail + "=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    public boolean registerUser(String name, String email, String password) {
        setLastError(null);
        resetSchemaLookupError();

        if (emailExists(email)) {
            setLastError("Email already exists.");
            return false;
        }

        String nameCol = nameCol();
        String emailCol = emailCol();
        String passCol = passwordCol();
        String roleCol = roleCol();
        String statusCol = statusCol();
        String roleType = columnDataType("users", roleCol);
        String statusType = columnDataType("users", statusCol);

        if (emailCol == null || passCol == null) {
            setLastError("Users table is missing required columns (email/password).");
            return false;
        }

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        if (nameCol != null) {
            columns.add(nameCol);
            values.add("?");
        }

        columns.add(emailCol);
        values.add("?");

        columns.add(passCol);
        values.add("?");

        if (roleCol != null) {
            columns.add(roleCol);
            values.add("?");
        }

        if (statusCol != null) {
            columns.add(statusCol);
            values.add("?");
        }

        String sql = "INSERT INTO users (" + String.join(",", columns) + ") VALUES (" + String.join(",", values) + ")";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int i = 1;
            if (nameCol != null) {
                ps.setString(i++, name);
            }
            ps.setString(i++, email);
            ps.setString(i++, password);

            if (roleCol != null) {
                if (isNumericType(roleType)) {
                    ps.setInt(i++, numericRoleValue(Role.USER));
                } else {
                    ps.setString(i++, Role.USER);
                }
            }
            if (statusCol != null) {
                if (isNumericType(statusType)) {
                    ps.setInt(i, 1);
                } else {
                    ps.setString(i, activeStatusValue(statusCol));
                }
            }

            int rows = ps.executeUpdate();
            if (rows <= 0) {
                setLastError("No row inserted into users.");
                return false;
            }

            int userId = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    userId = keys.getInt(1);
                }
            }
            if (userId == -1 && idCol() != null) {
                try (PreparedStatement q = con.prepareStatement(
                        "SELECT " + idCol() + " FROM users WHERE " + emailCol + "=? ORDER BY " + idCol() + " DESC LIMIT 1")) {
                    q.setString(1, email);
                    try (ResultSet rs = q.executeQuery()) {
                        if (rs.next()) userId = rs.getInt(1);
                    }
                }
            }

            if (nameCol == null && userId > 0) {
                insertUserProfile(con, userId, name);
            }

            return true;

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        if (getLastError().startsWith("Unknown")) {
            setLastError("Registration failed due to schema/data mismatch.");
        }
        return false;
    }

    public boolean addUser(String name, String email, String password, String role) {
        setLastError(null);
        resetSchemaLookupError();

        if (emailExists(email)) return false;

        String nameCol = nameCol();
        String emailCol = emailCol();
        String passCol = passwordCol();
        String roleCol = roleCol();
        String statusCol = statusCol();
        String roleType = columnDataType("users", roleCol);
        String statusType = columnDataType("users", statusCol);

        if (emailCol == null || passCol == null) {
            return false;
        }

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        if (nameCol != null) {
            columns.add(nameCol);
            values.add("?");
        }

        columns.add(emailCol);
        values.add("?");

        columns.add(passCol);
        values.add("?");

        if (roleCol != null) {
            columns.add(roleCol);
            values.add("?");
        }

        if (statusCol != null) {
            columns.add(statusCol);
            values.add("?");
        }

        String sql = "INSERT INTO users (" + String.join(",", columns) + ") VALUES (" + String.join(",", values) + ")";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int i = 1;
            if (nameCol != null) {
                ps.setString(i++, name);
            }
            ps.setString(i++, email);
            ps.setString(i++, password);

            if (roleCol != null) {
                if (isNumericType(roleType)) {
                    ps.setInt(i++, numericRoleValue(role));
                } else {
                    ps.setString(i++, normalizeRole(role));
                }
            }
            if (statusCol != null) {
                if (isNumericType(statusType)) {
                    ps.setInt(i, 1);
                } else {
                    ps.setString(i, activeStatusValue(statusCol));
                }
            }

            int rows = ps.executeUpdate();
            if (rows <= 0) return false;

            int userId = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    userId = keys.getInt(1);
                }
            }
            if (nameCol == null && userId == -1 && idCol() != null) {
                try (PreparedStatement q = con.prepareStatement(
                        "SELECT " + idCol() + " FROM users WHERE " + emailCol + "=? ORDER BY " + idCol() + " DESC LIMIT 1")) {
                    q.setString(1, email);
                    try (ResultSet rs = q.executeQuery()) {
                        if (rs.next()) userId = rs.getInt(1);
                    }
                }
            }
            if (nameCol == null && userId > 0) {
                insertUserProfile(con, userId, name);
            }

            return true;

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    private void insertUserProfile(Connection con, int userId, String name) {
        try {
            if (!tableExists("user_profiles")) return;
            String ncol = userProfileNameCol();
            if (ncol == null) return;

            String sql = "INSERT INTO user_profiles (user_id," + ncol + ") VALUES (?,?) " +
                    "ON DUPLICATE KEY UPDATE " + ncol + "=VALUES(" + ncol + ")";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, name);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {
            
        }
    }

    public boolean changePassword(int userId, String oldPass, String newPass) {
        setLastError(null);
        resetSchemaLookupError();

        String id = idCol();
        String pass = passwordCol();

        if (id == null || pass == null) return false;

        String check = "SELECT 1 FROM users WHERE " + id + "=? AND " + pass + "=?";
        String update = "UPDATE users SET " + pass + "=? WHERE " + id + "=?";

        try (Connection con = DBConfig.getConnection()) {

            try (PreparedStatement ps1 = con.prepareStatement(check)) {
                ps1.setInt(1, userId);
                ps1.setString(2, oldPass);

                try (ResultSet rs = ps1.executeQuery()) {
                    if (!rs.next()) return false;
                }
            }

            try (PreparedStatement ps2 = con.prepareStatement(update)) {
                ps2.setString(1, newPass);
                ps2.setInt(2, userId);
                return ps2.executeUpdate() > 0;
            }

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    public boolean updatePasswordByEmail(String email, String newPass) {
        setLastError(null);
        resetSchemaLookupError();

        String mail = emailCol();
        String pass = passwordCol();

        if (mail == null || pass == null) return false;

        String sql = "UPDATE users SET " + pass + "=? WHERE " + mail + "=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newPass);
            ps.setString(2, email);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    public String[] getUserById(int userId) {
        setLastError(null);
        resetSchemaLookupError();

        String id = idCol();
        String name = nameCol();
        String mail = emailCol();
        String role = roleCol();
        String status = statusCol();
        String created = createdAtCol();

        if (refreshSchemaIfNeeded(id, mail, null)) {
            id = idCol();
            name = nameCol();
            mail = emailCol();
            role = roleCol();
            status = statusCol();
            created = createdAtCol();
        }

        if (id == null || mail == null) {
            if (getSchemaLookupError() != null) {
                setLastError(getSchemaLookupError());
            }
            return null;
        }

        String displayNameExpr = userDisplayNameExpr("u", id, mail, name);
        String join = userProfileJoinClause("u", id);

        String sql =
                "SELECT u." + id + " AS id," +
                displayNameExpr + " AS name," +
                "u." + mail + " AS email," +
                (role != null ? "u." + role : "'USER'") + " AS role," +
                (status != null ? "u." + status : "'ACTIVE'") + " AS status," +
                (created != null ? "u." + created : "NULL") + " AS created_at " +
                "FROM users u" + join +
                "WHERE u." + id + "=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return buildUser(rs);
                }
            }

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return null;
    }

    public List<String[]> getAllUsers() {
        setLastError(null);
        resetSchemaLookupError();

        List<String[]> list = new ArrayList<>();

        String id = idCol();
        String name = nameCol();
        String mail = emailCol();
        String role = roleCol();
        String status = statusCol();
        String created = createdAtCol();

        if (refreshSchemaIfNeeded(id, mail, null)) {
            id = idCol();
            name = nameCol();
            mail = emailCol();
            role = roleCol();
            status = statusCol();
            created = createdAtCol();
        }

        if (id == null || mail == null) {
            if (getSchemaLookupError() != null) {
                setLastError(getSchemaLookupError());
            }
            return list;
        }

        String displayNameExpr = userDisplayNameExpr("u", id, mail, name);
        String join = userProfileJoinClause("u", id);

        String sql =
                "SELECT u." + id + " AS id," +
                displayNameExpr + " AS name," +
                "u." + mail + " AS email," +
                (role != null ? "u." + role : "'USER'") + " AS role," +
                (status != null ? "u." + status : "'ACTIVE'") + " AS status," +
                (created != null ? "u." + created : "NULL") + " AS created_at " +
                "FROM users u" + join +
                "ORDER BY u." + id + " DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(buildUser(rs));
            }

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return list;
    }

    public boolean updateUserStatus(int userId, String statusValue) {
        setLastError(null);
        resetSchemaLookupError();

        String id = idCol();
        String status = statusCol();
        String statusType = columnDataType("users", status);

        if (id == null || status == null) return false;

        String sql = "UPDATE users SET " + status + "=? WHERE " + id + "=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            boolean active = "ACTIVE".equalsIgnoreCase(statusValue);
            if (isNumericType(statusType)) {
                ps.setInt(1, active ? 1 : 0);
            } else {
                String dbValue = active ? activeStatusValue(status) : inactiveStatusValue(status);
                ps.setString(1, dbValue);
            }
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    public boolean updateUserRole(int userId, String roleValue) {
        setLastError(null);
        resetSchemaLookupError();

        String id = idCol();
        String role = roleCol();
        String roleType = columnDataType("users", role);

        if (id == null || role == null) return false;

        String sql = "UPDATE users SET " + role + "=? WHERE " + id + "=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (isNumericType(roleType)) {
                ps.setInt(1, numericRoleValue(roleValue));
            } else {
                ps.setString(1, normalizeRole(roleValue));
            }
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    public boolean deleteUser(int idValue) {
        setLastError(null);
        resetSchemaLookupError();

        String id = idCol();
        if (id == null) return false;

        String sql = "DELETE FROM users WHERE " + id + "=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idValue);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }
}
