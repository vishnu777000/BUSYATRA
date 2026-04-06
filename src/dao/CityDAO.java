package dao;

import config.DBConfig;
import util.SchemaCache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CityDAO {

    private static final Map<String, String> CITY_ALIASES = new LinkedHashMap<>();

    static {
        alias("vizag", "Visakhapatnam");
        alias("vizag city", "Visakhapatnam");
        alias("vizagpatanam", "Visakhapatnam");
        alias("vzg", "Visakhapatnam");
        alias("vskp", "Visakhapatnam");
        alias("vizagpatnam", "Visakhapatnam");
        alias("vizianagaram via vizag", "Visakhapatnam");
        alias("samalakota", "Samalkot");
        alias("samalkota", "Samalkot");
        alias("samlaakota", "Samalkot");
        alias("samlakota", "Samalkot");
        alias("samalkot junction", "Samalkot");
        alias("smkt", "Samalkot");
        alias("kkd", "Kakinada");
        alias("kakinada town", "Kakinada");
        alias("kakinada port", "Kakinada");
        alias("rjy", "Rajahmundry");
        alias("rajamahendravaram", "Rajahmundry");
        alias("rajamahendri", "Rajahmundry");
        alias("rajamundry", "Rajahmundry");
        alias("tni", "Tuni");
        alias("bza", "Vijayawada");
        alias("ravulapalam", "Ravulapalem");
        alias("ravulapalem", "Ravulapalem");
        alias("surampalem", "Surampalem");
        alias("bangalore", "Bangalore");
        alias("bengaluru", "Bangalore");
        alias("blr", "Bangalore");
        alias("bng", "Bangalore");
        alias("chennai", "Chennai");
        alias("madras", "Chennai");
        alias("maa", "Chennai");
        alias("mysore", "Mysore");
        alias("mysuru", "Mysore");
        alias("myq", "Mysore");
        alias("mumbai", "Mumbai");
        alias("bombay", "Mumbai");
        alias("bom", "Mumbai");
        alias("pune", "Pune");
        alias("poona", "Pune");
        alias("pnq", "Pune");
        alias("bhubaneswar", "Bhubaneswar");
        alias("bbsr", "Bhubaneswar");
        alias("kolkata", "Kolkata");
        alias("calcutta", "Kolkata");
        alias("ccu", "Kolkata");
        alias("nagpur", "Nagpur");
        alias("solapur", "Solapur");
        alias("vellore", "Vellore");
        alias("chittoor", "Chittoor");
    }

    public List<String> searchCities(String prefix) {

        List<String> list = new ArrayList<>();

        if (prefix == null || prefix.trim().isEmpty()) {
            return list;
        }

        String query = prefix.trim();
        String normalized = normalize(query);
        Set<String> ordered = new LinkedHashSet<>();

        for (Map.Entry<String, String> entry : CITY_ALIASES.entrySet()) {
            String alias = entry.getKey();
            String canonical = entry.getValue();
            if (alias.startsWith(normalized)
                    || canonical.toUpperCase(Locale.ENGLISH).startsWith(query.toUpperCase(Locale.ENGLISH))
                    || normalize(canonical).startsWith(normalized)) {
                ordered.add(canonical);
            }
        }

        searchLocations(query, ordered);

        list.addAll(ordered);
        return list;
    }

    public String resolveCityName(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }

        String normalized = normalize(rawValue);
        String alias = CITY_ALIASES.get(normalized);
        if (alias != null) {
            return alias;
        }

        String exactMatch = resolveExactLocation(rawValue.trim());
        if (exactMatch != null && !exactMatch.isBlank()) {
            return exactMatch;
        }

        List<String> suggestions = searchCities(rawValue);
        return suggestions.isEmpty() ? rawValue.trim() : suggestions.get(0);
    }

    private void searchLocations(String query, Set<String> ordered) {
        String unionSql = buildLocationUnionSql();
        if (unionSql == null) {
            return;
        }

        String sql =
                "SELECT location FROM (" + unionSql + ") locations " +
                "WHERE location IS NOT NULL AND TRIM(location) <> '' " +
                "AND (" +
                "UPPER(location) LIKE ? " +
                "OR REPLACE(UPPER(location), ' ', '') LIKE ? " +
                "OR UPPER(location) LIKE ?) " +
                "ORDER BY CASE " +
                "WHEN UPPER(location) LIKE ? THEN 0 " +
                "WHEN REPLACE(UPPER(location), ' ', '') LIKE ? THEN 1 " +
                "ELSE 2 END, location ASC " +
                "LIMIT 8";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String upper = query.toUpperCase(Locale.ENGLISH);
            String compact = upper.replace(" ", "");
            ps.setString(1, upper + "%");
            ps.setString(2, compact + "%");
            ps.setString(3, "% " + upper + "%");
            ps.setString(4, upper + "%");
            ps.setString(5, compact + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String location = rs.getString("location");
                    if (location != null && !location.isBlank()) {
                        ordered.add(location.trim());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String resolveExactLocation(String rawValue) {
        String unionSql = buildLocationUnionSql();
        if (unionSql == null) {
            return null;
        }

        String sql =
                "SELECT location FROM (" + unionSql + ") locations " +
                "WHERE location IS NOT NULL AND TRIM(location) <> '' " +
                "AND (UPPER(TRIM(location)) = UPPER(TRIM(?)) " +
                "OR REPLACE(UPPER(location), ' ', '') = REPLACE(UPPER(?), ' ', '')) " +
                "LIMIT 1";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rawValue);
            ps.setString(2, rawValue);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("location");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String buildLocationUnionSql() {
        if (!SchemaCache.tableExists("cities") && !SchemaCache.tableExists("route_stops")) {
            return null;
        }

        List<String> selects = new ArrayList<>();
        if (SchemaCache.tableExists("cities")) {
            selects.add("SELECT DISTINCT TRIM(name) AS location FROM cities");
        }

        String routeStopSelect = buildRouteStopLocationSelect();
        if (routeStopSelect != null) {
            selects.add(routeStopSelect);
        }

        if (selects.isEmpty()) {
            return null;
        }

        return String.join(" UNION ", selects);
    }

    private String buildRouteStopLocationSelect() {
        if (!SchemaCache.tableExists("route_stops")) {
            return null;
        }

        String stopCol = SchemaCache.firstExistingColumn("route_stops", "stop_name", "stop", "city_name");
        String cityIdCol = SchemaCache.firstExistingColumn("route_stops", "city_id");
        boolean hasCities = SchemaCache.tableExists("cities");

        if (cityIdCol != null && hasCities && stopCol != null) {
            return "SELECT DISTINCT COALESCE(NULLIF(TRIM(c.name), ''), NULLIF(TRIM(rs." + stopCol + "), '')) AS location " +
                    "FROM route_stops rs LEFT JOIN cities c ON c.id = rs." + cityIdCol;
        }
        if (cityIdCol != null && hasCities) {
            return "SELECT DISTINCT TRIM(c.name) AS location FROM route_stops rs JOIN cities c ON c.id = rs." + cityIdCol;
        }
        if (stopCol != null) {
            return "SELECT DISTINCT TRIM(rs." + stopCol + ") AS location FROM route_stops rs";
        }
        return null;
    }

    private static void alias(String alias, String canonical) {
        CITY_ALIASES.put(normalize(alias), canonical);
    }

    private static String normalize(String value) {
        return value == null ? ""
                : value.trim().toUpperCase(Locale.ENGLISH).replaceAll("[^A-Z0-9]", "");
    }
}
