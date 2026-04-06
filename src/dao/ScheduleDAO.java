package dao;

import util.DBConnectionUtil;
import util.SchemaCache;

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
    private volatile String lastErrorMessage = "";

    private String firstColumn(String table, String... cols) {
        return SchemaCache.firstExistingColumn(table, cols);
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

    public String getLastErrorMessage() {
        return lastErrorMessage == null ? "" : lastErrorMessage;
    }

    private void setLastErrorMessage(String message) {
        lastErrorMessage = message == null ? "" : message.trim();
    }

    private boolean tableExists(String tableName) {
        return SchemaCache.tableExists(tableName);
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

    private boolean validateScheduleWindow(Timestamp departure, Timestamp arrival) {
        setLastErrorMessage("");

        if (departure == null || arrival == null) {
            setLastErrorMessage("Departure and arrival are required.");
            return false;
        }
        if (!arrival.after(departure)) {
            setLastErrorMessage("Arrival must be after departure.");
            return false;
        }
        if (!departure.after(new Timestamp(System.currentTimeMillis()))) {
            setLastErrorMessage("Departure must be in the future.");
            return false;
        }
        return true;
    }

    public boolean isScheduleBookable(int scheduleId) {
        setLastErrorMessage("");

        if (scheduleId <= 0 || !tableExists("schedules")) {
            setLastErrorMessage("Schedule not found.");
            return false;
        }

        String departureCol = firstColumn("schedules", "departure_time", "start_time", "depart_at");
        String statusCol = statusCol();
        if (departureCol == null) {
            setLastErrorMessage("Schedule timing is unavailable.");
            return false;
        }

        String sql =
                "SELECT " + departureCol + " AS departure_value, " +
                (statusCol == null ? "NULL" : statusCol) + " AS status_value " +
                "FROM schedules WHERE id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    setLastErrorMessage("Schedule not found.");
                    return false;
                }

                Timestamp departure = rs.getTimestamp("departure_value");
                String status = rs.getString("status_value");

                if (statusCol != null && status != null && !status.isBlank() && !"ACTIVE".equalsIgnoreCase(status.trim())) {
                    setLastErrorMessage("This schedule is not active for booking.");
                    return false;
                }
                if (departure == null) {
                    setLastErrorMessage("Schedule timing is unavailable.");
                    return false;
                }
                if (!departure.after(new Timestamp(System.currentTimeMillis()))) {
                    setLastErrorMessage("This schedule has already departed.");
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            setLastErrorMessage(DBConnectionUtil.userMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    private String[] buildRow(ResultSet rs) throws Exception {
        return new String[]{
                rs.getString("id"),
                rs.getString("operator"),
                rs.getString("route_name"),
                rs.getString("departure_time"),
                rs.getString("arrival_time"),
                rs.getString("status_value")
        };
    }

    public List<String[]> getAllSchedules() {
        List<String[]> list = new ArrayList<>();
        String routeExpr = routeLabelExpr();
        String statusExpr = statusCol() == null ? "'ACTIVE'" : "s." + statusCol();

        String sql =
                "SELECT s.id, b.operator, " + routeExpr + " AS route_name, s.departure_time, s.arrival_time, " +
                statusExpr + " AS status_value " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "LEFT JOIN cities c1 ON c1.id = r.source_city_id " +
                "LEFT JOIN cities c2 ON c2.id = r.destination_city_id " +
                "WHERE s.departure_time >= CURRENT_TIMESTAMP " +
                "ORDER BY s.departure_time ASC";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(buildRow(rs));
        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
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
            DBConnectionUtil.logIfUnexpected(e);
        }
        return null;
    }

    public String[] getScheduleAdminEditData(int id) {
        String statusExpr = statusCol() == null ? "'ACTIVE'" : "s." + statusCol();

        String sql =
                "SELECT s.id, s.bus_id, s.route_id, s.departure_time, s.arrival_time, " +
                statusExpr + " AS status_value " +
                "FROM schedules s WHERE s.id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            rs.getString("id"),
                            rs.getString("bus_id"),
                            rs.getString("route_id"),
                            rs.getString("departure_time"),
                            rs.getString("arrival_time"),
                            rs.getString("status_value")
                    };
                }
            }
        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
        }
        return null;
    }

    public boolean addSchedule(int busId, int routeId, Timestamp departure, Timestamp arrival) {
        if (!validateScheduleWindow(departure, arrival)) return false;

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
            setLastErrorMessage("");
            boolean created = ps.executeUpdate() > 0;
            if (created) {
                new SearchDAO().clearSearchCache();
            }
            return created;
        } catch (Exception e) {
            setLastErrorMessage(DBConnectionUtil.userMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    public boolean updateSchedule(int id, int busId, int routeId, Timestamp departure, Timestamp arrival) {
        if (!validateScheduleWindow(departure, arrival)) return false;

        String sql =
                "UPDATE schedules SET bus_id=?, route_id=?, departure_time=?, arrival_time=? WHERE id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, busId);
            ps.setInt(2, routeId);
            ps.setTimestamp(3, departure);
            ps.setTimestamp(4, arrival);
            ps.setInt(5, id);
            setLastErrorMessage("");
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                new SearchDAO().clearSearchCache();
            }
            return updated;
        } catch (Exception e) {
            setLastErrorMessage(DBConnectionUtil.userMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    public boolean setScheduleStatus(int id, String status) {
        setLastErrorMessage("");

        String statusCol = statusCol();
        if (id <= 0) {
            setLastErrorMessage("Select a valid schedule.");
            return false;
        }
        if (statusCol == null) {
            setLastErrorMessage("Schedule status is not supported in this database.");
            return false;
        }

        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"CANCELLED".equals(normalized)) {
            setLastErrorMessage("Invalid schedule status.");
            return false;
        }

        String sql = "UPDATE schedules SET " + statusCol + "=? WHERE id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, normalized);
            ps.setInt(2, id);
            int updated = ps.executeUpdate();
            if (updated <= 0) {
                setLastErrorMessage("Schedule not found.");
                return false;
            }
            new SearchDAO().clearSearchCache();
            return true;
        } catch (Exception e) {
            setLastErrorMessage(DBConnectionUtil.userMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    public boolean deleteSchedule(int id) {
        String sql = "DELETE FROM schedules WHERE id=?";
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            boolean deleted = ps.executeUpdate() > 0;
            if (deleted) {
                new SearchDAO().clearSearchCache();
            }
            return deleted;
        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    public List<String[]> searchSchedule(String keyword) {
        List<String[]> list = new ArrayList<>();
        String routeExpr = routeLabelExpr();
        String statusExpr = statusCol() == null ? "'ACTIVE'" : "s." + statusCol();

        String sql =
                "SELECT s.id, b.operator, " + routeExpr + " AS route_name, s.departure_time, s.arrival_time, " +
                statusExpr + " AS status_value " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id=b.id " +
                "JOIN routes r ON s.route_id=r.id " +
                "LEFT JOIN cities c1 ON c1.id = r.source_city_id " +
                "LEFT JOIN cities c2 ON c2.id = r.destination_city_id " +
                "WHERE s.departure_time >= CURRENT_TIMESTAMP " +
                "AND (b.operator LIKE ? OR " + routeExpr + " LIKE ?) " +
                "ORDER BY s.departure_time ASC";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String q = "%" + (keyword == null ? "" : keyword) + "%";
            ps.setString(1, q);
            ps.setString(2, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(buildRow(rs));
            }
        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
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
