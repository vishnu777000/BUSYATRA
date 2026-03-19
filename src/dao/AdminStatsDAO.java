package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminStatsDAO {

    /* ================= GENERIC HELPERS ================= */

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

    /* ================= USERS ================= */

    public int getTotalUsers() {
        return getInt("SELECT COUNT(*) FROM users");
    }

    /* ================= BUSES ================= */

    public int getTotalBuses() {
        return getInt("SELECT COUNT(*) FROM buses");
    }

    /* ================= ROUTES ================= */

    public int getTotalRoutes() {
        return getInt("SELECT COUNT(*) FROM routes");
    }

    /* ================= SCHEDULES ================= */

    public int getTotalSchedules() {
        return getInt("SELECT COUNT(*) FROM schedules");
    }

    /* ================= TICKETS ================= */

    public int getTotalTickets() {
        return getInt("SELECT COUNT(*) FROM tickets");
    }

    public int getCancelledTickets() {
        return getInt("SELECT COUNT(*) FROM tickets WHERE status='CANCELLED'");
    }

    /* ================= TODAY BOOKINGS ================= */

    public int getTodayBookings() {

        String sql =
                "SELECT COUNT(*) FROM tickets " +
                "WHERE DATE(booking_time)=CURDATE()";

        return getInt(sql);
    }

    /* ================= TOTAL REVENUE ================= */

    public double getTotalRevenue() {

        String sql =
                "SELECT IFNULL(SUM(amount),0) " +
                "FROM tickets WHERE status='BOOKED'";

        return getDouble(sql);
    }

    /* ================= TODAY REVENUE ================= */

    public double getTodayRevenue() {

        String sql =
                "SELECT IFNULL(SUM(amount),0) " +
                "FROM tickets " +
                "WHERE status='BOOKED' " +
                "AND DATE(booking_time)=CURDATE()";

        return getDouble(sql);
    }
}