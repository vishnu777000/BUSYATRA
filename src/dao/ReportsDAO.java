package dao;

import config.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReportsDAO {

    private static final String NULL_MARKER = "__NULL__";
    private static final Map<String, String> COL_CACHE = new ConcurrentHashMap<>();

    private String firstColumn(String table, String... cols) {
        String sql =
                "SELECT 1 FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";
        for (String c : cols) {
            try (Connection con = DBConfig.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, table);
                ps.setString(2, c);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return c;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    private String col(String key, Resolver resolver) {
        if (COL_CACHE.containsKey(key)) {
            String v = COL_CACHE.get(key);
            return NULL_MARKER.equals(v) ? null : v;
        }
        String r = resolver.resolve();
        COL_CACHE.put(key, r == null ? NULL_MARKER : r);
        return r;
    }

    private String amountCol() {
        return col("bookings.amount", () -> firstColumn("bookings", "amount", "total_amount"));
    }

    private String statusCol() {
        return col("bookings.status", () -> firstColumn("bookings", "status"));
    }

    private String timeCol() {
        return col("bookings.time", () -> firstColumn("bookings", "created_at", "booking_time"));
    }

    private String userDisplayExpr() {
        String usersName = col("users.name", () -> firstColumn("users", "name", "full_name", "username"));
        if (usersName != null) return "u." + usersName;
        return "COALESCE(NULLIF(TRIM(up.name),''), u.email)";
    }

    private String[] buildRow(ResultSet rs) throws Exception {
        return new String[]{
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("source"),
                rs.getString("destination"),
                rs.getString("amount"),
                rs.getString("status"),
                rs.getString("booking_time")
        };
    }

    private String baseQuery() {
        String amount = amountCol();
        String status = statusCol();
        String time = timeCol();
        if (amount == null || time == null) return null;

        return
                "SELECT b.id, " + userDisplayExpr() + " AS name, " +
                "cs.name AS source, cd.name AS destination, " +
                "b." + amount + " AS amount, " +
                (status == null ? "'CONFIRMED'" : "b." + status) + " AS status, " +
                "b." + time + " AS booking_time " +
                "FROM bookings b " +
                "JOIN users u ON b.user_id = u.id " +
                "LEFT JOIN user_profiles up ON up.user_id = u.id " +
                "LEFT JOIN route_stops rsf ON rsf.id = b.from_stop_id " +
                "LEFT JOIN route_stops rst ON rst.id = b.to_stop_id " +
                "LEFT JOIN cities cs ON cs.id = rsf.city_id " +
                "LEFT JOIN cities cd ON cd.id = rst.city_id ";
    }

    public List<String[]> getAllBookings() {
        List<String[]> list = new ArrayList<>();
        String q = baseQuery();
        if (q == null) return list;

        String sql = q + "ORDER BY b.id DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(buildRow(rs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String[]> searchBooking(String query) {
        List<String[]> list = new ArrayList<>();
        String q = baseQuery();
        if (q == null) return list;

        String sql =
                q + "WHERE CAST(b.id AS CHAR) LIKE ? OR " + userDisplayExpr() + " LIKE ? " +
                "ORDER BY b.id DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String kw = "%" + (query == null ? "" : query) + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(buildRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String[]> getBookingsByDate(String startDate, String endDate) {
        List<String[]> list = new ArrayList<>();
        String q = baseQuery();
        String time = timeCol();
        if (q == null || time == null) return list;

        String sql =
                q + "WHERE DATE(b." + time + ") BETWEEN ? AND ? ORDER BY b." + time + " DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(buildRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public double getTotalRevenue() {
        String amount = amountCol();
        String status = statusCol();
        if (amount == null) return 0;

        String sql =
                "SELECT IFNULL(SUM(" + amount + "),0) FROM bookings " +
                (status == null ? "" : "WHERE " + status + "='CONFIRMED'");

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface Resolver {
        String resolve();
    }
}
