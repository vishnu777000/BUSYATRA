package dao;

import util.DBConnectionUtil;
import util.SchemaCache;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {

    private boolean tableExists(String tableName) {
        return SchemaCache.tableExists(tableName);
    }

    private boolean columnExists(String tableName, String columnName) {
        return SchemaCache.columnExists(tableName, columnName);
    }

    private String firstExistingColumn(String table, String... candidates) {
        return SchemaCache.firstExistingColumn(table, candidates);
    }

    

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

            ps.setInt(1, bookingId);   
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

    

    public List<String[]> getTicketsByUser(int userId) {

        List<String[]> list = new ArrayList<>();

        String routeNameCol = firstExistingColumn("routes", "route_name", "name", "route");
        String ticketSeatsCol = firstExistingColumn("tickets", "seats", "seat_no", "seat_number", "seat");
        String ticketAmountCol = firstExistingColumn("tickets", "amount", "total_amount", "fare", "price");
        String ticketStatusCol = firstExistingColumn("tickets", "status", "ticket_status");
        String ticketTimeCol = firstExistingColumn("tickets", "booking_time", "created_at");
        String ticketUserCol = firstExistingColumn("tickets", "user_id", "uid");
        String ticketScheduleCol = firstExistingColumn("tickets", "schedule_id");
        String ticketBookingCol = firstExistingColumn("tickets", "booking_id");
        String bookingScheduleCol = firstExistingColumn("bookings", "schedule_id");

        if (ticketUserCol == null || ticketSeatsCol == null || ticketAmountCol == null || ticketTimeCol == null) {
            return list;
        }
        if (ticketScheduleCol == null && (ticketBookingCol == null || bookingScheduleCol == null || !tableExists("bookings"))) {
            return list;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT t.id, " +
                        (routeNameCol != null ? "r." + routeNameCol : "''") + " AS route_name, " +
                        "t." + ticketSeatsCol + " AS seats, " +
                        "t." + ticketAmountCol + " AS amount, " +
                        (ticketStatusCol != null ? "t." + ticketStatusCol : "'BOOKED'") + " AS status, " +
                        "t." + ticketTimeCol + " AS booking_time " +
                        "FROM tickets t "
        );
        if (ticketBookingCol != null && tableExists("bookings")) {
            sql.append("LEFT JOIN bookings bk ON t.").append(ticketBookingCol).append(" = bk.id ");
        }
        if (ticketScheduleCol != null) {
            sql.append("JOIN schedules s ON t.").append(ticketScheduleCol).append(" = s.id ");
        } else {
            sql.append("JOIN schedules s ON bk.").append(bookingScheduleCol).append(" = s.id ");
        }
        sql.append("JOIN routes r ON s.route_id = r.id ");
        sql.append("WHERE t.").append(ticketUserCol).append("=? ");
        sql.append("ORDER BY t.").append(ticketTimeCol).append(" DESC");

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

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

    

    public String[] getTicketFullDetails(int ticketId) {

        String userNameCol = firstExistingColumn("users", "name", "full_name", "username", "user_name");
        String routeNameCol = firstExistingColumn("routes", "route_name", "name", "route");
        String busOperatorCol = firstExistingColumn("buses", "operator", "bus_name", "name");
        String busTypeCol = firstExistingColumn("buses", "bus_type", "type");
        String ticketSeatsCol = firstExistingColumn("tickets", "seats", "seat_no", "seat_number", "seat");
        String ticketAmountCol = firstExistingColumn("tickets", "amount", "total_amount", "fare", "price");
        String ticketUserCol = firstExistingColumn("tickets", "user_id", "uid");
        String ticketScheduleCol = firstExistingColumn("tickets", "schedule_id");
        String ticketBookingCol = firstExistingColumn("tickets", "booking_id");
        String bookingScheduleCol = firstExistingColumn("bookings", "schedule_id");
        String bookingUserCol = firstExistingColumn("bookings", "user_id");

        if (!tableExists("tickets") || !tableExists("users") || !tableExists("schedules") || !tableExists("buses") || !tableExists("routes")) {
            return null;
        }
        if (ticketScheduleCol == null && (ticketBookingCol == null || bookingScheduleCol == null || !tableExists("bookings"))) {
            return null;
        }

        String userJoinExpr = ticketUserCol != null
                ? "t." + ticketUserCol
                : (ticketBookingCol != null && bookingUserCol != null && tableExists("bookings"))
                ? "bk." + bookingUserCol
                : null;
        if (userJoinExpr == null) return null;

        StringBuilder sql = new StringBuilder(
                "SELECT " +
                (userNameCol != null ? "u." + userNameCol : "''") + " AS passenger_name, " +
                (routeNameCol != null ? "r." + routeNameCol : "''") + " AS route_name, " +
                (busOperatorCol != null ? "b." + busOperatorCol : "''") + " AS operator, " +
                (busTypeCol != null ? "b." + busTypeCol : "''") + " AS bus_type, " +
                (ticketSeatsCol != null ? "t." + ticketSeatsCol : "''") + " AS seats, " +
                (ticketAmountCol != null ? "t." + ticketAmountCol : "0") + " AS amount, " +
                "s.departure_time " +
                "FROM tickets t "
        );
        if (ticketBookingCol != null && tableExists("bookings")) {
            sql.append("LEFT JOIN bookings bk ON t.").append(ticketBookingCol).append(" = bk.id ");
        }
        sql.append("JOIN users u ON ").append(userJoinExpr).append(" = u.id ");

        if (ticketScheduleCol != null) {
            sql.append("JOIN schedules s ON t.").append(ticketScheduleCol).append(" = s.id ");
        } else {
            sql.append("JOIN schedules s ON bk.").append(bookingScheduleCol).append(" = s.id ");
        }
        sql.append("JOIN buses b ON s.bus_id = b.id ");
        sql.append("JOIN routes r ON s.route_id = r.id ");
        sql.append("WHERE t.id=?");

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

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
