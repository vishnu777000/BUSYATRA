package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AccountsDAO {

    /* ================= GENERIC HELPERS ================= */

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

    /* ================= TOTAL BOOKED REVENUE ================= */

    public double getTotalRevenueBooked() {

        String sql =
                "SELECT IFNULL(SUM(amount),0) " +
                "FROM tickets WHERE status='BOOKED'";

        return getDouble(sql);
    }

    /* ================= TOTAL REFUNDED ================= */

    public double getTotalRefunded() {

        String sql =
                "SELECT IFNULL(SUM(amount),0) " +
                "FROM tickets WHERE status='CANCELLED'";

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

    /* ================= TOTAL BOOKINGS ================= */

    public int getTotalBookings() {

        String sql =
                "SELECT COUNT(*) " +
                "FROM tickets WHERE status='BOOKED'";

        return getInt(sql);
    }

    /* ================= TODAY BOOKINGS ================= */

    public int getTodayBookings() {

        String sql =
                "SELECT COUNT(*) " +
                "FROM tickets " +
                "WHERE DATE(booking_time)=CURDATE()";

        return getInt(sql);
    }

    /* ================= CANCELLED BOOKINGS ================= */

    public int getCancelledTickets() {

        String sql =
                "SELECT COUNT(*) " +
                "FROM tickets WHERE status='CANCELLED'";

        return getInt(sql);
    }

    /* ================= NET REVENUE ================= */

    public double getNetRevenue() {

        return getTotalRevenueBooked() - getTotalRefunded();
    }
}