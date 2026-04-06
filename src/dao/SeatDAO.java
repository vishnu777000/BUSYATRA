package dao;

import config.DBConfig;
import util.DBConnectionUtil;
import util.SchemaCache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class SeatDAO {

    private final ThreadLocal<String> lastError = new ThreadLocal<>();

    public void clearLastError() {
        lastError.remove();
    }

    public String getLastError() {
        String message = lastError.get();
        return message == null ? "" : message;
    }

    public boolean hasLastError() {
        String message = lastError.get();
        return message != null && !message.isBlank();
    }

    private void setLastError(Throwable error) {
        lastError.set(DBConfig.userFriendlyMessage(error));
    }

    private void setLastError(String message) {
        if (message == null || message.isBlank()) {
            lastError.remove();
            return;
        }
        lastError.set(message);
    }

    private boolean tableExists(String tableName) {
        return SchemaCache.tableExists(tableName);
    }

    private boolean columnExists(String tableName, String columnName) {
        return SchemaCache.columnExists(tableName, columnName);
    }

    private String firstExistingColumn(String table, String... candidates) {
        return SchemaCache.firstExistingColumn(table, candidates);
    }

    private int getInt(String sql, int scheduleId) {
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            setLastError(e);
            DBConnectionUtil.logIfUnexpected(e);
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
            setLastError(e);
            DBConnectionUtil.logIfUnexpected(e);
        }
        return 0;
    }

    public Set<String> getUnavailableSeats(int scheduleId, int newFrom, int newTo) {
        clearLastError();
        Set<String> set = new HashSet<>();

        if (newFrom >= newTo) return set;
        boolean hasBookings = tableExists("bookings");
        boolean hasBookedSeats = tableExists("booked_seats");
        if (!hasBookings && !hasBookedSeats) return set;

        String bookingScheduleCol = hasBookings ? firstExistingColumn("bookings", "schedule_id", "schedule", "scheduleid", "sid") : null;
        String bookingSeatCol = hasBookings ? firstExistingColumn("bookings", "seat_no", "seat_number", "seat") : null;
        String bookingFromOrderCol = hasBookings ? firstExistingColumn("bookings", "from_order", "from_stop_order", "src_order") : null;
        String bookingToOrderCol = hasBookings ? firstExistingColumn("bookings", "to_order", "to_stop_order", "dst_order") : null;
        String bookingStatusCol = hasBookings ? firstExistingColumn("bookings", "status") : null;

        String holdScheduleCol = hasBookedSeats ? firstExistingColumn("booked_seats", "schedule_id", "schedule", "scheduleid", "sid") : null;
        String holdSeatCol = hasBookedSeats ? firstExistingColumn("booked_seats", "seat_no", "seat_number", "seat") : null;
        String holdFromOrderCol = hasBookedSeats ? firstExistingColumn("booked_seats", "from_order", "from_stop_order", "src_order") : null;
        String holdToOrderCol = hasBookedSeats ? firstExistingColumn("booked_seats", "to_order", "to_stop_order", "dst_order") : null;

        boolean hasSegmentCols = (bookingFromOrderCol != null && bookingToOrderCol != null)
                || (holdFromOrderCol != null && holdToOrderCol != null);

        if (hasBookings && bookingScheduleCol != null && bookingSeatCol != null) {
            String sql =
                    "SELECT " + bookingSeatCol + " AS seat_value" +
                    (bookingFromOrderCol != null ? ", " + bookingFromOrderCol + " AS from_ord" : "") +
                    (bookingToOrderCol != null ? ", " + bookingToOrderCol + " AS to_ord" : "") +
                    " FROM bookings WHERE " + bookingScheduleCol + "=?" +
                    (bookingStatusCol != null ? " AND " + bookingStatusCol + "='CONFIRMED'" : "");

            try (Connection con = DBConnectionUtil.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, scheduleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String seatNo = rs.getString("seat_value");
                        if (seatNo == null || seatNo.isBlank()) continue;

                        if (bookingFromOrderCol == null || bookingToOrderCol == null) {
                            
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
                setLastError(e);
                DBConnectionUtil.logIfUnexpected(e);
            }
        }

        if (hasBookedSeats && holdScheduleCol != null && holdSeatCol != null) {
            String sql =
                    "SELECT " + holdSeatCol + " AS seat_value" +
                    (holdFromOrderCol != null ? ", " + holdFromOrderCol + " AS from_ord" : "") +
                    (holdToOrderCol != null ? ", " + holdToOrderCol + " AS to_ord" : "") +
                    " FROM booked_seats WHERE " + holdScheduleCol + "=?";

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
                setLastError(e);
                DBConnectionUtil.logIfUnexpected(e);
            }
        }

        return set;
    }

    public int getTotalSeatsBySchedule(int scheduleId) {
        clearLastError();
        String seatsCol = firstExistingColumn("buses", "total_seats", "seat_count", "seats");
        if (seatsCol == null) {
            if (!hasLastError()) {
                setLastError("Bus seat configuration column is missing in the database.");
            }
            return 0;
        }

        String sql =
                "SELECT b." + seatsCol + " " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id=b.id " +
                "WHERE s.id=?";

        return getInt(sql, scheduleId);
    }

    public double getFareBySchedule(int scheduleId) {
        clearLastError();
        String fareCol = firstExistingColumn("buses", "fare_multiplier", "multiplier", "fare");
        if (fareCol == null) {
            if (!hasLastError()) {
                setLastError("Bus fare column is missing in the database.");
            }
            return 0;
        }

        String sql =
                "SELECT b." + fareCol + " " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id=b.id " +
                "WHERE s.id=?";

        return getDouble(sql, scheduleId);
    }

    public Set<String> getBookedSeats(int scheduleId) {
        clearLastError();
        Set<String> set = new HashSet<>();
        if (!tableExists("bookings")) return set;

        String scheduleCol = firstExistingColumn("bookings", "schedule_id", "schedule", "scheduleid", "sid");
        String seatCol = firstExistingColumn("bookings", "seat_no", "seat_number", "seat");
        String statusCol = firstExistingColumn("bookings", "status");
        if (scheduleCol == null || seatCol == null) return set;

        String sql =
                "SELECT " + seatCol + " AS seat_value FROM bookings WHERE " + scheduleCol + "=?" +
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
            setLastError(e);
            DBConnectionUtil.logIfUnexpected(e);
        }

        return set;
    }

    public String getSeatLabel(int seatNumber) {
        return "S" + seatNumber;
    }
}
