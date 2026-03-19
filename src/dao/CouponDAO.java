package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CouponDAO {

    /* ================= HELPERS ================= */

    private boolean exists(String sql, String code) {

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, code);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private String[] buildRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("discount_amount"),
                rs.getString("expiry_date"),
                rs.getInt("active") == 1 ? "ACTIVE" : "INACTIVE"
        };
    }

    /* ================= GET DISCOUNT ================= */

    public double getDiscountByCode(String code) {

        String sql =
                "SELECT discount_amount " +
                "FROM coupons " +
                "WHERE code=? AND active=1 " +
                "AND (expiry_date IS NULL OR expiry_date >= CURDATE())";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, code);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("discount_amount");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /* ================= VALIDATE ================= */

    public boolean isValidCoupon(String code) {

        String sql =
                "SELECT COUNT(*) FROM coupons " +
                "WHERE code=? AND active=1 " +
                "AND (expiry_date IS NULL OR expiry_date >= CURDATE())";

        return exists(sql, code);
    }

    /* ================= ADD ================= */

    public boolean addCoupon(String code,
                             double discount,
                             String expiryDate) {

        String sql =
                "INSERT INTO coupons (code, discount_amount, expiry_date, active) " +
                "VALUES (?,?,?,1)";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, code);
            ps.setDouble(2, discount);
            ps.setDate(3, java.sql.Date.valueOf(expiryDate));

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= ALL ================= */

    public List<String[]> getAllCoupons() {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT id,code,discount_amount,expiry_date,active " +
                "FROM coupons ORDER BY id DESC";

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

    /* ================= STATUS ================= */

    public boolean setCouponStatus(int id, boolean active) {

        String sql = "UPDATE coupons SET active=? WHERE id=?";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= DELETE ================= */

    public boolean deleteCoupon(int id) {

        String sql = "DELETE FROM coupons WHERE id=?";

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
}