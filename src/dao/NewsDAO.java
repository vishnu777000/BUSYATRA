package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class NewsDAO {

    /* ================= HELPERS ================= */

    private String[] buildActiveRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("message") != null ? rs.getString("message") : "-",
                rs.getString("created_at")
        };
    }

    private String[] buildAdminRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("id"),
                rs.getString("message") != null ? rs.getString("message") : "-",
                rs.getInt("active") == 1 ? "ACTIVE" : "INACTIVE",
                rs.getString("created_at")
        };
    }

    /* ================= ACTIVE ================= */

    public List<String[]> getActiveNews() {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT id,message,created_at " +
                "FROM admin_news " +
                "WHERE active=1 " +
                "ORDER BY created_at DESC";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                list.add(buildActiveRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= ALL ================= */

    public List<String[]> getAllNews() {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT id,message,active,created_at " +
                "FROM admin_news " +
                "ORDER BY created_at DESC";

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

    /* ================= ADD ================= */

    public boolean addNews(String message) {

        String sql =
                "INSERT INTO admin_news (message, active, created_at) " +
                "VALUES (?,1,NOW())";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, message);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= STATUS ================= */

    public boolean setNewsStatus(int id, boolean active) {

        String sql =
                "UPDATE admin_news SET active=? WHERE id=?";

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

    public boolean deleteNews(int id) {

        String sql =
                "DELETE FROM admin_news WHERE id=?";

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

    /* ================= LATEST ================= */

    public String getLatestNews() {

        String sql =
                "SELECT message FROM admin_news " +
                "WHERE active=1 " +
                "ORDER BY created_at DESC LIMIT 1";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getString("message");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "No announcements";
    }
}