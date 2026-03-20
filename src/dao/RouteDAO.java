package dao;

import config.DBConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteDAO {

    private final Map<String, Boolean> tableCache = new HashMap<>();
    private final Map<String, Boolean> columnCache = new HashMap<>();

    /* ================= HELPERS ================= */

    private String routeMapCol() {
        return columnExists("routes", "route_map") ? "route_map"
                : (columnExists("routes", "map_path") ? "map_path"
                : (columnExists("routes", "map_image") ? "map_image"
                : (columnExists("routes", "route_image") ? "route_image" : null)));
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

    private boolean tableExists(String tableName) {
        if (tableCache.containsKey(tableName)) {
            return tableCache.get(tableName);
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
                tableCache.put(tableName, exists);
                return exists;
            }
        } catch (Exception e) {
            tableCache.put(tableName, false);
            return false;
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        String key = tableName + "." + columnName;
        if (columnCache.containsKey(key)) {
            return columnCache.get(key);
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
                columnCache.put(key, exists);
                return exists;
            }
        } catch (Exception e) {
            columnCache.put(key, false);
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
            e.printStackTrace();
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
            e.printStackTrace();
        }

        return -1;
    }

    /* ================= ROUTES ================= */

    public List<String[]> getAllRoutes() {

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
            e.printStackTrace();
        }

        return list;
    }

    public boolean addRouteMaster(String name, int distance, double rate) {

        String sql =
                "INSERT INTO routes (route_name,total_distance,base_fare,status) " +
                "VALUES (?,?,?,'ACTIVE')";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, name.toUpperCase());
            ps.setInt(2, distance);
            ps.setDouble(3, rate);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateRoute(int id, String name, int distance, double rate) {

        String sql =
                "UPDATE routes SET route_name=?,total_distance=?,base_fare=? WHERE id=?";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, name);
            ps.setInt(2, distance);
            ps.setDouble(3, rate);
            ps.setInt(4, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteRoute(int id) {

        String sql = "DELETE FROM routes WHERE id=?";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= ROUTE MAP ================= */

    public String getRouteMap(int routeId) {

        String mapCol = routeMapCol();
        if (mapCol == null) {
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
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateRouteMap(int routeId, String fileName) {

        String mapCol = routeMapCol();
        if (mapCol == null) {
            return false;
        }

        String sql = "UPDATE routes SET " + mapCol + "=? WHERE id=?";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, fileName);
            ps.setInt(2, routeId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= STOPS ================= */

    public List<String[]> getStopsByRoute(int routeId) {

        List<String[]> list = new ArrayList<>();

        if (!tableExists("route_stops")) {
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
                e.printStackTrace();
            }
            return list;
        }

        if (stopCol == null || orderCol == null || distanceCol == null) {
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
            e.printStackTrace();
        }

        return list;
    }

    public boolean addStop(int routeId, String stop, int order, int distance) {

        String sql =
                "INSERT INTO route_stops (route_id,stop_name,stop_order,distance_from_start) " +
                "VALUES (?,?,?,?)";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, routeId);
            ps.setString(2, stop);
            ps.setInt(3, order);
            ps.setInt(4, distance);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= INTERNAL FETCH ================= */

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

    /* ================= DYNAMIC FARE ================= */

    public double calculateFare(int routeId, String from, String to) {

        if (from == null || to == null) return 0;
        if (from.equalsIgnoreCase(to)) return 0;

        int fromDist = getDistance(routeId, from);
        int toDist   = getDistance(routeId, to);

        if (fromDist == -1 || toDist == -1) return 0;

        double rate = getRatePerKm(routeId);

        int distance = Math.abs(toDist - fromDist);

        return Math.max(0, Math.round(distance * rate * 100.0) / 100.0);
    }
}
