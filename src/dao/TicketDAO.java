package dao;

import util.DBConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {

    /* ================= HELPER ================= */

    private String[] buildRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("route_name"),
                rs.getString("seats"),
                rs.getString("amount"),
                rs.getString("status"),
                rs.getString("booking_time")
        };
    }

    /* ================= CREATE ================= */

    public int createTicket(int bookingId,
                            int userId,
                            int scheduleId,
                            String seats,
                            double amount) {

        String sql =
                "INSERT INTO tickets " +
                "(booking_id, user_id, schedule_id, seats, amount, status, booking_time) " +
                "VALUES (?,?,?,?,?, 'BOOKED', NOW())";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps =
                     con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, bookingId);   // 🔥 important link
            ps.setInt(2, userId);
            ps.setInt(3, scheduleId);
            ps.setString(4, seats);
            ps.setDouble(5, amount);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    /* ================= USER TICKETS ================= */

    public List<String[]> getTicketsByUser(int userId) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT t.id, r.route_name, t.seats, t.amount, " +
                "t.status, t.booking_time " +
                "FROM tickets t " +
                "JOIN schedules s ON t.schedule_id = s.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "WHERE t.user_id=? " +
                "ORDER BY t.booking_time DESC";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

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

    /* ================= CANCEL ================= */

    public boolean cancelTicket(int ticketId) {

        String sql =
                "UPDATE tickets SET status='CANCELLED' WHERE id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, ticketId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= FULL DETAILS ================= */

    public String[] getTicketFullDetails(int ticketId) {

        String sql =
                "SELECT " +
                "u.name AS passenger_name, " +
                "r.route_name, " +
                "b.operator, " +
                "b.bus_type, " +
                "t.seats, " +
                "t.amount, " +
                "s.departure_time " +
                "FROM tickets t " +
                "JOIN users u ON t.user_id = u.id " +
                "JOIN schedules s ON t.schedule_id = s.id " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "WHERE t.id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, ticketId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                return new String[]{
                        rs.getString("passenger_name"),
                        rs.getString("route_name"),
                        rs.getString("operator"),
                        rs.getString("bus_type"),
                        rs.getString("seats"),
                        rs.getString("departure_time"),
                        rs.getString("amount")
                };
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}