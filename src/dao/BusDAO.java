package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BusDAO {

    /* ================= GENERIC HELPERS ================= */

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

    private int getInt(String sql) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /* ================= ALL BUSES ================= */

    public List<String[]> getAllBuses() {

        List<String[]> buses = new ArrayList<>();

        String sql =
                "SELECT id,operator,bus_type,total_seats,fare,status " +
                "FROM buses ORDER BY id DESC";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                buses.add(buildBusRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return buses;
    }

    /* ================= ADD BUS ================= */

    public boolean addBus(String operator, String type, int totalSeats, double fare) {

        String sql =
                "INSERT INTO buses (operator, bus_type, total_seats, fare, status) " +
                "VALUES (?,?,?,?,?)";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, operator);
            ps.setString(2, type);
            ps.setInt(3, totalSeats);
            ps.setDouble(4, fare);
            ps.setString(5, "ACTIVE");

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= UPDATE BUS ================= */

    public boolean updateBus(int id, String operator, String type, int totalSeats, double fare) {

        String sql =
                "UPDATE buses SET operator=?,bus_type=?,total_seats=?,fare=? WHERE id=?";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, operator);
            ps.setString(2, type);
            ps.setInt(3, totalSeats);
            ps.setDouble(4, fare);
            ps.setInt(5, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= CHANGE STATUS ================= */

    public boolean setBusStatus(int id, String status) {

        String sql = "UPDATE buses SET status=? WHERE id=?";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, status);
            ps.setInt(2, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= DELETE BUS ================= */

    public boolean deleteBus(int busId) {

        String sql = "DELETE FROM buses WHERE id=?";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, busId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= BUS COUNT ================= */

    public int getTotalBuses() {
        return getInt("SELECT COUNT(*) FROM buses");
    }

    /* ================= GET BUS BY ID ================= */

    public String[] getBusById(int id) {

        String sql =
                "SELECT operator,bus_type,total_seats,fare,status FROM buses WHERE id=?";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                return new String[]{
                        rs.getString("operator"),
                        rs.getString("bus_type"),
                        rs.getString("total_seats"),
                        rs.getString("fare"),
                        rs.getString("status")
                };
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}