package dao;

import config.DBConfig;
import util.FareCalculator;
import util.DBConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class RouteDAO {

    private static final Map<String, Boolean> TABLE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> COLUMN_CACHE = new ConcurrentHashMap<>();
    private final AtomicReference<String> lastError = new AtomicReference<>();
    private final ThreadLocal<String> schemaLookupError = new ThreadLocal<>();

    public String getLastError() {
        String err = lastError.get();
        return (err == null || err.isBlank()) ? "Route operation failed." : err;
    }

    public boolean hasLastError() {
        String err = lastError.get();
        return err != null && !err.isBlank();
    }

    private void setLastError(String message) {
        lastError.set(message);
    }

    private void resetSchemaLookupError() {
        schemaLookupError.remove();
    }

    private void markSchemaLookupFailure(Exception e) {
        if (schemaLookupError.get() == null) {
            schemaLookupError.set(DBConfig.userFriendlyMessage(e));
        }
    }

    private String getSchemaLookupError() {
        return schemaLookupError.get();
    }

    

    private String routeMapCol() {
        return columnExists("routes", "route_map") ? "route_map"
                : (columnExists("routes", "map_path") ? "map_path"
                : (columnExists("routes", "map_image") ? "map_image"
                : (columnExists("routes", "route_image") ? "route_image" : null)));
    }

    private String routeNameCol() {
        return columnExists("routes", "route_name") ? "route_name"
                : (columnExists("routes", "name") ? "name" : null);
    }

    private String totalDistanceCol() {
        return columnExists("routes", "total_distance") ? "total_distance"
                : (columnExists("routes", "distance_km") ? "distance_km"
                : (columnExists("routes", "distance") ? "distance" : null));
    }

    private String baseFareCol() {
        return columnExists("routes", "base_fare") ? "base_fare"
                : (columnExists("routes", "rate_per_km") ? "rate_per_km"
                : (columnExists("routes", "fare_per_km") ? "fare_per_km" : null));
    }

    private String routeStatusCol() {
        return columnExists("routes", "status") ? "status"
                : (columnExists("routes", "route_status") ? "route_status" : null);
    }

    private String[] buildRouteRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("route_name"),
                rs.getString("total_distance"),
                rs.getString("base_fare"),
                rs.getString("route_map")
        };
    }

    private String[] buildStopRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("stop"),
                rs.getString("ord"),
                rs.getString("dist")
        };
    }

    private String[] parseRouteEndpoints(String routeName) {
        if (routeName == null || routeName.isBlank()) {
            return null;
        }

        String normalized = routeName
                .replace("â†’", "->")
                .replace("→", "->")
                .trim();

        String[] parts = normalized.split("\\s*->\\s*");
        if (parts.length < 2) {
            return null;
        }

        String source = parts[0].trim();
        String destination = parts[parts.length - 1].trim();
        if (source.isBlank() || destination.isBlank()) {
            return null;
        }
        return new String[]{source, destination};
    }

    private boolean tableExists(String tableName) {
        if (TABLE_CACHE.containsKey(tableName)) {
            return TABLE_CACHE.get(tableName);
        }
        String sql =
                "SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ? LIMIT 1";
        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                TABLE_CACHE.put(tableName, exists);
                return exists;
            }
        } catch (Exception e) {
            markSchemaLookupFailure(e);
            return false;
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        String key = tableName + "." + columnName;
        if (COLUMN_CACHE.containsKey(key)) {
            return COLUMN_CACHE.get(key);
        }
        String sql =
                "SELECT 1 FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";
        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                COLUMN_CACHE.put(key, exists);
                return exists;
            }
        } catch (Exception e) {
            markSchemaLookupFailure(e);
            return false;
        }
    }

    private double getDouble(String sql, int id) {

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getDouble(1);

        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
        }

        return 0;
    }

    private int getInt(String sql, int routeId, String stop) {

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, routeId);
            ps.setString(2, stop);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
        }

        return -1;
    }

    

    public List<String[]> getAllRoutes() {
        setLastError(null);
        resetSchemaLookupError();

        List<String[]> list = new ArrayList<>();

        String mapCol = routeMapCol();
        String mapExpr = mapCol != null ? mapCol : "NULL";
        String sql;
        if (columnExists("routes", "route_name")) {
            sql = "SELECT id, route_name, total_distance, base_fare, " + mapExpr + " AS route_map FROM routes ORDER BY id DESC";
        } else {
            sql =
                    "SELECT r.id, " +
                    "CONCAT(c1.name,' -> ',c2.name) AS route_name, " +
                    "COALESCE((SELECT MAX(distance_from_start) FROM route_stops rs WHERE rs.route_id=r.id),0) AS total_distance, " +
                    "r.base_fare, " + (mapCol != null ? "r." + mapCol : "NULL") + " AS route_map " +
                    "FROM routes r " +
                    "LEFT JOIN cities c1 ON c1.id = r.source_city_id " +
                    "LEFT JOIN cities c2 ON c2.id = r.destination_city_id " +
                    "ORDER BY r.id DESC";
        }

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                list.add(buildRouteRow(rs));
            }

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return list;
    }

    public boolean addRouteMaster(String name, int distance, double rate) {
        setLastError(null);

        if (name == null || name.isBlank()) {
            setLastError("Route name is required.");
            return false;
        }
        if (distance < 0) {
            setLastError("Distance must be zero or greater.");
            return false;
        }
        if (rate < 0) {
            setLastError("Rate per KM must be zero or greater.");
            return false;
        }

        String[] endpoints = parseRouteEndpoints(name);
        boolean hasSourceCity = columnExists("routes", "source_city_id");
        boolean hasDestinationCity = columnExists("routes", "destination_city_id");
        Integer sourceCityId = hasSourceCity && endpoints != null ? findCityId(endpoints[0]) : null;
        Integer destinationCityId = hasDestinationCity && endpoints != null ? findCityId(endpoints[1]) : null;

        if ((hasSourceCity || hasDestinationCity) && endpoints == null) {
            setLastError("Use route format like 'City A -> City B'.");
            return false;
        }
        if (hasSourceCity && sourceCityId == null) {
            setLastError("Source city was not found.");
            return false;
        }
        if (hasDestinationCity && destinationCityId == null) {
            setLastError("Destination city was not found.");
            return false;
        }

        String routeNameCol = routeNameCol();
        String totalDistanceCol = totalDistanceCol();
        String baseFareCol = baseFareCol();
        String statusCol = routeStatusCol();

        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (routeNameCol != null) {
            columns.add(routeNameCol);
            values.add(name.toUpperCase());
        }
        if (totalDistanceCol != null) {
            columns.add(totalDistanceCol);
            values.add(distance);
        }
        if (baseFareCol != null) {
            columns.add(baseFareCol);
            values.add(rate);
        }
        if (statusCol != null) {
            columns.add(statusCol);
            values.add("ACTIVE");
        }
        if (hasSourceCity) {
            columns.add("source_city_id");
            values.add(sourceCityId);
        }
        if (hasDestinationCity) {
            columns.add("destination_city_id");
            values.add(destinationCityId);
        }

        if (columns.isEmpty()) {
            setLastError("Routes table does not have writable route columns.");
            return false;
        }

        StringBuilder sql = new StringBuilder("INSERT INTO routes (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append(columns.get(i));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())
        ) {

            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                if (value instanceof Integer) {
                    ps.setInt(i + 1, (Integer) value);
                } else if (value instanceof Double) {
                    ps.setDouble(i + 1, (Double) value);
                } else {
                    ps.setString(i + 1, String.valueOf(value));
                }
            }

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    public boolean updateRoute(int id, String name, int distance, double rate) {
        setLastError(null);

        if (id <= 0) {
            setLastError("Select a valid route.");
            return false;
        }
        if (name == null || name.isBlank()) {
            setLastError("Route name is required.");
            return false;
        }
        if (distance < 0) {
            setLastError("Distance must be zero or greater.");
            return false;
        }
        if (rate < 0) {
            setLastError("Rate per KM must be zero or greater.");
            return false;
        }

        String[] endpoints = parseRouteEndpoints(name);
        boolean hasSourceCity = columnExists("routes", "source_city_id");
        boolean hasDestinationCity = columnExists("routes", "destination_city_id");
        Integer sourceCityId = hasSourceCity && endpoints != null ? findCityId(endpoints[0]) : null;
        Integer destinationCityId = hasDestinationCity && endpoints != null ? findCityId(endpoints[1]) : null;

        if ((hasSourceCity || hasDestinationCity) && endpoints == null) {
            setLastError("Use route format like 'City A -> City B'.");
            return false;
        }
        if (hasSourceCity && sourceCityId == null) {
            setLastError("Source city was not found.");
            return false;
        }
        if (hasDestinationCity && destinationCityId == null) {
            setLastError("Destination city was not found.");
            return false;
        }

        String routeNameCol = routeNameCol();
        String totalDistanceCol = totalDistanceCol();
        String baseFareCol = baseFareCol();

        List<String> assignments = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (routeNameCol != null) {
            assignments.add(routeNameCol + "=?");
            values.add(name);
        }
        if (totalDistanceCol != null) {
            assignments.add(totalDistanceCol + "=?");
            values.add(distance);
        }
        if (baseFareCol != null) {
            assignments.add(baseFareCol + "=?");
            values.add(rate);
        }
        if (hasSourceCity) {
            assignments.add("source_city_id=?");
            values.add(sourceCityId);
        }
        if (hasDestinationCity) {
            assignments.add("destination_city_id=?");
            values.add(destinationCityId);
        }

        if (assignments.isEmpty()) {
            setLastError("Routes table does not have writable route columns.");
            return false;
        }

        StringBuilder sql = new StringBuilder("UPDATE routes SET ");
        for (int i = 0; i < assignments.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append(assignments.get(i));
        }
        sql.append(" WHERE id=?");

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())
        ) {

            int idx = 1;
            for (Object value : values) {
                if (value instanceof Integer) {
                    ps.setInt(idx++, (Integer) value);
                } else if (value instanceof Double) {
                    ps.setDouble(idx++, (Double) value);
                } else {
                    ps.setString(idx++, String.valueOf(value));
                }
            }
            ps.setInt(idx, id);

            int updated = ps.executeUpdate();
            if (updated > 0) {
                return true;
            }

            setLastError("Selected route was not found.");
            return false;

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    public boolean deleteRoute(int id) {
        setLastError(null);

        if (id <= 0) {
            setLastError("Select a valid route.");
            return false;
        }

        String sql = "DELETE FROM routes WHERE id=?";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                return true;
            }

            setLastError("Selected route was not found.");
            return false;

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    

    public String getRouteMap(int routeId) {
        setLastError(null);
        resetSchemaLookupError();

        String mapCol = routeMapCol();
        if (mapCol == null) {
            if (getSchemaLookupError() != null) {
                setLastError(getSchemaLookupError());
            } else {
                setLastError("Routes table is missing a route map column.");
            }
            return null;
        }

        String sql = "SELECT " + mapCol + " AS route_map FROM routes WHERE id=?";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, routeId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getString("route_map");

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return null;
    }

    public boolean updateRouteMap(int routeId, String fileName) {
        setLastError(null);
        resetSchemaLookupError();

        if (routeId <= 0) {
            setLastError("Select a valid route.");
            return false;
        }
        if (fileName == null || fileName.isBlank()) {
            setLastError("Select a valid map image.");
            return false;
        }

        String mapCol = routeMapCol();
        if (mapCol == null) {
            if (getSchemaLookupError() != null) {
                setLastError(getSchemaLookupError());
            } else {
                setLastError("Routes table is missing a map column (route_map, map_path, map_image, or route_image).");
            }
            return false;
        }

        String sql = "UPDATE routes SET " + mapCol + "=? WHERE id=?";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, fileName.trim().replace("\\", "/"));
            ps.setInt(2, routeId);

            int updated = ps.executeUpdate();
            if (updated > 0) {
                return true;
            }

            setLastError("Selected route was not found.");
            return false;

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    

    public List<String[]> getStopsByRoute(int routeId) {
        setLastError(null);
        resetSchemaLookupError();

        List<String[]> list = new ArrayList<>();

        if (!tableExists("route_stops")) {
            if (getSchemaLookupError() != null) {
                setLastError(getSchemaLookupError());
            }
            return list;
        }

        String stopCol = columnExists("route_stops", "stop_name") ? "stop_name" :
                (columnExists("route_stops", "stop") ? "stop" :
                        (columnExists("route_stops", "city_name") ? "city_name" : null));
        String orderCol = columnExists("route_stops", "stop_order") ? "stop_order" :
                (columnExists("route_stops", "stop_no") ? "stop_no" :
                        (columnExists("route_stops", "seq_no") ? "seq_no" : null));
        String distanceCol = columnExists("route_stops", "distance_from_start") ? "distance_from_start" :
                (columnExists("route_stops", "distance_km") ? "distance_km" :
                        (columnExists("route_stops", "distance") ? "distance" : null));

        if (stopCol == null && columnExists("route_stops", "city_id")) {
            String sql =
                    "SELECT c.name AS stop, rs.stop_order AS ord, rs.distance_from_start AS dist " +
                    "FROM route_stops rs " +
                    "LEFT JOIN cities c ON c.id = rs.city_id " +
                    "WHERE rs.route_id=? ORDER BY rs.stop_order";

            try (
                    Connection con = DBConfig.getConnection();
                    PreparedStatement ps = con.prepareStatement(sql)
            ) {
                ps.setInt(1, routeId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(buildStopRow(rs));
                }
            } catch (Exception e) {
                setLastError(DBConfig.userFriendlyMessage(e));
                DBConnectionUtil.logIfUnexpected(e);
            }
            return list;
        }

        if (stopCol == null || orderCol == null || distanceCol == null) {
            setLastError("Route stops table is missing required stop, order, or distance columns.");
            return list;
        }

        String sql =
                "SELECT " + stopCol + " AS stop, " +
                orderCol + " AS ord, " +
                distanceCol + " AS dist " +
                "FROM route_stops WHERE route_id=? ORDER BY " + orderCol;

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, routeId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(buildStopRow(rs));
            }

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return list;
    }

    public boolean addStop(int routeId, String stop, int order, int distance) {
        setLastError(null);
        resetSchemaLookupError();

        if (routeId <= 0) {
            setLastError("Select a valid route.");
            return false;
        }
        if (stop == null || stop.isBlank()) {
            setLastError("Stop name is required.");
            return false;
        }
        if (order <= 0) {
            setLastError("Stop order must be greater than zero.");
            return false;
        }
        if (distance < 0) {
            setLastError("Distance must be zero or greater.");
            return false;
        }
        if (!tableExists("route_stops")) {
            setLastError(getSchemaLookupError() != null ? getSchemaLookupError() : "Route stops table is not available.");
            return false;
        }

        String orderCol = columnExists("route_stops", "stop_order") ? "stop_order" :
                (columnExists("route_stops", "stop_no") ? "stop_no" :
                        (columnExists("route_stops", "seq_no") ? "seq_no" : null));
        String distanceCol = columnExists("route_stops", "distance_from_start") ? "distance_from_start" :
                (columnExists("route_stops", "distance_km") ? "distance_km" :
                        (columnExists("route_stops", "distance") ? "distance" : null));
        String stopCol = columnExists("route_stops", "stop_name") ? "stop_name" :
                (columnExists("route_stops", "stop") ? "stop" :
                        (columnExists("route_stops", "city_name") ? "city_name" : null));
        boolean hasCityId = columnExists("route_stops", "city_id");

        if (orderCol == null || distanceCol == null || (!hasCityId && stopCol == null)) {
            setLastError("Route stops table is missing writable stop, order, or distance columns.");
            return false;
        }

        Integer cityId = null;
        if (hasCityId) {
            cityId = findCityId(stop);
            if (cityId == null) {
                setLastError("No city found named '" + stop.trim() + "' for this route stop.");
                return false;
            }
        }

        List<String> cols = new ArrayList<>();
        cols.add("route_id");
        if (hasCityId) cols.add("city_id");
        if (stopCol != null) cols.add(stopCol);
        cols.add(orderCol);
        cols.add(distanceCol);

        StringBuilder sql = new StringBuilder("INSERT INTO route_stops (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append(cols.get(i));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())
        ) {
            int idx = 1;
            ps.setInt(idx++, routeId);
            if (hasCityId) ps.setInt(idx++, cityId);
            if (stopCol != null) ps.setString(idx++, stop.trim());
            ps.setInt(idx++, order);
            ps.setInt(idx, distance);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    

    public int getDistance(int routeId, String stop) {

        if (!tableExists("route_stops")) {
            return -1;
        }

        String stopCol = columnExists("route_stops", "stop_name") ? "stop_name" :
                (columnExists("route_stops", "stop") ? "stop" :
                        (columnExists("route_stops", "city_name") ? "city_name" : null));
        String distanceCol = columnExists("route_stops", "distance_from_start") ? "distance_from_start" :
                (columnExists("route_stops", "distance_km") ? "distance_km" :
                        (columnExists("route_stops", "distance") ? "distance" : null));

        if (stopCol == null && columnExists("route_stops", "city_id")) {
            return getInt(
                    "SELECT rs.distance_from_start " +
                    "FROM route_stops rs " +
                    "JOIN cities c ON c.id = rs.city_id " +
                    "WHERE rs.route_id=? AND c.name=?",
                    routeId, stop
            );
        }

        if (stopCol == null || distanceCol == null) {
            return -1;
        }

        return getInt(
                "SELECT " + distanceCol + " FROM route_stops WHERE route_id=? AND " + stopCol + "=?",
                routeId, stop
        );
    }

    public double getRatePerKm(int routeId) {
        String rateCol = columnExists("routes", "base_fare") ? "base_fare"
                : (columnExists("routes", "fare_per_km") ? "fare_per_km"
                : (columnExists("routes", "rate_per_km") ? "rate_per_km" : null));
        if (rateCol == null) return 0;
        return getDouble("SELECT " + rateCol + " FROM routes WHERE id=?", routeId);
    }

    

    public double calculateFare(int routeId, String from, String to) {
        return calculateFare(routeId, from, to, "", 0);
    }

    public double calculateFare(int routeId, String from, String to, String busType, double busMultiplier) {

        if (from == null || to == null) return 0;
        if (from.equalsIgnoreCase(to)) return 0;

        int fromDist = getDistance(routeId, from);
        int toDist   = getDistance(routeId, to);

        if (fromDist == -1 || toDist == -1) return 0;

        double rate = getRatePerKm(routeId);

        int distance = Math.abs(toDist - fromDist);

        return FareCalculator.calculateSegmentFare(distance, rate, busType, busMultiplier);
    }

    private Integer findCityId(String stop) {
        String sql = "SELECT id FROM cities WHERE UPPER(TRIM(name)) = UPPER(TRIM(?)) LIMIT 1";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, stop == null ? "" : stop.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            setLastError(DBConfig.userFriendlyMessage(e));
            DBConnectionUtil.logIfUnexpected(e);
        }

        return null;
    }
}
