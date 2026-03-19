package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class SeatDAO {

    /* ================= HELPERS ================= */

    private int getInt(String sql, int scheduleId) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, scheduleId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private double getDouble(String sql, int scheduleId) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, scheduleId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getDouble(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /* =====================================================
       ========= UNAVAILABLE SEATS (SEGMENT BASED) =========
       ===================================================== */

    public Set<String> getUnavailableSeats(
            int scheduleId,
            int newFrom,
            int newTo) {

        Set<String> set = new HashSet<>();

        // 🔥 Validation
        if (newFrom >= newTo) return set;

        String sql =
                "SELECT seat_no, from_order, to_order " +
                "FROM bookings " +
                "WHERE schedule_id=? AND status='CONFIRMED'";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, scheduleId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                int existingFrom = rs.getInt("from_order");
                int existingTo   = rs.getInt("to_order");

                // 🔥 Overlap logic (core feature)
                if (newFrom < existingTo && newTo > existingFrom) {
                    set.add(rs.getString("seat_no"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return set;
    }

    /* =====================================================
       ================= TOTAL SEATS ========================
       ===================================================== */

    public int getTotalSeatsBySchedule(int scheduleId) {

        String sql =
                "SELECT b.total_seats " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id=b.id " +
                "WHERE s.id=?";

        return getInt(sql, scheduleId);
    }

    /* =====================================================
       ================= BASE FARE ==========================
       ===================================================== */

    public double getFareBySchedule(int scheduleId) {

        String sql =
                "SELECT b.fare_multiplier " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id=b.id " +
                "WHERE s.id=?";

        return getDouble(sql, scheduleId);
    }

    /* =====================================================
       ================= BOOKED SEATS =======================
       ===================================================== */

    public Set<String> getBookedSeats(int scheduleId) {

        Set<String> set = new HashSet<>();

        String sql =
                "SELECT seat_no FROM bookings " +
                "WHERE schedule_id=? AND status='CONFIRMED'";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, scheduleId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                set.add(rs.getString("seat_no"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return set;
    }

    /* =====================================================
       ================= SEAT LABEL =========================
       ===================================================== */

    public String getSeatLabel(int seatNumber) {
        return "S" + seatNumber;
    }
}