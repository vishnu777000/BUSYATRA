package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ComplaintDAO {

    /* ================= HELPERS ================= */

    private String[] buildUserRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("category"),
                rs.getString("message"),
                rs.getString("status"),
                rs.getString("admin_reply") != null ? rs.getString("admin_reply") : "-",
                rs.getString("created_at")
        };
    }

    private String[] buildAdminRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("email"),
                rs.getString("category"),
                rs.getString("message"),
                rs.getString("status"),
                rs.getString("admin_reply") != null ? rs.getString("admin_reply") : "-",
                rs.getString("created_at")
        };
    }

    private int getInt(String sql) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /* ================= ADD ================= */

    public boolean addComplaint(int userId, String category, String message) {

        String sql =
                "INSERT INTO complaints (user_id, category, message, status, created_at) " +
                "VALUES (?,?,?, 'OPEN', NOW())";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, category);
            ps.setString(3, message);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= USER ================= */

    public List<String[]> getComplaintsByUser(int userId) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT id,category,message,status,admin_reply,created_at " +
                "FROM complaints WHERE user_id=? ORDER BY created_at DESC";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(buildUserRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= ADMIN ================= */

    public List<String[]> getAllComplaints() {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT c.id,u.email,c.category,c.message,c.status,c.admin_reply,c.created_at " +
                "FROM complaints c " +
                "JOIN users u ON c.user_id = u.id " +
                "ORDER BY c.created_at DESC";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(buildAdminRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= UPDATE ================= */

    public boolean updateComplaint(int id, String status, String reply) {

        String sql =
                "UPDATE complaints SET status=?, admin_reply=? WHERE id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, reply);
            ps.setInt(3, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= STATS ================= */

    public int getTotalComplaints() {
        return getInt("SELECT COUNT(*) FROM complaints");
    }

    public int getOpenComplaints() {
        return getInt("SELECT COUNT(*) FROM complaints WHERE status='OPEN'");
    }
}