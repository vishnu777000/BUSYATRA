package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO {

    

    private String[] buildRow(ResultSet rs) throws Exception {

        return new String[]{
                rs.getString("created_at"),
                rs.getString("action") != null ? rs.getString("action") : "-",
                rs.getString("details") != null ? rs.getString("details") : "-"
        };
    }

    

    public List<String[]> getRecentActivity(int userId) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT created_at, action, details FROM (" +

                "  SELECT created_at, 'BOOKING' AS action, " +
                "         CONCAT('Ticket ID ', id, ' booked (₹', amount, ')') AS details " +
                "  FROM tickets WHERE user_id=? " +

                "  UNION ALL " +

                "  SELECT created_at, 'CANCEL' AS action, " +
                "         CONCAT('Ticket ID ', id, ' cancelled (₹', amount, ' refunded)') AS details " +
                "  FROM tickets WHERE user_id=? AND status='CANCELLED' " +

                "  UNION ALL " +

                "  SELECT created_at, type AS action, " +
                "         CONCAT(type, ' ₹', amount) AS details " +
                "  FROM wallet_transactions WHERE user_id=? " +

                ") t ORDER BY created_at DESC LIMIT 8";

        try (
                Connection con = DBConnectionUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(buildRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}