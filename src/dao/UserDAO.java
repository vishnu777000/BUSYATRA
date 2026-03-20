package dao;

import config.DBConfig;
import model.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class UserDAO {

    private final ThreadLocal<String> lastError = new ThreadLocal<>();
    private static final String NULL_SENTINEL = "__NULL__";
    private static final Map<String, String> COL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> TABLE_CACHE = new ConcurrentHashMap<>();

    public String getLastError() {
        String err = lastError.get();
        return (err == null || err.isBlank()) ? "Unknown registration/login error" : err;
    }

    private void setLastError(String message) {
        lastError.set(message);
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
            } catch (Exception ignored) {
                // try next
            }
        }

        return null;
    }

    private String cachedColumn(String key, Supplier<String> resolver) {
        if (COL_CACHE.containsKey(key)) {
            String cached = COL_CACHE.get(key);
            return NULL_SENTINEL.equals(cached) ? null : cached;
        }
        String resolved = resolver.get();
        COL_CACHE.put(key, resolved == null ? NULL_SENTINEL : resolved);
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
            } catch (Exception ignored) {
                // try next pattern
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
        } catch (Exception ignored) {
            // fallback
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
        } catch (Exception ignored) {
            TABLE_CACHE.put(table, false);
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
            String col = findFirstExistingColumn("users", "name", "full_name", "username", "user_name", "first_name");
            return col != null ? col : findColumnByLike("users", "%name%");
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
            String col = findFirstExistingColumn("users", "email", "email_id", "mail", "emailid");
            return col != null ? col : findColumnByLike("users", "%mail%");
        });
    }

    private String idCol() {
        return cachedColumn("users.id", () -> {
            String col = findFirstExistingColumn("users", "id", "user_id", "uid");
            return col != null ? col : findColumnByLike("users", "%id");
        });
    }

    private String passwordCol() {
        return cachedColumn("users.password", () -> {
            String col = findFirstExistingColumn("users", "password", "pass", "pwd", "passwd", "user_password", "pass_word");
            return col != null ? col : findColumnByLike("users", "%pass%", "%pwd%");
        });
    }

    private String activeStatusValue(String statusColumn) {
        String col = statusColumn == null ? "" : statusColumn.toLowerCase();
        if (col.contains("active") || col.contains("enabled") || col.contains("flag")) {
            return "1";
        }
        return "ACTIVE";
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
                readCol(rs, "status", "ACTIVE"),
                readCol(rs, "created_at", "")
        };
    }

    public String[] loginUser(String email, String password) {
        setLastError(null);

        String id = idCol();
        String name = nameCol();
        String mail = emailCol();
        String role = roleCol();
        String status = statusCol();
        String pass = passwordCol();

        if (id == null || mail == null || pass == null) {
            setLastError("Required user columns not found (id/email/password).");
            return null;
        }

        String displayNameExpr = name != null ? "u." + name : "u." + mail;

        if (tableExists("user_profiles")) {
            String profileName = userProfileNameCol();
            if (profileName != null) {
                displayNameExpr =
                        "COALESCE(NULLIF(TRIM(up." + profileName + "),''), " + displayNameExpr + ")";
            }
        }

        String sqlExact =
                "SELECT u." + id + " AS id, " + displayNameExpr + " AS name, u." + mail + " AS email, " +
                (role != null ? "u." + role : "'USER'") + " AS role, " +
                (status != null ? "u." + status : "'ACTIVE'") + " AS status " +
                "FROM users u " +
                (tableExists("user_profiles") ? "LEFT JOIN user_profiles up ON up.user_id = u." + id + " " : "") +
                "WHERE u." + mail + "=? " +
                "AND u." + pass + "=?";

        String sqlFallback =
                "SELECT u." + id + " AS id, " + displayNameExpr + " AS name, u." + mail + " AS email, " +
                (role != null ? "u." + role : "'USER'") + " AS role, " +
                (status != null ? "u." + status : "'ACTIVE'") + " AS status " +
                "FROM users u " +
                (tableExists("user_profiles") ? "LEFT JOIN user_profiles up ON up.user_id = u." + id + " " : "") +
                "WHERE LOWER(TRIM(u." + mail + "))=LOWER(TRIM(?)) " +
                "AND TRIM(u." + pass + ")=TRIM(?)";

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

            // Compatibility fallback for trimmed/variant stored values
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
            setLastError(e.getMessage());
            e.printStackTrace();
        }

        if (getLastError().startsWith("Unknown")) {
            setLastError("Invalid email or password.");
        }
        return null;
    }

    public boolean emailExists(String email) {
        setLastError(null);

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
            setLastError(e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean registerUser(String name, String email, String password) {
        setLastError(null);

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
            setLastError(e.getMessage());
            e.printStackTrace();
        }

        if (getLastError().startsWith("Unknown")) {
            setLastError("Registration failed due to schema/data mismatch.");
        }
        return false;
    }

    public boolean addUser(String name, String email, String password, String role) {

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
            if (nameCol == null && userId > 0) {
                insertUserProfile(con, userId, name);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
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
            // profile is optional for login/register success
        }
    }

    public boolean changePassword(int userId, String oldPass, String newPass) {

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
            e.printStackTrace();
        }

        return false;
    }

    public boolean updatePasswordByEmail(String email, String newPass) {

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
            e.printStackTrace();
        }

        return false;
    }

    public String[] getUserById(int userId) {

        String id = idCol();
        String name = nameCol();
        String mail = emailCol();
        String role = roleCol();
        String status = statusCol();
        String created = createdAtCol();

        if (id == null || name == null || mail == null) {
            return null;
        }

        String sql =
                "SELECT " + id + " AS id," +
                name + " AS name," +
                mail + " AS email," +
                (role != null ? role : "'USER'") + " AS role," +
                (status != null ? status : "'ACTIVE'") + " AS status," +
                (created != null ? created : "NULL") + " AS created_at " +
                "FROM users WHERE " + id + "=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return buildUser(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<String[]> getAllUsers() {

        List<String[]> list = new ArrayList<>();

        String id = idCol();
        String name = nameCol();
        String mail = emailCol();
        String role = roleCol();
        String status = statusCol();
        String created = createdAtCol();

        if (id == null || name == null || mail == null) {
            return list;
        }

        String sql =
                "SELECT " + id + " AS id," +
                name + " AS name," +
                mail + " AS email," +
                (role != null ? role : "'USER'") + " AS role," +
                (status != null ? status : "'ACTIVE'") + " AS status," +
                (created != null ? created : "NULL") + " AS created_at " +
                "FROM users ORDER BY " + id + " DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(buildUser(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public boolean updateUserStatus(int userId, String statusValue) {

        String id = idCol();
        String status = statusCol();

        if (id == null || status == null) return false;

        String sql = "UPDATE users SET " + status + "=? WHERE " + id + "=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String dbValue = statusValue;
            if ("is_active".equalsIgnoreCase(status)) {
                dbValue = "ACTIVE".equalsIgnoreCase(statusValue) ? "1" : "0";
            }

            ps.setString(1, dbValue);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateUserRole(int userId, String roleValue) {

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
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteUser(int idValue) {

        String id = idCol();
        if (id == null) return false;

        String sql = "DELETE FROM users WHERE " + id + "=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idValue);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
