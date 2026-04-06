package dao;

import config.DBConfig;
import util.SchemaCache;

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

    private boolean tableExists(String table) {
        return SchemaCache.tableExists(table);
    }

    private String firstColumn(String table, String... cols) {
        return SchemaCache.firstExistingColumn(table, cols);
    }

    private String col(String key, Resolver resolver) {
        if (COL_CACHE.containsKey(key)) {
            String v = COL_CACHE.get(key);
            return NULL_MARKER.equals(v) ? null : v;
        }
        String resolved = resolver.resolve();
        COL_CACHE.put(key, resolved == null ? NULL_MARKER : resolved);
        return resolved;
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

    private String userIdCol() {
        return col("bookings.user_id", () -> firstColumn("bookings", "user_id", "uid"));
    }

    private String fromStopIdCol() {
        return col("bookings.from_stop_id", () -> firstColumn("bookings", "from_stop_id"));
    }

    private String toStopIdCol() {
        return col("bookings.to_stop_id", () -> firstColumn("bookings", "to_stop_id"));
    }

    private String fromCityCol() {
        return col("bookings.from_city", () -> firstColumn("bookings", "from_city", "from_stop", "source", "src"));
    }

    private String toCityCol() {
        return col("bookings.to_city", () -> firstColumn("bookings", "to_city", "to_stop", "destination", "dst"));
    }

    private String userDisplayExpr() {
        String usersName = col("users.name", () -> firstColumn("users", "name", "full_name", "username"));
        boolean hasProfiles = tableExists("user_profiles");
        if (usersName != null && hasProfiles) {
            return "COALESCE(NULLIF(TRIM(up.name), ''), NULLIF(TRIM(u." + usersName + "), ''), u.email)";
        }
        if (usersName != null) {
            return "COALESCE(NULLIF(TRIM(u." + usersName + "), ''), u.email)";
        }
        return hasProfiles ? "COALESCE(NULLIF(TRIM(up.name),''), u.email)" : "u.email";
    }

    private String sourceExpr() {
        String bookingFrom = fromCityCol();
        if (tableExists("route_stops") && tableExists("cities") && fromStopIdCol() != null) {
            return bookingFrom == null
                    ? "COALESCE(cs.name, '-')"
                    : "COALESCE(cs.name, NULLIF(TRIM(b." + bookingFrom + "), ''), '-')";
        }
        return bookingFrom == null ? "'-'" : "COALESCE(NULLIF(TRIM(b." + bookingFrom + "), ''), '-')";
    }

    private String destinationExpr() {
        String bookingTo = toCityCol();
        if (tableExists("route_stops") && tableExists("cities") && toStopIdCol() != null) {
            return bookingTo == null
                    ? "COALESCE(cd.name, '-')"
                    : "COALESCE(cd.name, NULLIF(TRIM(b." + bookingTo + "), ''), '-')";
        }
        return bookingTo == null ? "'-'" : "COALESCE(NULLIF(TRIM(b." + bookingTo + "), ''), '-')";
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
        String userId = userIdCol();
        if (amount == null || time == null || userId == null || !tableExists("bookings") || !tableExists("users")) {
            return null;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT b.id, ")
                .append(userDisplayExpr()).append(" AS name, ")
                .append(sourceExpr()).append(" AS source, ")
                .append(destinationExpr()).append(" AS destination, ")
                .append("b.").append(amount).append(" AS amount, ")
                .append(status == null ? "'CONFIRMED'" : "b." + status).append(" AS status, ")
                .append("b.").append(time).append(" AS booking_time ")
                .append("FROM bookings b ")
                .append("JOIN users u ON b.").append(userId).append(" = u.id ");

        if (tableExists("user_profiles")) {
            sql.append("LEFT JOIN user_profiles up ON up.user_id = u.id ");
        }
        if (tableExists("route_stops") && tableExists("cities") && fromStopIdCol() != null) {
            sql.append("LEFT JOIN route_stops rsf ON rsf.id = b.").append(fromStopIdCol()).append(" ")
                    .append("LEFT JOIN cities cs ON cs.id = rsf.city_id ");
        }
        if (tableExists("route_stops") && tableExists("cities") && toStopIdCol() != null) {
            sql.append("LEFT JOIN route_stops rst ON rst.id = b.").append(toStopIdCol()).append(" ")
                    .append("LEFT JOIN cities cd ON cd.id = rst.city_id ");
        }

        return sql.toString();
    }

    public List<String[]> getAllBookings() {
        return getRecentBookings(0);
    }

    public List<String[]> getRecentBookings(int limit) {
        List<String[]> list = new ArrayList<>();
        String query = baseQuery();
        if (query == null) {
            return list;
        }

        StringBuilder sql = new StringBuilder(query).append("ORDER BY b.id DESC");
        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(buildRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String[]> searchBooking(String queryText) {
        List<String[]> list = new ArrayList<>();
        String query = baseQuery();
        if (query == null) {
            return list;
        }

        String sql =
                query + "WHERE CAST(b.id AS CHAR) LIKE ? OR " + userDisplayExpr() + " LIKE ? " +
                        "ORDER BY b.id DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String kw = "%" + (queryText == null ? "" : queryText.trim()) + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(buildRow(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String[]> getBookingsByDate(String startDate, String endDate) {
        List<String[]> list = new ArrayList<>();
        String query = baseQuery();
        String time = timeCol();
        if (query == null || time == null) {
            return list;
        }

        String sql = query + "WHERE DATE(b." + time + ") BETWEEN ? AND ? ORDER BY b." + time + " DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(buildRow(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public double getTotalRevenue() {
        String amount = amountCol();
        String status = statusCol();
        if (amount == null || !tableExists("bookings")) {
            return 0;
        }

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
