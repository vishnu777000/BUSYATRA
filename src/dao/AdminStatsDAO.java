package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class AdminStatsDAO {

    private final Map<String, Boolean> tableCache = new HashMap<>();
    private final Map<String, Boolean> columnCache = new HashMap<>();

    private boolean tableExists(String table) {
        if (tableCache.containsKey(table)) {
            return tableCache.get(table);
        }
        String sql = "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ? LIMIT 1";
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                tableCache.put(table, exists);
                return exists;
            }
        } catch (Exception e) {
            tableCache.put(table, false);
            return false;
        }
    }

    private boolean columnExists(String table, String column) {
        String key = table + "." + column;
        if (columnCache.containsKey(key)) {
            return columnCache.get(key);
        }
        String sql = "SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                columnCache.put(key, exists);
                return exists;
            }
        } catch (Exception e) {
            columnCache.put(key, false);
            return false;
        }
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
        if (!tableExists("bookings")) return 0;

        if (columnExists("bookings", "status")) {
            return getInt("SELECT COUNT(*) FROM bookings WHERE status='CANCELLED'");
        }

        return 0;
    }

    public int getTodayBookings() {

        if (!tableExists("bookings")) return 0;

        String timeCol = columnExists("bookings", "booking_time") ? "booking_time" :
                (columnExists("bookings", "created_at") ? "created_at" : null);

        if (timeCol == null) return 0;

        String sql =
                "SELECT COUNT(*) FROM bookings WHERE DATE(" + timeCol + ")=CURDATE()";

        return getInt(sql);
    }

    public double getTotalRevenue() {

        if (!tableExists("bookings")) return 0;

        String amountCol = columnExists("bookings", "total_amount") ? "total_amount" :
                (columnExists("bookings", "amount") ? "amount" : null);

        if (amountCol == null) return 0;

        String sql;
        if (columnExists("bookings", "status")) {
            sql = "SELECT IFNULL(SUM(" + amountCol + "),0) FROM bookings WHERE status='CONFIRMED'";
        } else {
            sql = "SELECT IFNULL(SUM(" + amountCol + "),0) FROM bookings";
        }

        return getDouble(sql);
    }

    public double getTodayRevenue() {

        if (!tableExists("bookings")) return 0;

        String amountCol = columnExists("bookings", "total_amount") ? "total_amount" :
                (columnExists("bookings", "amount") ? "amount" : null);
        String timeCol = columnExists("bookings", "booking_time") ? "booking_time" :
                (columnExists("bookings", "created_at") ? "created_at" : null);

        if (amountCol == null || timeCol == null) return 0;

        String sql;
        if (columnExists("bookings", "status")) {
            sql = "SELECT IFNULL(SUM(" + amountCol + "),0) FROM bookings WHERE status='CONFIRMED' AND DATE(" + timeCol + ")=CURDATE()";
        } else {
            sql = "SELECT IFNULL(SUM(" + amountCol + "),0) FROM bookings WHERE DATE(" + timeCol + ")=CURDATE()";
        }

        return getDouble(sql);
    }
}
