package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScheduleDAO {

    private static final String NULL_MARKER = "__NULL__";
    private static final Map<String, String> COL_CACHE = new ConcurrentHashMap<>();

    private String firstColumn(String table, String... cols) {
        String sql =
                "SELECT 1 FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";
        for (String c : cols) {
            try (Connection con = DBConnectionUtil.getConnection();
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

    private String routeLabelExpr() {
        String routeName = col("routes.route_name", () -> firstColumn("routes", "route_name", "name"));
        if (routeName != null) return "r." + routeName;
        return "CONCAT(c1.name, ' -> ', c2.name)";
    }

    private String statusCol() {
        return col("schedules.status", () -> firstColumn("schedules", "status"));
    }

    private String typeCol() {
        return col("buses.bus_type", () -> firstColumn("buses", "bus_type", "type"));
    }

    private String driverCol() {
        return col("buses.driver_phone", () -> firstColumn("buses", "driver_phone", "contact_phone", "phone"));
    }

    private String[] buildRow(ResultSet rs) throws Exception {
        return new String[]{
                rs.getString("id"),
                rs.getString("operator"),
                rs.getString("route_name"),
                rs.getString("departure_time"),
                rs.getString("arrival_time")
        };
    }

    public List<String[]> getAllSchedules() {
        List<String[]> list = new ArrayList<>();
        String routeExpr = routeLabelExpr();

        String sql =
                "SELECT s.id, b.operator, " + routeExpr + " AS route_name, s.departure_time, s.arrival_time " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "LEFT JOIN cities c1 ON c1.id = r.source_city_id " +
                "LEFT JOIN cities c2 ON c2.id = r.destination_city_id " +
                "ORDER BY s.departure_time DESC";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(buildRow(rs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public String[] getScheduleDetails(int id) {
        String routeExpr = routeLabelExpr();
        String busType = typeCol();
        String driver = driverCol();
        if (busType == null) busType = "'N/A'";

        String sql =
                "SELECT s.id, b.operator, " +
                (busType.startsWith("'") ? busType : "b." + busType) + " AS bus_type, " +
                (driver == null ? "'-'" : "b." + driver) + " AS driver_phone, " +
                "s.departure_time, s.arrival_time, " + routeExpr + " AS route_name " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "LEFT JOIN cities c1 ON c1.id = r.source_city_id " +
                "LEFT JOIN cities c2 ON c2.id = r.destination_city_id " +
                "WHERE s.id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            rs.getString("id"),
                            rs.getString("operator"),
                            rs.getString("bus_type"),
                            rs.getString("driver_phone"),
                            rs.getString("departure_time"),
                            rs.getString("arrival_time"),
                            rs.getString("route_name")
                    };
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addSchedule(int busId, int routeId, Timestamp departure, Timestamp arrival) {
        if (departure.after(arrival)) return false;

        String status = statusCol();
        String sql = status == null
                ? "INSERT INTO schedules (bus_id, route_id, departure_time, arrival_time) VALUES (?,?,?,?)"
                : "INSERT INTO schedules (bus_id, route_id, departure_time, arrival_time, " + status + ") VALUES (?,?,?,?, 'ACTIVE')";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, busId);
            ps.setInt(2, routeId);
            ps.setTimestamp(3, departure);
            ps.setTimestamp(4, arrival);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateSchedule(int id, int busId, int routeId, Timestamp departure, Timestamp arrival) {
        if (departure.after(arrival)) return false;

        String sql =
                "UPDATE schedules SET bus_id=?, route_id=?, departure_time=?, arrival_time=? WHERE id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, busId);
            ps.setInt(2, routeId);
            ps.setTimestamp(3, departure);
            ps.setTimestamp(4, arrival);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteSchedule(int id) {
        String sql = "DELETE FROM schedules WHERE id=?";
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String[]> searchSchedule(String keyword) {
        List<String[]> list = new ArrayList<>();
        String routeExpr = routeLabelExpr();

        String sql =
                "SELECT s.id, b.operator, " + routeExpr + " AS route_name, s.departure_time, s.arrival_time " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id=b.id " +
                "JOIN routes r ON s.route_id=r.id " +
                "LEFT JOIN cities c1 ON c1.id = r.source_city_id " +
                "LEFT JOIN cities c2 ON c2.id = r.destination_city_id " +
                "WHERE b.operator LIKE ? OR " + routeExpr + " LIKE ? " +
                "ORDER BY s.departure_time DESC";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String q = "%" + (keyword == null ? "" : keyword) + "%";
            ps.setString(1, q);
            ps.setString(2, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(buildRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getScheduleCount() {
        String sql = "SELECT COUNT(*) FROM schedules";
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface Resolver {
        String resolve();
    }
}
