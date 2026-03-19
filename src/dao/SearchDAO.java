package dao;

import config.DBConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SearchDAO {

    public List<String[]> searchSchedules(String source,
                                          String destination,
                                          String date) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT " +
                "s.id AS schedule_id, " +
                "s.route_id, " +
                "b.operator, " +
                "b.bus_type, " +

                "rs1.stop_order AS from_order, " +
                "rs2.stop_order AS to_order, " +

                /* 🔥 DYNAMIC FARE */
                "ROUND((rs2.distance_from_start - rs1.distance_from_start) * r.base_fare * b.fare_multiplier, 2) AS fare, " +

                "TIME(s.departure_time) AS dep_time, " +
                "TIME(s.arrival_time) AS arr_time, " +

                /* 🔥 AVAILABLE SEATS (NO GROUP BY) */
                "(b.total_seats - IFNULL((" +
                "   SELECT COUNT(*) FROM bookings bk " +
                "   WHERE bk.schedule_id = s.id " +
                "   AND bk.status = 'CONFIRMED'" +
                "),0)) AS available_seats " +

                "FROM schedules s " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "JOIN route_stops rs1 ON rs1.route_id = r.id " +
                "JOIN route_stops rs2 ON rs2.route_id = r.id " +

                "WHERE rs1.stop_name = ? " +
                "AND rs2.stop_name = ? " +
                "AND rs1.stop_order < rs2.stop_order " +

                "AND s.departure_time >= ? " +
                "AND s.departure_time < DATE_ADD(?, INTERVAL 1 DAY) " +

                "AND s.status = 'ACTIVE' " +

                "ORDER BY s.departure_time";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, source.trim());
            ps.setString(2, destination.trim());
            ps.setString(3, date + " 00:00:00");
            ps.setString(4, date + " 00:00:00");

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                list.add(new String[]{
                        rs.getString("schedule_id"),
                        rs.getString("route_id"),
                        rs.getString("operator"),
                        rs.getString("bus_type"),
                        rs.getString("fare") != null ? rs.getString("fare") : "0",
                        rs.getString("dep_time"),
                        rs.getString("arr_time"),
                        rs.getString("from_order"),
                        rs.getString("to_order"),
                        rs.getString("available_seats")
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}