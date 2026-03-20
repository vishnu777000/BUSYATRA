package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SeatDAO {

    private final Map<String, Boolean> tableCache = new HashMap<>();
    private final Map<String, Boolean> columnCache = new HashMap<>();

    private boolean tableExists(String tableName) {
        if (tableCache.containsKey(tableName)) return tableCache.get(tableName);

        String sql =
                "SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ? LIMIT 1";

        try (Connection con = DBConnectionUtil.getConnection();
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

        try (Connection con = DBConnectionUtil.getConnection();
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

    private int getInt(String sql, int scheduleId) {
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double getDouble(String sql, int scheduleId) {
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Set<String> getUnavailableSeats(int scheduleId, int newFrom, int newTo) {
        Set<String> set = new HashSet<>();

        if (newFrom >= newTo) return set;
        boolean hasBookings = tableExists("bookings");
        boolean hasBookedSeats = tableExists("booked_seats");
        if (!hasBookings && !hasBookedSeats) return set;

        String bookingSeatCol = hasBookings ? firstExistingColumn("bookings", "seat_no", "seat_number", "seat") : null;
        String bookingFromOrderCol = hasBookings ? firstExistingColumn("bookings", "from_order", "from_stop_order", "src_order") : null;
        String bookingToOrderCol = hasBookings ? firstExistingColumn("bookings", "to_order", "to_stop_order", "dst_order") : null;
        String bookingStatusCol = hasBookings ? firstExistingColumn("bookings", "status") : null;

        String holdSeatCol = hasBookedSeats ? firstExistingColumn("booked_seats", "seat_no", "seat_number", "seat") : null;
        String holdFromOrderCol = hasBookedSeats ? firstExistingColumn("booked_seats", "from_order", "from_stop_order", "src_order") : null;
        String holdToOrderCol = hasBookedSeats ? firstExistingColumn("booked_seats", "to_order", "to_stop_order", "dst_order") : null;

        boolean hasSegmentCols = (bookingFromOrderCol != null && bookingToOrderCol != null)
                || (holdFromOrderCol != null && holdToOrderCol != null);

        if (hasBookings && bookingSeatCol != null) {
            String sql =
                    "SELECT " + bookingSeatCol + " AS seat_value" +
                    (bookingFromOrderCol != null ? ", " + bookingFromOrderCol + " AS from_ord" : "") +
                    (bookingToOrderCol != null ? ", " + bookingToOrderCol + " AS to_ord" : "") +
                    " FROM bookings WHERE schedule_id=?" +
                    (bookingStatusCol != null ? " AND " + bookingStatusCol + "='CONFIRMED'" : "");

            try (Connection con = DBConnectionUtil.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, scheduleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String seatNo = rs.getString("seat_value");
                        if (seatNo == null || seatNo.isBlank()) continue;

                        if (bookingFromOrderCol == null || bookingToOrderCol == null) {
                            // Avoid blocking whole seat map when segment columns are missing but we have booked_seats segments.
                            if (!hasSegmentCols) {
                                set.add(seatNo);
                            }
                            continue;
                        }

                        int existingFrom = rs.getInt("from_ord");
                        int existingTo = rs.getInt("to_ord");

                        if (newFrom < existingTo && newTo > existingFrom) {
                            set.add(seatNo);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (hasBookedSeats && holdSeatCol != null) {
            String sql =
                    "SELECT " + holdSeatCol + " AS seat_value" +
                    (holdFromOrderCol != null ? ", " + holdFromOrderCol + " AS from_ord" : "") +
                    (holdToOrderCol != null ? ", " + holdToOrderCol + " AS to_ord" : "") +
                    " FROM booked_seats WHERE schedule_id=?";

            try (Connection con = DBConnectionUtil.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, scheduleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String seatNo = rs.getString("seat_value");
                        if (seatNo == null || seatNo.isBlank()) continue;

                        if (holdFromOrderCol == null || holdToOrderCol == null) {
                            if (!hasSegmentCols) {
                                set.add(seatNo);
                            }
                            continue;
                        }

                        int existingFrom = rs.getInt("from_ord");
                        int existingTo = rs.getInt("to_ord");
                        if (newFrom < existingTo && newTo > existingFrom) {
                            set.add(seatNo);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return set;
    }

    public int getTotalSeatsBySchedule(int scheduleId) {
        String seatsCol = firstExistingColumn("buses", "total_seats", "seat_count", "seats");
        if (seatsCol == null) return 0;

        String sql =
                "SELECT b." + seatsCol + " " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id=b.id " +
                "WHERE s.id=?";

        return getInt(sql, scheduleId);
    }

    public double getFareBySchedule(int scheduleId) {
        String fareCol = firstExistingColumn("buses", "fare_multiplier", "multiplier", "fare");
        if (fareCol == null) return 0;

        String sql =
                "SELECT b." + fareCol + " " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id=b.id " +
                "WHERE s.id=?";

        return getDouble(sql, scheduleId);
    }

    public Set<String> getBookedSeats(int scheduleId) {
        Set<String> set = new HashSet<>();
        if (!tableExists("bookings")) return set;

        String seatCol = firstExistingColumn("bookings", "seat_no", "seat_number", "seat");
        String statusCol = firstExistingColumn("bookings", "status");
        if (seatCol == null) return set;

        String sql =
                "SELECT " + seatCol + " AS seat_value FROM bookings WHERE schedule_id=?" +
                (statusCol != null ? " AND " + statusCol + "='CONFIRMED'" : "");

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String seatNo = rs.getString("seat_value");
                    if (seatNo != null && !seatNo.isBlank()) {
                        set.add(seatNo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return set;
    }

    public String getSeatLabel(int seatNumber) {
        return "S" + seatNumber;
    }
}
