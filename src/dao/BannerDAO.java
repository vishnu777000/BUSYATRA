package dao;

import config.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BannerDAO {

    /* ================= ADD BANNER ================= */

    public boolean addBanner(String title, String path) {

        String sql =
                "INSERT INTO banners (title, image_path, active) VALUES (?, ?, 0)";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, title);
            ps.setString(2, path);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= GENERIC ROW ================= */

    private String[] buildRow(ResultSet rs, boolean includeActive) throws Exception {

        if (includeActive) {
            return new String[]{
                    rs.getString("id"),
                    rs.getString("title"),
                    rs.getString("image_path"),
                    rs.getInt("active") == 1 ? "YES" : "NO"
            };
        } else {
            return new String[]{
                    rs.getString("id"),
                    rs.getString("title"),
                    rs.getString("image_path")
            };
        }
    }

    /* ================= GET ALL ================= */

    public List<String[]> getAllBanners() {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT id, title, image_path, active FROM banners ORDER BY id DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(buildRow(rs, true));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= ACTIVE ================= */

    public List<String[]> getActiveBanners() {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT id, title, image_path FROM banners WHERE active=1";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(buildRow(rs, false));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ================= SET ACTIVE ================= */

    public boolean setActiveBanner(int id) {

        String disableSql = "UPDATE banners SET active=0";
        String enableSql  = "UPDATE banners SET active=1 WHERE id=?";

        try (Connection con = DBConfig.getConnection()) {

            con.setAutoCommit(false);

            try (
                    PreparedStatement disable = con.prepareStatement(disableSql);
                    PreparedStatement enable = con.prepareStatement(enableSql)
            ) {

                disable.executeUpdate();

                enable.setInt(1, id);
                int updated = enable.executeUpdate();

                con.commit();

                return updated > 0;

            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= DELETE ================= */

    public boolean deleteBanner(int id) {

        String sql = "DELETE FROM banners WHERE id=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= HOMEPAGE ================= */

    public List<String> getHomepageBannerImages() {

        List<String> list = new ArrayList<>();

        String sql =
                "SELECT image_path FROM banners WHERE active=1 ORDER BY id DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(rs.getString("image_path"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}