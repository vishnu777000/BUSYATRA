package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    

    private String[] buildBusReviewRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("name"),
                rs.getString("rating"),
                rs.getString("comment") != null ? rs.getString("comment") : "-",
                rs.getString("created_at")
        };
    }

    private String[] buildAdminRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("operator"),
                rs.getString("rating"),
                rs.getString("comment") != null ? rs.getString("comment") : "-",
                rs.getString("created_at")
        };
    }

    private double getDouble(String sql, int param) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, param);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getDouble(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private int getInt(String sql, int param) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, param);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    

    public boolean addReview(int userId, int busId, int rating, String comment) {

        String sql =
                "INSERT INTO reviews (user_id,bus_id,rating,comment,created_at) " +
                "VALUES (?,?,?,?,NOW())";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, userId);
            ps.setInt(2, busId);
            ps.setInt(3, rating);
            ps.setString(4, comment);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    

    public List<String[]> getReviewsByBus(int busId) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT u.name,r.rating,r.comment,r.created_at " +
                "FROM reviews r " +
                "JOIN users u ON r.user_id=u.id " +
                "WHERE r.bus_id=? " +
                "ORDER BY r.created_at DESC";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, busId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(buildBusReviewRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    

    public double getAverageRating(int busId) {

        String sql =
                "SELECT IFNULL(AVG(rating),0) FROM reviews WHERE bus_id=?";

        return getDouble(sql, busId);
    }

    

    public int getReviewCount(int busId) {

        String sql =
                "SELECT COUNT(*) FROM reviews WHERE bus_id=?";

        return getInt(sql, busId);
    }

    

    public List<String[]> getAllReviews() {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT r.id,u.name,b.operator,r.rating,r.comment,r.created_at " +
                "FROM reviews r " +
                "JOIN users u ON r.user_id=u.id " +
                "JOIN buses b ON r.bus_id=b.id " +
                "ORDER BY r.created_at DESC";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                list.add(buildAdminRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}