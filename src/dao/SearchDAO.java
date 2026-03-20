package dao;

import config.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SearchDAO {

    private static final String NULL_MARKER = "__NULL__";
    private static final Map<String, String> COL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CacheEntry> SEARCH_CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30_000L;

    private String firstColumn(String table, String... candidates) {
        String sql =
                "SELECT 1 FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";

        for (String candidate : candidates) {
            try (Connection con = DBConfig.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, table);
                ps.setString(2, candidate);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return candidate;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    private String cachedColumn(String key, Resolver resolver) {
        if (COL_CACHE.containsKey(key)) {
            String v = COL_CACHE.get(key);
            return NULL_MARKER.equals(v) ? null : v;
        }
        String resolved = resolver.resolve();
        COL_CACHE.put(key, resolved == null ? NULL_MARKER : resolved);
        return resolved;
    }

    public List<String[]> searchSchedules(String source, String destination, String date) {
        List<String[]> list = new ArrayList<>();

        String sourceName = source == null ? "" : source.trim();
        String destinationName = destination == null ? "" : destination.trim();
        String journeyDate = date == null ? "" : date.trim();
        String cacheKey = (sourceName + "|" + destinationName + "|" + journeyDate).toUpperCase();

        if (sourceName.isBlank() || destinationName.isBlank() || journeyDate.isBlank()) {
            return list;
        }

        CacheEntry cached = SEARCH_CACHE.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.timeMs) <= CACHE_TTL_MS) {
            List<String[]> copy = new ArrayList<>();
            for (String[] row : cached.rows) {
                copy.add(row.clone());
            }
            return copy;
        }

        String baseFareCol = cachedColumn("routes.base_fare",
                () -> firstColumn("routes", "base_fare", "rate_per_km", "fare_per_km"));
        String fareMultiplierCol = cachedColumn("buses.fare_multiplier",
                () -> firstColumn("buses", "fare_multiplier", "multiplier"));
        String seatsCol = cachedColumn("buses.total_seats",
                () -> firstColumn("buses", "total_seats", "seat_count", "seats"));
        String bookingStatusCol = cachedColumn("bookings.status",
                () -> firstColumn("bookings", "status"));
        String bookingSeatCol = cachedColumn("bookings.seat_no",
                () -> firstColumn("bookings", "seat_no", "seat", "seat_number"));
        String bookingFromOrderCol = cachedColumn("bookings.from_order",
                () -> firstColumn("bookings", "from_order", "from_stop_order", "src_order"));
        String bookingToOrderCol = cachedColumn("bookings.to_order",
                () -> firstColumn("bookings", "to_order", "to_stop_order", "dst_order"));
        String scheduleStatusCol = cachedColumn("schedules.status",
                () -> firstColumn("schedules", "status"));

        if (baseFareCol == null || seatsCol == null) {
            return list;
        }

        String fareExpr =
                "ROUND((rs2.distance_from_start - rs1.distance_from_start) * " +
                "COALESCE(r." + baseFareCol + ", 0) * " +
                "COALESCE(" + (fareMultiplierCol == null ? "1" : "b." + fareMultiplierCol) + ", 1), 2)";

        String bookedCountSubquery;
        if (bookingSeatCol != null && bookingFromOrderCol != null && bookingToOrderCol != null) {
            bookedCountSubquery =
                    "SELECT COUNT(DISTINCT bk." + bookingSeatCol + ") FROM bookings bk " +
                    "WHERE bk.schedule_id = s.id " +
                    "AND bk." + bookingFromOrderCol + " < rs2.stop_order " +
                    "AND bk." + bookingToOrderCol + " > rs1.stop_order" +
                    (bookingStatusCol != null ? " AND bk." + bookingStatusCol + " = 'CONFIRMED'" : "");
        } else {
            bookedCountSubquery =
                    "SELECT COUNT(*) FROM bookings bk " +
                    "WHERE bk.schedule_id = s.id" +
                    (bookingStatusCol != null ? " AND bk." + bookingStatusCol + " = 'CONFIRMED'" : "");
        }

        String scheduleStatusFilter = scheduleStatusCol != null ? "AND s." + scheduleStatusCol + " = 'ACTIVE' " : "";

        String sql =
                "SELECT " +
                " s.id AS schedule_id, " +
                " s.route_id, " +
                " b.operator, " +
                " b.bus_type, " +
                " " + fareExpr + " AS fare, " +
                " DATE_FORMAT(s.departure_time, '%H:%i') AS dep_time, " +
                " DATE_FORMAT(s.arrival_time, '%H:%i') AS arr_time, " +
                " rs1.stop_order AS from_order, " +
                " rs2.stop_order AS to_order, " +
                " (COALESCE(b." + seatsCol + ", 0) - IFNULL((" + bookedCountSubquery + "), 0)) AS available_seats " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "JOIN route_stops rs1 ON rs1.route_id = r.id " +
                "JOIN route_stops rs2 ON rs2.route_id = r.id " +
                "JOIN cities c1 ON c1.id = rs1.city_id " +
                "JOIN cities c2 ON c2.id = rs2.city_id " +
                "WHERE UPPER(TRIM(c1.name)) = UPPER(TRIM(?)) " +
                "AND UPPER(TRIM(c2.name)) = UPPER(TRIM(?)) " +
                "AND rs1.stop_order < rs2.stop_order " +
                "AND s.departure_time >= ? " +
                "AND s.departure_time < DATE_ADD(?, INTERVAL 1 DAY) " +
                scheduleStatusFilter +
                "ORDER BY s.departure_time";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, sourceName);
            ps.setString(2, destinationName);
            ps.setString(3, journeyDate + " 00:00:00");
            ps.setString(4, journeyDate + " 00:00:00");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("schedule_id"),
                            rs.getString("route_id"),
                            rs.getString("operator"),
                            rs.getString("bus_type"),
                            rs.getString("fare") != null ? rs.getString("fare") : "0",
                            rs.getString("dep_time"),
                            rs.getString("arr_time"),
                            rs.getString("from_order"),
                            rs.getString("to_order"),
                            rs.getString("available_seats")
                    });
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        CacheEntry entry = new CacheEntry();
        entry.timeMs = now;
        entry.rows = new ArrayList<>();
        for (String[] row : list) {
            entry.rows.add(row.clone());
        }
        SEARCH_CACHE.put(cacheKey, entry);

        return list;
    }

    @FunctionalInterface
    private interface Resolver {
        String resolve();
    }

    private static class CacheEntry {
        long timeMs;
        List<String[]> rows = Collections.emptyList();
    }
}
