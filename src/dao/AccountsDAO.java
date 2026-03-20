package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AccountsDAO {

    private static final String NULL_MARKER = "__NULL__";
    private static final Map<String, String> PICKED_COL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> TABLE_COLUMNS_CACHE = new ConcurrentHashMap<>();

    private Set<String> columns(String tableName) {
        if (TABLE_COLUMNS_CACHE.containsKey(tableName)) {
            return TABLE_COLUMNS_CACHE.get(tableName);
        }

        String sql =
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ?";

        Set<String> cols = ConcurrentHashMap.newKeySet();
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cols.add(rs.getString(1));
                }
            }
        } catch (Exception e) {
            cols = Collections.emptySet();
        }

        TABLE_COLUMNS_CACHE.put(tableName, cols);
        return cols;
    }

    private boolean tableExists(String tableName) {
        return !columns(tableName).isEmpty();
    }

    private String firstColumn(String table, String... candidates) {
        Set<String> cols = columns(table);
        if (cols.isEmpty()) return null;

        for (String c : candidates) {
            if (cols.contains(c)) {
                return c;
            }
        }
        return null;
    }

    private String col(String key, Resolver resolver) {
        if (PICKED_COL_CACHE.containsKey(key)) {
            String v = PICKED_COL_CACHE.get(key);
            return NULL_MARKER.equals(v) ? null : v;
        }

        String r = resolver.resolve();
        PICKED_COL_CACHE.put(key, r == null ? NULL_MARKER : r);
        return r;
    }

    private String bookingAmountCol() {
        return col("bookings.amount", () -> firstColumn("bookings", "amount", "total_amount"));
    }

    private String bookingStatusCol() {
        return col("bookings.status", () -> firstColumn("bookings", "status"));
    }

    private String bookingTimeCol() {
        return col("bookings.time", () -> firstColumn("bookings", "booking_time", "created_at"));
    }

    private String refundCol() {
        return col("cancellations.refund", () -> firstColumn("cancellations", "refund_amount", "amount"));
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

    private int getInt(String sql) {
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public double getTotalRevenueBooked() {
        String amount = bookingAmountCol();
        if (amount == null || !tableExists("bookings")) return 0;
        String status = bookingStatusCol();
        String sql =
                "SELECT IFNULL(SUM(" + amount + "),0) FROM bookings " +
                        (status == null ? "" : "WHERE " + status + "='CONFIRMED'");
        return getDouble(sql);
    }

    public double getTotalRefunded() {
        if (tableExists("cancellations")) {
            String refund = refundCol();
            if (refund != null) {
                return getDouble("SELECT IFNULL(SUM(" + refund + "),0) FROM cancellations");
            }
        }

        String amount = bookingAmountCol();
        String status = bookingStatusCol();
        if (amount == null || status == null || !tableExists("bookings")) return 0;
        return getDouble("SELECT IFNULL(SUM(" + amount + "),0) FROM bookings WHERE " + status + "='CANCELLED'");
    }

    public double getTodayRevenue() {
        String amount = bookingAmountCol();
        String time = bookingTimeCol();
        String status = bookingStatusCol();
        if (amount == null || time == null || !tableExists("bookings")) return 0;
        String sql =
                "SELECT IFNULL(SUM(" + amount + "),0) FROM bookings " +
                        "WHERE DATE(" + time + ")=CURDATE() " +
                        (status == null ? "" : "AND " + status + "='CONFIRMED'");
        return getDouble(sql);
    }

    public int getTotalBookings() {
        String status = bookingStatusCol();
        if (!tableExists("bookings")) return 0;
        String sql = "SELECT COUNT(*) FROM bookings " +
                (status == null ? "" : "WHERE " + status + "='CONFIRMED'");
        return getInt(sql);
    }

    public int getTodayBookings() {
        String time = bookingTimeCol();
        if (time == null || !tableExists("bookings")) return 0;
        return getInt("SELECT COUNT(*) FROM bookings WHERE DATE(" + time + ")=CURDATE()");
    }

    public int getCancelledTickets() {
        String status = bookingStatusCol();
        if (status == null || !tableExists("bookings")) return 0;
        return getInt("SELECT COUNT(*) FROM bookings WHERE " + status + "='CANCELLED'");
    }

    public double getNetRevenue() {
        return getTotalRevenueBooked() - getTotalRefunded();
    }

    @FunctionalInterface
    private interface Resolver {
        String resolve();
    }
}
