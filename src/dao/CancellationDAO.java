package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CancellationDAO {

    /* ================= GENERIC HELPERS ================= */

    private String[] buildUserRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("ticket_id"),
                rs.getString("reason"),
                rs.getString("refund_amount"),
                rs.getString("status"),
                rs.getString("created_at")
        };
    }

    private String[] buildAdminRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("name") != null ? rs.getString("name") : "System",
                rs.getString("ticket_id"),
                rs.getString("reason"),
                rs.getString("refund_amount"),
                rs.getString("status"),
                rs.getString("created_at")
        };
    }

    private double getDouble(String sql) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getDouble(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private int getInt(String sql) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /* ================= ADD CANCELLATION ================= */

    public boolean addCancellation(
            int ticketId,
            int userId,
            String reason,
            double refundAmount
    ) {

        String sql =
                "INSERT INTO cancellations " +
                "(ticket_id, user_id, reason, refund_amount, status, created_at) " +
                "VALUES (?,?,?,?, 'REFUNDED', NOW())";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, ticketId);
            ps.setInt(2, userId);
            ps.setString(3, reason);
            ps.setDouble(4, refundAmount);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= USER CANCELLATIONS ================= */

    public List<String[]> getUserCancellations(int userId) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT ticket_id,reason,refund_amount,status,created_at " +
                "FROM cancellations " +
                "WHERE user_id=? " +
                "ORDER BY created_at DESC";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

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

    /* ================= ALL CANCELLATIONS ================= */

    public List<String[]> getAllCancellations() {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT c.id,u.name,c.ticket_id,c.reason,c.refund_amount,c.status,c.created_at " +
                "FROM cancellations c " +
                "LEFT JOIN users u ON c.user_id=u.id " +
                "ORDER BY c.created_at DESC";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                list.add(buildAdminRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= TOTAL REFUNDS ================= */

    public double getTotalRefundAmount() {
        return getDouble("SELECT IFNULL(SUM(refund_amount),0) FROM cancellations");
    }

    /* ================= COUNT ================= */

    public int getCancellationCount() {
        return getInt("SELECT COUNT(*) FROM cancellations");
    }
}