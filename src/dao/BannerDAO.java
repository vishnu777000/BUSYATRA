package dao;

import config.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BannerDAO {

    private final Map<String, Boolean> tableCache = new HashMap<>();
    private final Map<String, Boolean> columnCache = new HashMap<>();

    private boolean tableExists(String tableName) {
        if (tableCache.containsKey(tableName)) return tableCache.get(tableName);

        String sql =
                "SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ? LIMIT 1";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                tableCache.put(tableName, exists);
                return exists;
            }
        } catch (Exception e) {
            tableCache.put(tableName, false);
            return false;
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        String key = tableName + "." + columnName;
        if (columnCache.containsKey(key)) return columnCache.get(key);

        String sql =
                "SELECT 1 FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                columnCache.put(key, exists);
                return exists;
            }
        } catch (Exception e) {
            columnCache.put(key, false);
            return false;
        }
    }

    private String firstExistingColumn(String table, String... candidates) {
        for (String candidate : candidates) {
            if (columnExists(table, candidate)) return candidate;
        }
        return null;
    }

    private String titleCol() {
        return firstExistingColumn("banners", "title", "banner_title", "name", "heading");
    }

    private String imageCol() {
        return firstExistingColumn("banners", "image_path", "image", "banner_image", "path", "file_path");
    }

    private String activeCol() {
        return firstExistingColumn("banners", "active", "is_active", "enabled", "status");
    }

    private String activeTrueExpr(String col) {
        if (col == null) return "";
        if ("status".equalsIgnoreCase(col)) return col + "='ACTIVE'";
        return col + "=1";
    }

    /* ================= ADD BANNER ================= */

    public boolean addBanner(String title, String path) {

        if (!tableExists("banners")) return false;

        String tCol = titleCol();
        String iCol = imageCol();
        String aCol = activeCol();
        if (iCol == null) return false;

        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        if (tCol != null) {
            cols.add(tCol);
            vals.add(title);
        }
        cols.add(iCol);
        vals.add(path);
        if (aCol != null) {
            cols.add(aCol);
            vals.add("status".equalsIgnoreCase(aCol) ? "INACTIVE" : 0);
        }

        StringBuilder sql = new StringBuilder("INSERT INTO banners (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append(cols.get(i));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < vals.size(); i++) {
                ps.setObject(i + 1, vals.get(i));
            }

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
                    rs.getString("title_value"),
                    rs.getString("image_value"),
                    rs.getString("active_value")
            };
        } else {
            return new String[]{
                    rs.getString("id"),
                    rs.getString("title_value"),
                    rs.getString("image_value")
            };
        }
    }

    /* ================= GET ALL ================= */

    public List<String[]> getAllBanners() {

        List<String[]> list = new ArrayList<>();
        if (!tableExists("banners")) return list;

        String tCol = titleCol();
        String iCol = imageCol();
        String aCol = activeCol();
        if (iCol == null) return list;

        String activeExpr = aCol == null
                ? "'NO'"
                : ("status".equalsIgnoreCase(aCol)
                ? "CASE WHEN " + aCol + "='ACTIVE' THEN 'YES' ELSE 'NO' END"
                : "CASE WHEN " + aCol + "=1 THEN 'YES' ELSE 'NO' END");

        String sql =
                "SELECT id, " +
                (tCol != null ? tCol : "''") + " AS title_value, " +
                iCol + " AS image_value, " +
                activeExpr + " AS active_value " +
                "FROM banners ORDER BY id DESC LIMIT 300";

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
        if (!tableExists("banners")) return list;

        String tCol = titleCol();
        String iCol = imageCol();
        String aCol = activeCol();
        if (iCol == null) return list;

        String sql =
                "SELECT id, " +
                (tCol != null ? tCol : "''") + " AS title_value, " +
                iCol + " AS image_value " +
                "FROM banners " +
                (aCol != null ? "WHERE " + activeTrueExpr(aCol) : "");

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

        if (!tableExists("banners")) return false;

        String aCol = activeCol();
        if (aCol == null) return false;

        String disableSql;
        String enableSql;
        if ("status".equalsIgnoreCase(aCol)) {
            disableSql = "UPDATE banners SET " + aCol + "='INACTIVE'";
            enableSql = "UPDATE banners SET " + aCol + "='ACTIVE' WHERE id=?";
        } else {
            disableSql = "UPDATE banners SET " + aCol + "=0";
            enableSql = "UPDATE banners SET " + aCol + "=1 WHERE id=?";
        }

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
        if (!tableExists("banners")) return list;

        String iCol = imageCol();
        String aCol = activeCol();
        if (iCol == null) return list;

        String sql =
                "SELECT " + iCol + " AS image_value FROM banners " +
                (aCol != null ? "WHERE " + activeTrueExpr(aCol) + " " : "") +
                "ORDER BY id DESC LIMIT 20";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String image = rs.getString("image_value");
                if (image != null && !image.isBlank()) {
                    list.add(image);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
