package dao;

import config.DBConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClerkDashboardDAO {

    /* ================= GENERIC HELPERS ================= */

    private int getInt(String sql) {

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private double getDouble(String sql) {

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            if (rs.next()) return rs.getDouble(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /* ================= TODAY BOOKINGS ================= */

    public int getTodayTickets() {

        return getInt(
                "SELECT COUNT(*) FROM bookings " +
                "WHERE DATE(created_at)=CURDATE()"
        );
    }

    /* ================= TODAY REVENUE ================= */

    public double getTodayRevenue() {

        return getDouble(
                "SELECT IFNULL(SUM(amount),0) FROM bookings " +
                "WHERE status='CONFIRMED' " +
                "AND DATE(created_at)=CURDATE()"
        );
    }

    /* ================= TODAY BUSES ================= */

    public int getTodayBuses() {

        return getInt(
                "SELECT COUNT(*) FROM schedules " +
                "WHERE DATE(departure_time)=CURDATE()"
        );
    }

    /* ================= TOTAL AVAILABLE SEATS (FIXED 🔥) ================= */

    public int getAvailableSeats() {

        String sql =
                "SELECT IFNULL(SUM(b.total_seats),0) - " +
                "IFNULL((SELECT COUNT(*) FROM booked_seats),0) " +
                "FROM buses b";

        return getInt(sql);
    }

    /* ================= TODAY CANCELLATIONS ================= */

    public int getTodayCancellations() {

        return getInt(
                "SELECT COUNT(*) FROM bookings " +
                "WHERE status='CANCELLED' " +
                "AND DATE(created_at)=CURDATE()"
        );
    }

    /* ================= BOOKED SEATS TODAY (FIXED 🔥) ================= */

    public int getBookedSeatsToday() {

        String sql =
                "SELECT COUNT(*) FROM booked_seats bs " +
                "JOIN bookings b ON bs.booking_id = b.id " +
                "WHERE DATE(b.created_at)=CURDATE()";

        return getInt(sql);
    }
}