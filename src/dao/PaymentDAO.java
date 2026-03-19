package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    /* ================= HELPERS ================= */

    private double getDouble(String sql, int param) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, param);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private boolean exists(String sql, int param) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, param);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private String[] buildRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("ticket_id"),
                rs.getString("payment_method"),
                rs.getString("amount"),
                rs.getString("status"),
                rs.getString("created_at")
        };
    }

    /* ================= GET TICKET AMOUNT ================= */

    public double getTicketAmount(int ticketId) {

        String sql = "SELECT amount FROM tickets WHERE id=?";

        return getDouble(sql, ticketId);
    }

    /* ================= RECORD PAYMENT ================= */

    public boolean recordPayment(int ticketId, String method, double amount) {

        String sql =
                "INSERT INTO payments (ticket_id, payment_method, amount, status, created_at) " +
                "VALUES (?, ?, ?, 'SUCCESS', NOW())";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, ticketId);
            ps.setString(2, method);
            ps.setDouble(3, amount);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= CHECK PAYMENT ================= */

    public boolean isTicketPaid(int ticketId) {

        String sql =
                "SELECT COUNT(*) FROM payments " +
                "WHERE ticket_id=? AND status='SUCCESS'";

        return exists(sql, ticketId);
    }

    /* ================= PAYMENT HISTORY ================= */

    public List<String[]> getPaymentHistory(int userId) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT p.id,t.id AS ticket_id,p.payment_method,p.amount,p.status,p.created_at " +
                "FROM payments p " +
                "JOIN tickets t ON p.ticket_id=t.id " +
                "WHERE t.user_id=? " +
                "ORDER BY p.created_at DESC";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(buildRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= TOTAL PAYMENTS ================= */

    public double getTotalPayments() {

        String sql =
                "SELECT IFNULL(SUM(amount),0) FROM payments WHERE status='SUCCESS'";

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
}