package dao;

import util.DBConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    

    public boolean logAction(int userId, String action, String description) {

        String sql =
                "INSERT INTO audit_logs (user_id, action, description, action_time) " +
                "VALUES (?,?,?,?)";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, description);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    

    private String[] buildRow(ResultSet rs, boolean includeUser) throws SQLException {

        if (includeUser) {
            return new String[]{
                    rs.getString("id"),
                    rs.getString("name") != null ? rs.getString("name") : "System",
                    rs.getString("action"),
                    rs.getString("description"),
                    rs.getString("action_time")
            };
        } else {
            return new String[]{
                    rs.getString("id"),
                    rs.getString("action"),
                    rs.getString("description"),
                    rs.getString("action_time")
            };
        }
    }

    

    public List<String[]> getAllLogs() {

        List<String[]> logs = new ArrayList<>();

        String sql =
                "SELECT a.id, u.name, a.action, a.description, a.action_time " +
                "FROM audit_logs a " +
                "LEFT JOIN users u ON a.user_id = u.id " +
                "ORDER BY a.action_time DESC";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                logs.add(buildRow(rs, true));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return logs;
    }

    

    public List<String[]> getLogsByUser(int userId) {

        List<String[]> logs = new ArrayList<>();

        String sql =
                "SELECT id, action, description, action_time " +
                "FROM audit_logs " +
                "WHERE user_id=? " +
                "ORDER BY action_time DESC";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    logs.add(buildRow(rs, false));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return logs;
    }

    

    public List<String[]> getRecentLogs(int limit) {

        List<String[]> logs = new ArrayList<>();

        String sql =
                "SELECT a.id, u.name, a.action, a.description, a.action_time " +
                "FROM audit_logs a " +
                "LEFT JOIN users u ON a.user_id = u.id " +
                "ORDER BY a.action_time DESC " +
                "LIMIT ?";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    logs.add(buildRow(rs, true));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return logs;
    }
}