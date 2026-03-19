package dao;

import config.DBConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /* ================= HELPER ================= */

    private String[] buildUser(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("role"),
                rs.getString("status"),
                rs.getString("created_at")
        };
    }

    /* ================= LOGIN ================= */

    public String[] loginUser(String email, String password) {

        String sql =
                "SELECT id,name,email,role,status " +
                "FROM users " +
                "WHERE email=? AND password=? AND status='ACTIVE'";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new String[]{
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("status")
                };
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /* ================= EMAIL EXISTS ================= */

    public boolean emailExists(String email) {

        String sql = "SELECT 1 FROM users WHERE email=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= REGISTER ================= */

    public boolean registerUser(String name, String email, String password) {

        if (emailExists(email)) return false;

        String sql =
                "INSERT INTO users (name,email,password,role,status) " +
                "VALUES (?,?,?,'USER','ACTIVE')";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, password);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= ADD USER (ADMIN) ================= */

    public boolean addUser(String name, String email, String password, String role) {

        String sql =
                "INSERT INTO users (name,email,password,role,status) " +
                "VALUES (?,?,?,?, 'ACTIVE')";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, role);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= CHANGE PASSWORD ================= */

    public boolean changePassword(int userId, String oldPass, String newPass) {

        String check =
                "SELECT 1 FROM users WHERE id=? AND password=?";

        String update =
                "UPDATE users SET password=? WHERE id=?";

        try (Connection con = DBConfig.getConnection()) {

            PreparedStatement ps1 = con.prepareStatement(check);
            ps1.setInt(1, userId);
            ps1.setString(2, oldPass);

            ResultSet rs = ps1.executeQuery();
            if (!rs.next()) return false;

            PreparedStatement ps2 = con.prepareStatement(update);
            ps2.setString(1, newPass);
            ps2.setInt(2, userId);

            return ps2.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= RESET PASSWORD ================= */

    public boolean updatePasswordByEmail(String email, String newPass) {

        String sql =
                "UPDATE users SET password=? WHERE email=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newPass);
            ps.setString(2, email);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= GET USER ================= */

    public String[] getUserById(int userId) {

        String sql =
                "SELECT id,name,email,role,status,created_at " +
                "FROM users WHERE id=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return buildUser(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /* ================= ALL USERS ================= */

    public List<String[]> getAllUsers() {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT id,name,email,role,status,created_at " +
                "FROM users ORDER BY id DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(buildUser(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= UPDATE STATUS ================= */

    public boolean updateUserStatus(int userId, String status) {

        String sql =
                "UPDATE users SET status=? WHERE id=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= DELETE ================= */

    public boolean deleteUser(int id) {

        String sql = "DELETE FROM users WHERE id=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}