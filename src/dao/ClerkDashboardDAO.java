package dao;

import config.DBConfig;
import util.SchemaCache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ClerkDashboardDAO {

    private boolean tableExists(String table) {
        return SchemaCache.tableExists(table);
    }

    private String firstColumn(String table, String... columns) {
        return SchemaCache.firstExistingColumn(table, columns);
    }

    private int getInt(Connection con, String sql) {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double getDouble(Connection con, String sql) {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String routeDisplayExpr() {
        String routeNameCol = firstColumn("routes", "route_name", "name");
        if (routeNameCol != null) {
            return "r." + routeNameCol;
        }

        String sourceCityCol = firstColumn("routes", "source_city_id");
        String destinationCityCol = firstColumn("routes", "destination_city_id");
        if (sourceCityCol != null && destinationCityCol != null && tableExists("cities")) {
            return "CONCAT(COALESCE(c1.name, '-'), ' -> ', COALESCE(c2.name, '-'))";
        }

        return "'Route'";
    }

    public DashboardSnapshot getTodaySnapshot() {
        DashboardSnapshot snapshot = new DashboardSnapshot();

        String bookingTimeCol = firstColumn("bookings", "created_at", "booking_time");
        String bookingStatusCol = firstColumn("bookings", "status");
        String bookingAmountCol = firstColumn("bookings", "amount", "total_amount");
        String seatsCol = firstColumn("buses", "total_seats", "seat_count", "seats");
        String scheduleStatusCol = firstColumn("schedules", "status");

        try (Connection con = DBConfig.getConnection()) {
            if (tableExists("bookings") && bookingTimeCol != null) {
                snapshot.tickets = getInt(con,
                        "SELECT COUNT(*) FROM bookings WHERE DATE(" + bookingTimeCol + ")=CURDATE()");
                if (bookingAmountCol != null) {
                    String revenueSql =
                            "SELECT IFNULL(SUM(" + bookingAmountCol + "),0) FROM bookings WHERE DATE(" +
                            bookingTimeCol + ")=CURDATE()" +
                            (bookingStatusCol != null ? " AND " + bookingStatusCol + "='CONFIRMED'" : "");
                    snapshot.revenue = getDouble(con, revenueSql);
                }
                if (bookingStatusCol != null) {
                    snapshot.cancellations = getInt(con,
                            "SELECT COUNT(*) FROM bookings WHERE " + bookingStatusCol + "='CANCELLED' " +
                            "AND DATE(" + bookingTimeCol + ")=CURDATE()");
                }
            }

            if (tableExists("schedules")) {
                String busesTodaySql =
                        "SELECT COUNT(*) FROM schedules WHERE DATE(departure_time)=CURDATE()" +
                        (scheduleStatusCol != null ? " AND " + scheduleStatusCol + "='ACTIVE'" : "");
                snapshot.buses = getInt(con, busesTodaySql);
            }

            if (tableExists("booked_seats") && tableExists("bookings") && bookingTimeCol != null) {
                String bookedSeatsSql =
                        "SELECT COUNT(*) FROM booked_seats bs " +
                        "JOIN bookings b ON bs.booking_id = b.id " +
                        "WHERE DATE(b." + bookingTimeCol + ")=CURDATE()";
                snapshot.bookedSeats = getInt(con, bookedSeatsSql);
            }

            if (tableExists("buses") && seatsCol != null) {
                int totalSeats = getInt(con, "SELECT IFNULL(SUM(" + seatsCol + "),0) FROM buses");
                snapshot.availableSeats = Math.max(0, totalSeats - snapshot.bookedSeats);
            }
        } catch (Exception ignored) {

        }

        return snapshot;
    }

    public List<String[]> getTodaySchedules(int limit) {
        List<String[]> rows = new ArrayList<>();
        if (!tableExists("schedules") || !tableExists("buses") || !tableExists("routes")) {
            return rows;
        }

        String seatsCol = firstColumn("buses", "total_seats", "seat_count", "seats");
        String scheduleStatusCol = firstColumn("schedules", "status");
        String bookingSeatCol = firstColumn("bookings", "seat_no", "seat", "seat_number");
        String bookingStatusCol = firstColumn("bookings", "status");
        String sourceCityCol = firstColumn("routes", "source_city_id");
        String destinationCityCol = firstColumn("routes", "destination_city_id");
        String routeExpr = routeDisplayExpr();
        int normalizedLimit = Math.max(1, limit);

        String availableSeatsExpr;
        if (seatsCol != null && bookingSeatCol != null && tableExists("bookings")) {
            availableSeatsExpr =
                    "(COALESCE(b." + seatsCol + ", 0) - IFNULL((" +
                    "SELECT COUNT(DISTINCT bk." + bookingSeatCol + ") FROM bookings bk WHERE bk.schedule_id = s.id" +
                    (bookingStatusCol != null ? " AND bk." + bookingStatusCol + "='CONFIRMED'" : "") +
                    "), 0))";
        } else if (seatsCol != null) {
            availableSeatsExpr = "COALESCE(b." + seatsCol + ", 0)";
        } else {
            availableSeatsExpr = "0";
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append(routeExpr).append(" AS route_name, ")
                .append("b.operator AS bus_name, ")
                .append("DATE_FORMAT(s.departure_time, '%H:%i') AS departure_value, ")
                .append(availableSeatsExpr).append(" AS available_seats, ")
                .append(scheduleStatusCol == null ? "'ACTIVE'" : "s." + scheduleStatusCol)
                .append(" AS status_value ")
                .append("FROM schedules s ")
                .append("JOIN buses b ON s.bus_id = b.id ")
                .append("JOIN routes r ON s.route_id = r.id ");

        if (sourceCityCol != null && destinationCityCol != null && tableExists("cities")) {
            sql.append("LEFT JOIN cities c1 ON c1.id = r.").append(sourceCityCol).append(" ")
                    .append("LEFT JOIN cities c2 ON c2.id = r.").append(destinationCityCol).append(" ");
        }

        sql.append("WHERE DATE(s.departure_time)=CURDATE() ");
        if (scheduleStatusCol != null) {
            sql.append("AND s.").append(scheduleStatusCol).append("='ACTIVE' ");
        }
        sql.append("ORDER BY s.departure_time ASC LIMIT ").append(normalizedLimit);

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int seats = rs.getInt("available_seats");
                String status = rs.getString("status_value");
                rows.add(new String[]{
                        rs.getString("route_name"),
                        rs.getString("bus_name"),
                        rs.getString("departure_value"),
                        String.valueOf(Math.max(0, seats)),
                        seats <= 0 ? "FULL" : (status == null || status.isBlank() ? "ACTIVE" : status)
                });
            }
        } catch (Exception ignored) {

        }

        return rows;
    }

    public int getTodayTickets() {
        return getTodaySnapshot().tickets;
    }

    public double getTodayRevenue() {
        return getTodaySnapshot().revenue;
    }

    public int getTodayBuses() {
        return getTodaySnapshot().buses;
    }

    public int getAvailableSeats() {
        return getTodaySnapshot().availableSeats;
    }

    public int getTodayCancellations() {
        return getTodaySnapshot().cancellations;
    }

    public int getBookedSeatsToday() {
        return getTodaySnapshot().bookedSeats;
    }

    public static class DashboardSnapshot {
        public int tickets;
        public double revenue;
        public int buses;
        public int availableSeats;
        public int cancellations;
        public int bookedSeats;
    }
}
