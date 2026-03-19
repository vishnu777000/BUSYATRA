package dao;

import util.DBConnectionUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDAO {

    /* ================= HELPERS ================= */

    private String[] buildRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("operator"),
                rs.getString("route_name"),
                rs.getString("departure_time"),
                rs.getString("arrival_time")
        };
    }

    private int getInt(String sql) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /* ================= ALL ================= */

    public List<String[]> getAllSchedules() {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT s.id, b.operator, r.route_name, " +
                "s.departure_time, s.arrival_time " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "ORDER BY s.departure_time DESC";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                list.add(buildRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= DETAILS ================= */

    public String[] getScheduleDetails(int id) {

        String sql =
                "SELECT s.id, b.operator, b.bus_type, b.driver_phone, " +
                "s.departure_time, s.arrival_time, r.route_name " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "WHERE s.id=?";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

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

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /* ================= ADD ================= */

    public boolean addSchedule(int busId,
                               int routeId,
                               Timestamp departure,
                               Timestamp arrival) {

        // 🔥 Validation
        if (departure.after(arrival)) return false;

        String sql =
                "INSERT INTO schedules " +
                "(bus_id, route_id, departure_time, arrival_time, status) " +
                "VALUES (?,?,?,?, 'ACTIVE')";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, busId);
            ps.setInt(2, routeId);
            ps.setTimestamp(3, departure);
            ps.setTimestamp(4, arrival);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= UPDATE ================= */

    public boolean updateSchedule(int id,
                                  int busId,
                                  int routeId,
                                  Timestamp departure,
                                  Timestamp arrival) {

        if (departure.after(arrival)) return false;

        String sql =
                "UPDATE schedules " +
                "SET bus_id=?,route_id=?,departure_time=?,arrival_time=? " +
                "WHERE id=?";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, busId);
            ps.setInt(2, routeId);
            ps.setTimestamp(3, departure);
            ps.setTimestamp(4, arrival);
            ps.setInt(5, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= DELETE ================= */

    public boolean deleteSchedule(int id) {

        String sql = "DELETE FROM schedules WHERE id=?";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= SEARCH ================= */

    public List<String[]> searchSchedule(String keyword) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT s.id,b.operator,r.route_name,s.departure_time,s.arrival_time " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id=b.id " +
                "JOIN routes r ON s.route_id=r.id " +
                "WHERE b.operator LIKE ? OR r.route_name LIKE ? " +
                "ORDER BY s.departure_time DESC";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            String q = "%" + keyword + "%";

            ps.setString(1, q);
            ps.setString(2, q);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(buildRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= COUNT ================= */

    public int getScheduleCount() {

        return getInt("SELECT COUNT(*) FROM schedules");
    }
}