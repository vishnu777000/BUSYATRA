package dao;

import config.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SeatLockDAO {

    private final Map<String, Boolean> tableCache = new HashMap<>();
    private final Map<String, Boolean> columnCache = new HashMap<>();
    private volatile long lastClearExpiredMs = 0L;

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

    private String seatCol() {
        return firstExistingColumn("seat_locks", "seat_no", "seat_number", "seat");
    }

    private String scheduleCol() {
        return firstExistingColumn("seat_locks", "schedule_id", "schedule");
    }

    private String userCol() {
        return firstExistingColumn("seat_locks", "user_id", "uid");
    }

    private String expiresCol() {
        return firstExistingColumn("seat_locks", "expires_at", "expire_at", "expires_on", "lock_expires_at");
    }

    private String fromOrderCol() {
        return firstExistingColumn("seat_locks", "from_order", "from_stop_order", "src_order");
    }

    private String toOrderCol() {
        return firstExistingColumn("seat_locks", "to_order", "to_stop_order", "dst_order");
    }

    private boolean segmentsOverlap(int newFrom, int newTo, int existingFrom, int existingTo) {
        if (newFrom <= 0 || newTo <= 0 || existingFrom <= 0 || existingTo <= 0) return true;
        return newFrom < existingTo && newTo > existingFrom;
    }

    private boolean lockingInfrastructureAvailable() {
        return tableExists("seat_locks")
                && scheduleCol() != null
                && seatCol() != null
                && userCol() != null;
    }

    public void clearExpiredLocks() {
        long now = System.currentTimeMillis();
        if (now - lastClearExpiredMs < 10_000L) return;
        if (!lockingInfrastructureAvailable()) return;

        String expires = expiresCol();
        if (expires == null) return;

        String sql = "DELETE FROM seat_locks WHERE " + expires + " < NOW()";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
            lastClearExpiredMs = now;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean lockSeat(int scheduleId, String seatNo, int userId) {
        return lockSeat(scheduleId, seatNo, userId, -1, -1);
    }

    public boolean lockSeat(int scheduleId, String seatNo, int userId, int fromOrder, int toOrder) {
        // Fail-open for legacy schemas without lock support.
        if (!lockingInfrastructureAvailable()) return true;

        String schedule = scheduleCol();
        String seat = seatCol();
        String user = userCol();
        String expires = expiresCol();
        String from = fromOrderCol();
        String to = toOrderCol();

        if (schedule == null || seat == null || user == null) return true;

        clearExpiredLocks();
        if (isSeatLocked(scheduleId, seatNo, fromOrder, toOrder)) return false;

        StringBuilder colSql = new StringBuilder();
        StringBuilder valSql = new StringBuilder();
        colSql.append(schedule).append(", ").append(seat).append(", ").append(user);
        valSql.append("?, ?, ?");
        if (from != null) {
            colSql.append(", ").append(from);
            valSql.append(", ?");
        }
        if (to != null) {
            colSql.append(", ").append(to);
            valSql.append(", ?");
        }
        if (expires != null) {
            colSql.append(", ").append(expires);
            valSql.append(", DATE_ADD(NOW(), INTERVAL 5 MINUTE)");
        }
        String sql = "INSERT INTO seat_locks(" + colSql + ") VALUES (" + valSql + ")";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            ps.setString(2, seatNo);
            ps.setInt(3, userId);
            int idx = 4;
            if (from != null) ps.setInt(idx++, fromOrder);
            if (to != null) ps.setInt(idx, toOrder);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean isSeatLocked(int scheduleId, String seatNo) {
        return isSeatLocked(scheduleId, seatNo, -1, -1);
    }

    public boolean isSeatLocked(int scheduleId, String seatNo, int fromOrder, int toOrder) {
        if (!lockingInfrastructureAvailable()) return false;

        String schedule = scheduleCol();
        String seat = seatCol();
        String expires = expiresCol();
        String from = fromOrderCol();
        String to = toOrderCol();

        if (schedule == null || seat == null) return false;

        clearExpiredLocks();

        String sql =
                "SELECT " +
                        (from != null ? from + " AS from_ord, " : "") +
                        (to != null ? to + " AS to_ord, " : "") +
                        "1 AS one_col " +
                "FROM seat_locks WHERE " + schedule + "=? AND " + seat + "=?" +
                (expires != null ? " AND " + expires + " > NOW()" : "");

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            ps.setString(2, seatNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (from == null || to == null) return true;
                    int existingFrom = rs.getInt("from_ord");
                    int existingTo = rs.getInt("to_ord");
                    if (segmentsOverlap(fromOrder, toOrder, existingFrom, existingTo)) {
                        return true;
                    }
                }
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public Set<String> getLockedSeats(int scheduleId) {
        return getLockedSeats(scheduleId, -1, -1);
    }

    public Set<String> getLockedSeats(int scheduleId, int fromOrder, int toOrder) {
        Set<String> set = new HashSet<>();
        if (!lockingInfrastructureAvailable()) return set;

        String schedule = scheduleCol();
        String seat = seatCol();
        String expires = expiresCol();
        String from = fromOrderCol();
        String to = toOrderCol();

        if (schedule == null || seat == null) return set;

        clearExpiredLocks();

        String sql =
                "SELECT " + seat + " AS seat_value" +
                        (from != null ? ", " + from + " AS from_ord" : "") +
                        (to != null ? ", " + to + " AS to_ord" : "") +
                        " FROM seat_locks WHERE " + schedule + "=?" +
                (expires != null ? " AND " + expires + " > NOW()" : "");

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String seatNo = rs.getString("seat_value");
                    if (seatNo != null && !seatNo.isBlank()) {
                        if (from == null || to == null) {
                            set.add(seatNo);
                        } else {
                            int existingFrom = rs.getInt("from_ord");
                            int existingTo = rs.getInt("to_ord");
                            if (segmentsOverlap(fromOrder, toOrder, existingFrom, existingTo)) {
                                set.add(seatNo);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return set;
    }

    public void releaseSeatLocks(int userId) {
        if (!lockingInfrastructureAvailable()) return;

        String user = userCol();
        if (user == null) return;

        String sql = "DELETE FROM seat_locks WHERE " + user + "=?";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean releaseSeatLock(int scheduleId, String seatNo, int userId) {
        if (!lockingInfrastructureAvailable()) return true;

        String schedule = scheduleCol();
        String seat = seatCol();
        String user = userCol();
        if (schedule == null || seat == null || user == null) return false;

        String sql =
                "DELETE FROM seat_locks WHERE " + schedule + "=? AND " + seat + "=? AND " + user + "=?";

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
}
