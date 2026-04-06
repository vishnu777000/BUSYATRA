package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class NewsDAO {

    private static final Object SCHEMA_LOCK = new Object();
    private static volatile boolean adminNewsChecked = false;

    private void ensureAdminNewsTable() {
        if (adminNewsChecked) return;

        synchronized (SCHEMA_LOCK) {
            if (adminNewsChecked) return;

            String sql =
                    "CREATE TABLE IF NOT EXISTS admin_news (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "message TEXT NOT NULL," +
                    "active TINYINT(1) NOT NULL DEFAULT 1," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

            try (
                    Connection con = DBConnectionUtil.getConnection();
                    PreparedStatement ps = con.prepareStatement(sql)
            ) {
                ps.executeUpdate();
                adminNewsChecked = true;
            } catch (Exception ignored) {

            }
        }
    }

    

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

    

    public List<String[]> getActiveNews() {

        List<String[]> list = new ArrayList<>();
        ensureAdminNewsTable();

        String sql =
                "SELECT id,message,created_at " +
                "FROM admin_news " +
                "WHERE active=1 " +
                "ORDER BY created_at DESC LIMIT 50";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                list.add(buildActiveRow(rs));
            }

        } catch (Exception e) {
            
        }

        return list;
    }

    

    public List<String[]> getAllNews() {

        List<String[]> list = new ArrayList<>();
        ensureAdminNewsTable();

        String sql =
                "SELECT id,message,active,created_at " +
                "FROM admin_news " +
                "ORDER BY created_at DESC LIMIT 500";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                list.add(buildAdminRow(rs));
            }

        } catch (Exception e) {
            
        }

        return list;
    }

    

    public boolean addNews(String message) {
        ensureAdminNewsTable();

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
            
        }

        return false;
    }

    

    public boolean setNewsStatus(int id, boolean active) {
        ensureAdminNewsTable();

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
            
        }

        return false;
    }

    

    public boolean deleteNews(int id) {
        ensureAdminNewsTable();

        String sql =
                "DELETE FROM admin_news WHERE id=?";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            
        }

        return false;
    }

    

    public String getLatestNews() {
        ensureAdminNewsTable();

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
            
        }

        return "No announcements";
    }
}
