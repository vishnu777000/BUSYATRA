package dao;

import config.DBConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    /* ================= HELPER ================= */

    private String[] buildBookingRow(ResultSet rs) throws SQLException {

        return new String[]{
                rs.getString("id"),
                rs.getString("from_city"),
                rs.getString("to_city"),
                rs.getString("seat_no"),
                rs.getString("amount"),
                rs.getString("journey_date"),
                rs.getString("status")
        };
    }

    /* ================= UPCOMING ================= */

    public String getUpcomingBookingText(int userId) {

        String sql =
                "SELECT from_city, to_city, journey_date " +
                "FROM bookings " +
                "WHERE user_id=? " +
                "AND journey_date >= CURDATE() " +
                "AND status='CONFIRMED' " +
                "ORDER BY journey_date ASC LIMIT 1";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("from_city") + " → " +
                        rs.getString("to_city") + " | " +
                        rs.getDate("journey_date");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "No active booking";
    }

    /* ================= USER BOOKINGS ================= */

    public List<String[]> getBookingsByUser(int userId) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT id,from_city,to_city,seat_no,amount,journey_date,status " +
                "FROM bookings WHERE user_id=? ORDER BY journey_date DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(buildBookingRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= SEAT CHECK (UPDATED 🔥) ================= */

    public boolean isSeatAvailable(int scheduleId,
                                   String seatNo,
                                   int newFrom,
                                   int newTo) {

        String sql =
                "SELECT from_order, to_order FROM booked_seats " +
                "WHERE schedule_id=? AND seat_no=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, scheduleId);
            ps.setString(2, seatNo);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                int existingFrom = rs.getInt("from_order");
                int existingTo = rs.getInt("to_order");

                // 🔥 overlap logic
                if (newFrom < existingTo && newTo > existingFrom) {
                    return false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    /* ================= INSERT BOOKING ================= */

    public int insertBooking(
            int userId,
            int routeId,
            int scheduleId,
            String fromCity,
            String toCity,
            int fromOrder,
            int toOrder,
            String seatNo,
            double amount,
            String journeyDate) {

        String sql =
                "INSERT INTO bookings " +
                "(user_id, route_id, schedule_id, from_city, to_city, " +
                "from_order, to_order, seat_no, amount, journey_date, status, created_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,'CONFIRMED',NOW())";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setInt(2, routeId);
            ps.setInt(3, scheduleId);
            ps.setString(4, fromCity);
            ps.setString(5, toCity);
            ps.setInt(6, fromOrder);
            ps.setInt(7, toOrder);
            ps.setString(8, seatNo);
            ps.setDouble(9, amount);
            ps.setDate(10, Date.valueOf(journeyDate));

            int affected = ps.executeUpdate();

            if (affected > 0) {

                ResultSet rs = ps.getGeneratedKeys();

                if (rs.next()) {

                    int bookingId = rs.getInt(1);

                    // 🔥 INSERT INTO booked_seats
                    insertBookedSeat(bookingId, scheduleId, seatNo, fromOrder, toOrder);

                    return bookingId;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    /* ================= INSERT BOOKED SEAT 🔥 ================= */

    public void insertBookedSeat(int bookingId,
                                 int scheduleId,
                                 String seatNo,
                                 int fromOrder,
                                 int toOrder) {

        String sql =
                "INSERT INTO booked_seats " +
                "(booking_id, schedule_id, seat_no, from_order, to_order) " +
                "VALUES (?,?,?,?,?)";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, bookingId);
            ps.setInt(2, scheduleId);
            ps.setString(3, seatNo);
            ps.setInt(4, fromOrder);
            ps.setInt(5, toOrder);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= CANCEL ================= */

    public boolean cancelBooking(int bookingId) {

        String sql =
                "UPDATE bookings SET status='CANCELLED' WHERE id=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, bookingId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= AMOUNT ================= */

    public double getBookingAmount(int bookingId) {

        String sql =
                "SELECT amount FROM bookings WHERE id=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, bookingId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("amount");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /* ================= TICKET PREVIEW ================= */

    public String[] getTicketPreviewData(int bookingId) {

        String sql =
                "SELECT b.id,u.name,r.route_name," +
                "b.from_city,b.to_city,b.seat_no,b.amount," +
                "b.journey_date,s.departure_time " +
                "FROM bookings b " +
                "JOIN users u ON b.user_id=u.id " +
                "JOIN schedules s ON b.schedule_id=s.id " +
                "JOIN routes r ON b.route_id=r.id " +
                "WHERE b.id=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, bookingId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                return new String[]{
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("route_name"),
                        rs.getString("from_city"),
                        rs.getString("to_city"),
                        rs.getString("seat_no"),
                        rs.getString("amount"),
                        rs.getString("journey_date"),
                        rs.getString("departure_time")
                };
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}