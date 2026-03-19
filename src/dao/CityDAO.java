package dao;

import config.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CityDAO {

    public List<String> searchCities(String prefix) {

        List<String> list = new ArrayList<>();

        if (prefix == null || prefix.trim().isEmpty()) {
            return list;
        }

        String sql =
                "SELECT name FROM cities " +
                "WHERE UPPER(name) LIKE ? " +
                "ORDER BY name ASC " +
                "LIMIT 5";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, prefix.trim().toUpperCase() + "%");

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(rs.getString("name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}