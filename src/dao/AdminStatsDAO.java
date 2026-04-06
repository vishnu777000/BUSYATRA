package dao;

import util.DBConnectionUtil;
import util.SchemaCache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminStatsDAO {

    private boolean tableExists(String table) {
        return SchemaCache.tableExists(table);
    }

    private boolean columnExists(String table, String column) {
        return SchemaCache.columnExists(table, column);
    }

    private int getInt(String sql) {
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double getDouble(String sql) {
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (Exception e) {
            return 0;
        }
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

    public StatsSnapshot getDashboardSnapshot() {
        StatsSnapshot snapshot = new StatsSnapshot();

        String bookingsAmountCol = columnExists("bookings", "total_amount") ? "total_amount"
                : (columnExists("bookings", "amount") ? "amount" : null);
        String bookingsTimeCol = columnExists("bookings", "booking_time") ? "booking_time"
                : (columnExists("bookings", "created_at") ? "created_at" : null);
        boolean bookingStatusSupported = columnExists("bookings", "status");

        try (Connection con = DBConnectionUtil.getConnection()) {
            if (tableExists("users")) {
                snapshot.users = getInt(con, "SELECT COUNT(*) FROM users");
            }
            if (tableExists("buses")) {
                snapshot.buses = getInt(con, "SELECT COUNT(*) FROM buses");
            }
            if (tableExists("routes")) {
                snapshot.routes = getInt(con, "SELECT COUNT(*) FROM routes");
            }
            if (tableExists("schedules")) {
                snapshot.schedules = getInt(con, "SELECT COUNT(*) FROM schedules");
            }
            if (tableExists("tickets")) {
                snapshot.tickets = getInt(con, "SELECT COUNT(*) FROM tickets");
            } else if (tableExists("bookings")) {
                snapshot.tickets = getInt(con, "SELECT COUNT(*) FROM bookings");
            }
            if (tableExists("bookings") && bookingStatusSupported) {
                snapshot.cancelled = getInt(con, "SELECT COUNT(*) FROM bookings WHERE status='CANCELLED'");
            }
            if (tableExists("bookings") && bookingsAmountCol != null) {
                String revenueSql = "SELECT IFNULL(SUM(" + bookingsAmountCol + "),0) FROM bookings" +
                        (bookingStatusSupported ? " WHERE status='CONFIRMED'" : "");
                snapshot.revenue = getDouble(con, revenueSql);
            }
            if (tableExists("bookings") && bookingsTimeCol != null) {
                snapshot.todayBookings = getInt(con,
                        "SELECT COUNT(*) FROM bookings WHERE DATE(" + bookingsTimeCol + ")=CURDATE()");
                if (bookingsAmountCol != null) {
                    String todayRevenueSql =
                            "SELECT IFNULL(SUM(" + bookingsAmountCol + "),0) FROM bookings WHERE DATE(" +
                            bookingsTimeCol + ")=CURDATE()" +
                            (bookingStatusSupported ? " AND status='CONFIRMED'" : "");
                    snapshot.todayRevenue = getDouble(con, todayRevenueSql);
                }
            }
        } catch (Exception ignored) {

        }

        return snapshot;
    }

    public int getTotalUsers() {
        return tableExists("users") ? getInt("SELECT COUNT(*) FROM users") : 0;
    }

    public int getTotalBuses() {
        return tableExists("buses") ? getInt("SELECT COUNT(*) FROM buses") : 0;
    }

    public int getTotalRoutes() {
        return tableExists("routes") ? getInt("SELECT COUNT(*) FROM routes") : 0;
    }

    public int getTotalSchedules() {
        return tableExists("schedules") ? getInt("SELECT COUNT(*) FROM schedules") : 0;
    }

    public int getTotalTickets() {
        if (tableExists("tickets")) return getInt("SELECT COUNT(*) FROM tickets");
        if (tableExists("bookings")) return getInt("SELECT COUNT(*) FROM bookings");
        return 0;
    }

    public int getCancelledTickets() {
        if (!tableExists("bookings") || !columnExists("bookings", "status")) return 0;
        return getInt("SELECT COUNT(*) FROM bookings WHERE status='CANCELLED'");
    }

    public int getTodayBookings() {
        if (!tableExists("bookings")) return 0;

        String timeCol = columnExists("bookings", "booking_time") ? "booking_time"
                : (columnExists("bookings", "created_at") ? "created_at" : null);
        if (timeCol == null) return 0;

        return getInt("SELECT COUNT(*) FROM bookings WHERE DATE(" + timeCol + ")=CURDATE()");
    }

    public double getTotalRevenue() {
        if (!tableExists("bookings")) return 0;

        String amountCol = columnExists("bookings", "total_amount") ? "total_amount"
                : (columnExists("bookings", "amount") ? "amount" : null);
        if (amountCol == null) return 0;

        String sql = "SELECT IFNULL(SUM(" + amountCol + "),0) FROM bookings" +
                (columnExists("bookings", "status") ? " WHERE status='CONFIRMED'" : "");
        return getDouble(sql);
    }

    public double getTodayRevenue() {
        if (!tableExists("bookings")) return 0;

        String amountCol = columnExists("bookings", "total_amount") ? "total_amount"
                : (columnExists("bookings", "amount") ? "amount" : null);
        String timeCol = columnExists("bookings", "booking_time") ? "booking_time"
                : (columnExists("bookings", "created_at") ? "created_at" : null);
        if (amountCol == null || timeCol == null) return 0;

        String sql = "SELECT IFNULL(SUM(" + amountCol + "),0) FROM bookings WHERE DATE(" + timeCol + ")=CURDATE()" +
                (columnExists("bookings", "status") ? " AND status='CONFIRMED'" : "");
        return getDouble(sql);
    }

    public static class StatsSnapshot {
        public int users;
        public int buses;
        public int routes;
        public int schedules;
        public int tickets;
        public int cancelled;
        public int todayBookings;
        public double revenue;
        public double todayRevenue;
    }
}
