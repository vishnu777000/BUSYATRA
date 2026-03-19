package dao;

import config.DBConfig;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class SeatLockDAO {

    private void clearExpiredLocks() {
        String sql = "DELETE FROM seat_locks WHERE expires_at < NOW()";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean lockSeat(int scheduleId, String seatNo, int userId) {

        clearExpiredLocks();

        if (isSeatLocked(scheduleId, seatNo)) return false;

        String sql =
                "INSERT INTO seat_locks(schedule_id, seat_no, user_id, expires_at) " +
                "VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL 5 MINUTE))";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, scheduleId);
            ps.setString(2, seatNo);
            ps.setInt(3, userId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean isSeatLocked(int scheduleId, String seatNo) {

        clearExpiredLocks();

        String sql =
                "SELECT 1 FROM seat_locks WHERE schedule_id=? AND seat_no=? AND expires_at > NOW()";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, scheduleId);
            ps.setString(2, seatNo);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public Set<String> getLockedSeats(int scheduleId) {

        Set<String> set = new HashSet<>();
        clearExpiredLocks();

        String sql =
                "SELECT seat_no FROM seat_locks WHERE schedule_id=? AND expires_at > NOW()";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, scheduleId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                set.add(rs.getString("seat_no"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return set;
    }

    public void releaseSeatLocks(int userId) {

        String sql = "DELETE FROM seat_locks WHERE user_id=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}