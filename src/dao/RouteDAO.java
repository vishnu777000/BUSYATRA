package dao;

import config.DBConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RouteDAO {

    /* ================= HELPERS ================= */

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
                rs.getString("stop_name"),
                rs.getString("stop_order"),
                rs.getString("distance_from_start")
        };
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

        String sql =
                "SELECT id, route_name, total_distance, base_fare, route_map " +
                "FROM routes ORDER BY id DESC";

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

        String sql = "SELECT route_map FROM routes WHERE id=?";

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

        String sql = "UPDATE routes SET route_map=? WHERE id=?";

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

        String sql =
                "SELECT stop_name,stop_order,distance_from_start " +
                "FROM route_stops WHERE route_id=? ORDER BY stop_order";

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

        return getInt(
                "SELECT distance_from_start FROM route_stops WHERE route_id=? AND stop_name=?",
                routeId, stop
        );
    }

    public double getRatePerKm(int routeId) {

        return getDouble(
                "SELECT base_fare FROM routes WHERE id=?",
                routeId
        );
    }

    /* ================= DYNAMIC FARE ================= */

    public double calculateFare(int routeId, String from, String to) {

        if (from.equalsIgnoreCase(to)) return 0;

        int fromDist = getDistance(routeId, from);
        int toDist   = getDistance(routeId, to);

        if (fromDist == -1 || toDist == -1) return 0;

        double rate = getRatePerKm(routeId);

        int distance = Math.abs(toDist - fromDist);

        return Math.round(distance * rate * 100.0) / 100.0;
    }
}