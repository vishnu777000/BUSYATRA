package dao;

import config.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ReportsDAO {

    /* ================= HELPER ================= */

    private String[] buildRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("source"),
                rs.getString("destination"),
                rs.getString("amount"),
                rs.getString("status"),
                rs.getString("booking_time")
        };
    }

    private double getDouble(String sql) {

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) return rs.getDouble(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /* ================= ALL BOOKINGS ================= */

    public List<String[]> getAllBookings() {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT " +
                " b.id, " +
                " u.name, " +
                " b.from_city AS source, " +
                " b.to_city AS destination, " +
                " b.amount, " +
                " b.status, " +
                " b.created_at AS booking_time " +
                "FROM bookings b " +
                "JOIN users u ON b.user_id = u.id " +
                "ORDER BY b.id DESC";

        try (
                Connection con = DBConfig.getConnection();
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

    /* ================= SEARCH BOOKINGS ================= */

    public List<String[]> searchBooking(String query) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT " +
                " b.id, " +
                " u.name, " +
                " b.from_city AS source, " +
                " b.to_city AS destination, " +
                " b.amount, " +
                " b.status, " +
                " b.created_at AS booking_time " +
                "FROM bookings b " +
                "JOIN users u ON b.user_id = u.id " +
                "WHERE b.id LIKE ? OR u.name LIKE ? " +
                "ORDER BY b.id DESC";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            String q = "%" + query + "%";

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

    /* ================= BOOKINGS BY DATE ================= */

    public List<String[]> getBookingsByDate(String startDate, String endDate) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT " +
                " b.id, " +
                " u.name, " +
                " b.from_city AS source, " +
                " b.to_city AS destination, " +
                " b.amount, " +
                " b.status, " +
                " b.created_at AS booking_time " +
                "FROM bookings b " +
                "JOIN users u ON b.user_id = u.id " +
                "WHERE DATE(b.created_at) BETWEEN ? AND ? " +
                "ORDER BY b.created_at DESC";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, startDate);
            ps.setString(2, endDate);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(buildRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= TOTAL REVENUE ================= */

    public double getTotalRevenue() {

        String sql =
                "SELECT IFNULL(SUM(amount),0) " +
                "FROM bookings WHERE status='CONFIRMED'";

        return getDouble(sql);
    }
}