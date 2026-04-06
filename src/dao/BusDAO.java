package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BusDAO {

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
                
            }
        }
        return null;
    }

    private String col(String key, Resolver resolver) {
        if (COL_CACHE.containsKey(key)) {
            String c = COL_CACHE.get(key);
            return NULL_MARKER.equals(c) ? null : c;
        }
        String r = resolver.resolve();
        COL_CACHE.put(key, r == null ? NULL_MARKER : r);
        return r;
    }

    private String seatsCol() {
        return col("buses.seats", () -> firstColumn("buses", "total_seats", "seat_count", "seats"));
    }

    private String fareCol() {
        return col("buses.fare", () -> firstColumn("buses", "fare", "fare_multiplier", "multiplier"));
    }

    private String statusCol() {
        return col("buses.status", () -> firstColumn("buses", "status", "bus_status"));
    }

    private String typeCol() {
        return col("buses.type", () -> firstColumn("buses", "bus_type", "type"));
    }

    private String[] buildBusRow(ResultSet rs) throws Exception {
        return new String[]{
                rs.getString("id"),
                rs.getString("operator"),
                rs.getString("bus_type"),
                rs.getString("total_seats"),
                rs.getString("fare"),
                rs.getString("status")
        };
    }

    public List<String[]> getAllBuses() {
        List<String[]> buses = new ArrayList<>();

        String seats = seatsCol();
        String fare = fareCol();
        String status = statusCol();
        String type = typeCol();

        if (seats == null || fare == null || type == null) return buses;

        String sql =
                "SELECT id, operator, " + type + " AS bus_type, " + seats + " AS total_seats, " +
                fare + " AS fare, " + (status == null ? "'ACTIVE'" : status) + " AS status " +
                "FROM buses ORDER BY id DESC";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                buses.add(buildBusRow(rs));
            }
        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
        }

        return buses;
    }

    public boolean addBus(String operator, String typeValue, int totalSeats, double fareValue) {
        String seats = seatsCol();
        String fare = fareCol();
        String status = statusCol();
        String type = typeCol();
        if (seats == null || fare == null || type == null) return false;

        String sql;
        if (status == null) {
            sql = "INSERT INTO buses (operator, " + type + ", " + seats + ", " + fare + ") VALUES (?,?,?,?)";
        } else {
            sql = "INSERT INTO buses (operator, " + type + ", " + seats + ", " + fare + ", " + status + ") VALUES (?,?,?,?,?)";
        }

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, operator);
            ps.setString(2, typeValue);
            ps.setInt(3, totalSeats);
            ps.setDouble(4, fareValue);
            if (status != null) ps.setString(5, "ACTIVE");
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    public boolean updateBus(int id, String operator, String typeValue, int totalSeats, double fareValue) {
        String seats = seatsCol();
        String fare = fareCol();
        String type = typeCol();
        if (seats == null || fare == null || type == null) return false;

        String sql =
                "UPDATE buses SET operator=?, " + type + "=?, " + seats + "=?, " + fare + "=? WHERE id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, operator);
            ps.setString(2, typeValue);
            ps.setInt(3, totalSeats);
            ps.setDouble(4, fareValue);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    public boolean setBusStatus(int id, String statusValue) {
        String status = statusCol();
        if (status == null) return true; 

        String sql = "UPDATE buses SET " + status + "=? WHERE id=?";
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, statusValue);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    public boolean deleteBus(int busId) {
        String sql = "DELETE FROM buses WHERE id=?";
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, busId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    public int getTotalBuses() {
        String sql = "SELECT COUNT(*) FROM buses";
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public String[] getBusById(int id) {
        String seats = seatsCol();
        String fare = fareCol();
        String status = statusCol();
        String type = typeCol();
        if (seats == null || fare == null || type == null) return null;

        String sql =
                "SELECT operator, " + type + " AS bus_type, " + seats + " AS total_seats, " +
                fare + " AS fare, " + (status == null ? "'ACTIVE'" : status) + " AS status " +
                "FROM buses WHERE id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            rs.getString("operator"),
                            rs.getString("bus_type"),
                            rs.getString("total_seats"),
                            rs.getString("fare"),
                            rs.getString("status")
                    };
                }
            }
        } catch (Exception e) {
            DBConnectionUtil.logIfUnexpected(e);
        }
        return null;
    }

    @FunctionalInterface
    private interface Resolver {
        String resolve();
    }
}
