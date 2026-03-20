package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ComplaintDAO {

    private static final String TABLE_COMPLAINTS = "complaints";
    private static final String TABLE_SUPPORT = "support_messages";
    private static final Map<String, Boolean> TABLE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> COLUMN_CACHE = new ConcurrentHashMap<>();

    private boolean tableExists(String tableName) {
        if (TABLE_CACHE.containsKey(tableName)) {
            return TABLE_CACHE.get(tableName);
        }

        String sql =
                "SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ? LIMIT 1";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                TABLE_CACHE.put(tableName, exists);
                return exists;
            }
        } catch (Exception e) {
            TABLE_CACHE.put(tableName, false);
            return false;
        }
    }

    private String userEmailColumn() {
        if (COLUMN_CACHE.containsKey("users.email")) {
            return COLUMN_CACHE.get("users.email");
        }

        String sql =
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name IN " +
                "('email','email_id','mail','username','name') " +
                "ORDER BY FIELD(column_name,'email','email_id','mail','username','name') LIMIT 1";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            String col = rs.next() ? rs.getString("column_name") : "id";
            COLUMN_CACHE.put("users.email", col);
            return col;
        } catch (Exception e) {
            COLUMN_CACHE.put("users.email", "id");
            return "id";
        }
    }

    private String activeTable() {
        if (tableExists(TABLE_COMPLAINTS)) return TABLE_COMPLAINTS;
        if (tableExists(TABLE_SUPPORT)) return TABLE_SUPPORT;
        return null;
    }

    private int getInt(String sql) {
        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {
            // graceful fallback
        }
        return 0;
    }

    public boolean addComplaint(int userId, String category, String message) {
        String table = activeTable();
        if (table == null) return false;

        String sql;
        if (TABLE_COMPLAINTS.equals(table)) {
            sql = "INSERT INTO complaints (user_id, category, message, status, created_at) VALUES (?,?,?,'OPEN',NOW())";
        } else {
            sql = "INSERT INTO support_messages (user_id, message, status, created_at) VALUES (?,?,'OPEN',NOW())";
        }

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (TABLE_COMPLAINTS.equals(table)) {
                ps.setInt(1, userId);
                ps.setString(2, category);
                ps.setString(3, message);
            } else {
                ps.setInt(1, userId);
                ps.setString(2, (category == null || category.isBlank() ? "" : "[" + category + "] ") + message);
            }
            return ps.executeUpdate() > 0;

        } catch (Exception ignored) {
            return false;
        }
    }

    public List<String[]> getComplaintsByUser(int userId) {
        List<String[]> list = new ArrayList<>();
        String table = activeTable();
        if (table == null) return list;

        String sql;
        if (TABLE_COMPLAINTS.equals(table)) {
            sql = "SELECT id,category,message,status,admin_reply,created_at FROM complaints WHERE user_id=? ORDER BY created_at DESC";
        } else {
            sql =
                    "SELECT id, " +
                    "'General' AS category, " +
                    "message, " +
                    "CASE WHEN status='CLOSED' THEN 'RESOLVED' ELSE 'OPEN' END AS status, " +
                    "reply AS admin_reply, " +
                    "created_at " +
                    "FROM support_messages WHERE user_id=? ORDER BY created_at DESC";
        }

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("id"),
                            rs.getString("category"),
                            rs.getString("message"),
                            rs.getString("status"),
                            rs.getString("admin_reply") != null ? rs.getString("admin_reply") : "-",
                            rs.getString("created_at")
                    });
                }
            }
        } catch (Exception ignored) {
            // graceful fallback
        }

        return list;
    }

    public List<String[]> getAllComplaints() {
        List<String[]> list = new ArrayList<>();
        String table = activeTable();
        if (table == null) return list;

        String emailCol = userEmailColumn();
        String sql;

        if (TABLE_COMPLAINTS.equals(table)) {
            sql =
                    "SELECT c.id,u." + emailCol + " AS email,c.category,c.message,c.status,c.admin_reply,c.created_at " +
                    "FROM complaints c JOIN users u ON c.user_id=u.id ORDER BY c.created_at DESC";
        } else {
            sql =
                    "SELECT c.id,u." + emailCol + " AS email,'General' AS category,c.message, " +
                    "CASE WHEN c.status='CLOSED' THEN 'RESOLVED' ELSE 'OPEN' END AS status, " +
                    "c.reply AS admin_reply,c.created_at " +
                    "FROM support_messages c JOIN users u ON c.user_id=u.id ORDER BY c.created_at DESC";
        }

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("id"),
                        rs.getString("email"),
                        rs.getString("category"),
                        rs.getString("message"),
                        rs.getString("status"),
                        rs.getString("admin_reply") != null ? rs.getString("admin_reply") : "-",
                        rs.getString("created_at")
                });
            }

        } catch (Exception ignored) {
            // graceful fallback
        }

        return list;
    }

    public boolean updateComplaint(int id, String status, String reply) {
        String table = activeTable();
        if (table == null) return false;

        String sql;
        String mappedStatus = status;

        if (TABLE_COMPLAINTS.equals(table)) {
            sql = "UPDATE complaints SET status=?, admin_reply=? WHERE id=?";
        } else {
            sql = "UPDATE support_messages SET status=?, reply=? WHERE id=?";
            // support_messages supports OPEN/CLOSED only
            if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
                mappedStatus = "CLOSED";
            } else {
                mappedStatus = "OPEN";
            }
        }

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, mappedStatus);
            ps.setString(2, reply);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public int getTotalComplaints() {
        String table = activeTable();
        if (table == null) return 0;
        return getInt("SELECT COUNT(*) FROM " + table);
    }

    public int getOpenComplaints() {
        String table = activeTable();
        if (table == null) return 0;
        if (TABLE_COMPLAINTS.equals(table)) {
            return getInt("SELECT COUNT(*) FROM complaints WHERE status='OPEN'");
        }
        return getInt("SELECT COUNT(*) FROM support_messages WHERE status='OPEN'");
    }
}
