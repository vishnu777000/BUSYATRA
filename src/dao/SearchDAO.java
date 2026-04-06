package dao;

import config.DBConfig;
import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SearchDAO {

    private static final String NULL_MARKER = "__NULL__";
    private static final Map<String, String> COL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CacheEntry> SEARCH_CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30_000L;

    private String firstColumn(Connection con, String table, String... candidates) {
        String sql =
                "SELECT 1 FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (String candidate : candidates) {
                ps.setString(1, table);
                ps.setString(2, candidate);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return candidate;
                }
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    private String cachedColumn(Connection con, String key, Resolver resolver) {
        if (COL_CACHE.containsKey(key)) {
            String v = COL_CACHE.get(key);
            return NULL_MARKER.equals(v) ? null : v;
        }
        String resolved = resolver.resolve();
        COL_CACHE.put(key, resolved == null ? NULL_MARKER : resolved);
        return resolved;
    }

    private String derivedBusTypeMultiplierExpr(String busTypeExpr) {
        return "CASE " +
                "WHEN " + busTypeExpr + " IS NULL OR TRIM(" + busTypeExpr + ") = '' THEN 1 " +
                "WHEN UPPER(" + busTypeExpr + ") LIKE '%NON%AC%' AND UPPER(" + busTypeExpr + ") LIKE '%SLEEPER%' THEN 1.08 " +
                "WHEN UPPER(" + busTypeExpr + ") LIKE '%AC%' AND UPPER(" + busTypeExpr + ") LIKE '%SLEEPER%' THEN 1.28 " +
                "WHEN UPPER(" + busTypeExpr + ") LIKE '%NON%AC%' AND UPPER(" + busTypeExpr + ") LIKE '%SEATER%' THEN 0.95 " +
                "WHEN UPPER(" + busTypeExpr + ") LIKE '%AC%' AND UPPER(" + busTypeExpr + ") LIKE '%SEATER%' THEN 1.10 " +
                "WHEN UPPER(" + busTypeExpr + ") LIKE '%NON%AC%' THEN 0.95 " +
                "WHEN UPPER(" + busTypeExpr + ") LIKE '%AC%' THEN 1.10 " +
                "WHEN UPPER(" + busTypeExpr + ") LIKE '%SLEEPER%' THEN 1.08 " +
                "WHEN UPPER(" + busTypeExpr + ") LIKE '%SEATER%' THEN 0.95 " +
                "ELSE 1 END";
    }

    public List<String[]> searchSchedules(String source, String destination, String date) {
        List<String[]> list = new ArrayList<>();

        String sourceName = source == null ? "" : source.trim();
        String destinationName = destination == null ? "" : destination.trim();
        String journeyDate = date == null ? "" : date.trim();

        if (sourceName.isBlank() || destinationName.isBlank() || journeyDate.isBlank()) {
            return list;
        }

        LocalDate requestedDate;
        try {
            requestedDate = LocalDate.parse(journeyDate);
        } catch (Exception ignored) {
            return list;
        }

        LocalDate today = LocalDate.now();
        if (requestedDate.isBefore(today)) {
            return list;
        }

        Timestamp dayStart = Timestamp.valueOf(requestedDate.atStartOfDay());
        Timestamp dayEnd = Timestamp.valueOf(requestedDate.plusDays(1).atStartOfDay());
        Timestamp searchStart = new Timestamp(Math.max(dayStart.getTime(), System.currentTimeMillis()));
        if (!searchStart.before(dayEnd)) {
            return list;
        }

        String cacheKey = (sourceName + "|" + destinationName + "|" + journeyDate).toUpperCase();

        CacheEntry cached = SEARCH_CACHE.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.timeMs) <= CACHE_TTL_MS) {
            List<String[]> copy = new ArrayList<>();
            for (String[] row : cached.rows) {
                copy.add(row.clone());
            }
            return copy;
        }

        try (Connection con = DBConfig.getConnection()) {
            String baseFareCol = cachedColumn(con, "routes.base_fare",
                    () -> firstColumn(con, "routes", "base_fare", "rate_per_km", "fare_per_km"));
            String busTypeCol = cachedColumn(con, "buses.bus_type",
                    () -> firstColumn(con, "buses", "bus_type", "type"));
            String fareMultiplierCol = cachedColumn(con, "buses.fare_multiplier",
                    () -> firstColumn(con, "buses", "fare_multiplier", "multiplier"));
            String seatsCol = cachedColumn(con, "buses.total_seats",
                    () -> firstColumn(con, "buses", "total_seats", "seat_count", "seats"));
            String bookingStatusCol = cachedColumn(con, "bookings.status",
                    () -> firstColumn(con, "bookings", "status"));
            String bookingSeatCol = cachedColumn(con, "bookings.seat_no",
                    () -> firstColumn(con, "bookings", "seat_no", "seat", "seat_number"));
            String bookingFromOrderCol = cachedColumn(con, "bookings.from_order",
                    () -> firstColumn(con, "bookings", "from_order", "from_stop_order", "src_order"));
            String bookingToOrderCol = cachedColumn(con, "bookings.to_order",
                    () -> firstColumn(con, "bookings", "to_order", "to_stop_order", "dst_order"));
            String scheduleStatusCol = cachedColumn(con, "schedules.status",
                    () -> firstColumn(con, "schedules", "status"));
            String routeStopOrderCol = cachedColumn(con, "route_stops.stop_order",
                    () -> firstColumn(con, "route_stops", "stop_order", "stop_no", "seq_no"));
            String routeStopDistanceCol = cachedColumn(con, "route_stops.distance_from_start",
                    () -> firstColumn(con, "route_stops", "distance_from_start", "distance_km", "distance"));
            String routeStopNameCol = cachedColumn(con, "route_stops.stop_name",
                    () -> firstColumn(con, "route_stops", "stop_name", "stop", "city_name"));
            String routeStopCityIdCol = cachedColumn(con, "route_stops.city_id",
                    () -> firstColumn(con, "route_stops", "city_id"));

            if (baseFareCol == null || seatsCol == null || routeStopOrderCol == null || routeStopDistanceCol == null) {
                return list;
            }
            if (routeStopCityIdCol == null && routeStopNameCol == null) {
                return list;
            }

            String busTypeExpr = busTypeCol == null ? "NULL" : "b." + busTypeCol;
            String multiplierExpr =
                    "COALESCE(" + derivedBusTypeMultiplierExpr(busTypeExpr) + ", " +
                            "NULLIF(" + (fareMultiplierCol == null ? "0" : "b." + fareMultiplierCol) + ", 0), 1)";

            String fareExpr =
                    "ROUND((rs2." + routeStopDistanceCol + " - rs1." + routeStopDistanceCol + ") * " +
                    "COALESCE(r." + baseFareCol + ", 0) * " +
                    multiplierExpr + ", 2)";

            String bookedCountSubquery;
            if (bookingSeatCol != null && bookingFromOrderCol != null && bookingToOrderCol != null) {
                bookedCountSubquery =
                        "SELECT COUNT(DISTINCT bk." + bookingSeatCol + ") FROM bookings bk " +
                        "WHERE bk.schedule_id = s.id " +
                        "AND bk." + bookingFromOrderCol + " < rs2." + routeStopOrderCol + " " +
                        "AND bk." + bookingToOrderCol + " > rs1." + routeStopOrderCol +
                        (bookingStatusCol != null ? " AND bk." + bookingStatusCol + " = 'CONFIRMED'" : "");
            } else {
                bookedCountSubquery =
                        "SELECT COUNT(*) FROM bookings bk " +
                        "WHERE bk.schedule_id = s.id" +
                        (bookingStatusCol != null ? " AND bk." + bookingStatusCol + " = 'CONFIRMED'" : "");
            }

            String scheduleStatusFilter = scheduleStatusCol != null ? "AND s." + scheduleStatusCol + " = 'ACTIVE' " : "";
            String stopJoin;
            String stopFilter;
            if (routeStopCityIdCol != null) {
                stopJoin =
                        "JOIN cities c1 ON c1.id = rs1." + routeStopCityIdCol + " " +
                        "JOIN cities c2 ON c2.id = rs2." + routeStopCityIdCol + " ";
                stopFilter =
                        "WHERE UPPER(TRIM(c1.name)) = UPPER(TRIM(?)) " +
                        "AND UPPER(TRIM(c2.name)) = UPPER(TRIM(?)) ";
            } else {
                stopJoin = "";
                stopFilter =
                        "WHERE UPPER(TRIM(rs1." + routeStopNameCol + ")) = UPPER(TRIM(?)) " +
                        "AND UPPER(TRIM(rs2." + routeStopNameCol + ")) = UPPER(TRIM(?)) ";
            }

            String sql =
                    "SELECT " +
                    " s.id AS schedule_id, " +
                    " s.route_id, " +
                    " b.operator, " +
                    " " + busTypeExpr + " AS bus_type, " +
                    " " + fareExpr + " AS fare, " +
                    " DATE_FORMAT(s.departure_time, '%H:%i') AS dep_time, " +
                    " DATE_FORMAT(s.arrival_time, '%H:%i') AS arr_time, " +
                    " rs1." + routeStopOrderCol + " AS from_order, " +
                    " rs2." + routeStopOrderCol + " AS to_order, " +
                    " (COALESCE(b." + seatsCol + ", 0) - IFNULL((" + bookedCountSubquery + "), 0)) AS available_seats " +
                    "FROM schedules s " +
                    "JOIN buses b ON s.bus_id = b.id " +
                    "JOIN routes r ON s.route_id = r.id " +
                    "JOIN route_stops rs1 ON rs1.route_id = r.id " +
                    "JOIN route_stops rs2 ON rs2.route_id = r.id " +
                    stopJoin +
                    stopFilter +
                    "AND rs1." + routeStopOrderCol + " < rs2." + routeStopOrderCol + " " +
                    "AND s.departure_time >= ? " +
                    "AND s.departure_time < ? " +
                    scheduleStatusFilter +
                    "ORDER BY s.departure_time, rs1." + routeStopOrderCol + ", rs2." + routeStopOrderCol;

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, sourceName);
                ps.setString(2, destinationName);
                ps.setTimestamp(3, searchStart);
                ps.setTimestamp(4, dayEnd);

                try (ResultSet rs = ps.executeQuery()) {
                    Map<String, String[]> uniqueRows = new LinkedHashMap<>();
                    while (rs.next()) {
                        String[] row = new String[]{
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
                        };

                        String scheduleId = row[0] == null || row[0].isBlank()
                                ? (row[1] + "|" + row[5] + "|" + row[6])
                                : row[0];
                        uniqueRows.putIfAbsent(scheduleId, row);
                    }

                    list.addAll(uniqueRows.values());
                }
            }
        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
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

    public void clearSearchCache() {
        SEARCH_CACHE.clear();
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
