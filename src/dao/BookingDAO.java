package dao;

import config.DBConfig;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingDAO {

    private final Map<String, Boolean> tableCache = new HashMap<>();
    private final Map<String, Boolean> columnCache = new HashMap<>();
    private final Map<String, Integer> routeStopCache = new HashMap<>();
    private volatile String lastErrorMessage = "";

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

    private void ensureColumn(String table, String column, String ddlType) {
        if (!tableExists(table) || columnExists(table, column)) return;
        String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddlType;
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
            columnCache.put(table + "." + column, true);
        } catch (Exception ignored) {
            // best-effort compatibility migration
        }
    }

    private void ensureBookingSchemaCompatibility() {
        if (!tableExists("bookings")) return;

        ensureColumn("bookings", "user_id", "INT NULL");
        ensureColumn("bookings", "schedule_id", "INT NULL");
        ensureColumn("bookings", "route_id", "INT NULL");
        ensureColumn("bookings", "from_city", "VARCHAR(120) NULL");
        ensureColumn("bookings", "to_city", "VARCHAR(120) NULL");
        ensureColumn("bookings", "from_order", "INT NULL");
        ensureColumn("bookings", "to_order", "INT NULL");
        ensureColumn("bookings", "seat_no", "VARCHAR(24) NULL");
        ensureColumn("bookings", "journey_date", "DATE NULL");
        ensureColumn("bookings", "amount", "DECIMAL(12,2) NULL");
        ensureColumn("bookings", "passenger_name", "VARCHAR(120) NULL");
        ensureColumn("bookings", "passenger_phone", "VARCHAR(20) NULL");
        ensureColumn("bookings", "passenger_email", "VARCHAR(160) NULL");
        ensureColumn("bookings", "status", "VARCHAR(24) NULL");
        ensureColumn("bookings", "created_at", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP");

        if (tableExists("booked_seats")) {
            ensureColumn("booked_seats", "booking_id", "INT NULL");
            ensureColumn("booked_seats", "schedule_id", "INT NULL");
            ensureColumn("booked_seats", "seat_no", "VARCHAR(24) NULL");
            ensureColumn("booked_seats", "from_order", "INT NULL");
            ensureColumn("booked_seats", "to_order", "INT NULL");
        }
    }

    private Integer resolveRouteStopId(int scheduleId, String stopName) {
        if (stopName == null || stopName.isBlank()) return null;
        if (!tableExists("route_stops") || !tableExists("schedules")) return null;
        String cacheKey = scheduleId + "|" + stopName.trim().toUpperCase();
        if (routeStopCache.containsKey(cacheKey)) {
            return routeStopCache.get(cacheKey);
        }

        String stopNameCol = firstExistingColumn("route_stops", "stop_name", "name");
        String cityIdCol = firstExistingColumn("route_stops", "city_id");
        String cityNameCol = tableExists("cities") ? firstExistingColumn("cities", "name", "city_name") : null;
        String scheduleRouteCol = firstExistingColumn("schedules", "route_id");

        if (scheduleRouteCol == null) return null;
        if (stopNameCol == null && (cityIdCol == null || cityNameCol == null)) return null;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT rs.id FROM schedules s ");
        sql.append("JOIN route_stops rs ON rs.route_id = s.").append(scheduleRouteCol).append(" ");
        if (cityIdCol != null && cityNameCol != null && tableExists("cities")) {
            sql.append("LEFT JOIN cities c ON c.id = rs.").append(cityIdCol).append(" ");
        }
        sql.append("WHERE s.id=? AND (");
        boolean added = false;
        if (stopNameCol != null) {
            sql.append("UPPER(TRIM(rs.").append(stopNameCol).append(")) = UPPER(TRIM(?))");
            added = true;
        }
        if (cityIdCol != null && cityNameCol != null && tableExists("cities")) {
            if (added) sql.append(" OR ");
            sql.append("UPPER(TRIM(c.").append(cityNameCol).append(")) = UPPER(TRIM(?))");
            added = true;
        }
        if (!added) return null;
        sql.append(") ORDER BY rs.stop_order LIMIT 1");

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setInt(1, scheduleId);
            int idx = 2;
            if (stopNameCol != null) ps.setString(idx++, stopName);
            if (cityIdCol != null && cityNameCol != null && tableExists("cities")) ps.setString(idx, stopName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int value = rs.getInt(1);
                    routeStopCache.put(cacheKey, value);
                    return value;
                }
            }
        } catch (Exception ignored) {
            // no-op, best-effort mapping
        }
        routeStopCache.put(cacheKey, null);
        return null;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage == null ? "" : lastErrorMessage;
    }

    private void addColumn(List<String> cols, List<Object> vals, String col, Object val) {
        if (col != null) {
            cols.add(col);
            vals.add(val);
        }
    }

    private boolean segmentsOverlap(int newFrom, int newTo, int existingFrom, int existingTo) {
        if (newFrom <= 0 || newTo <= 0 || existingFrom <= 0 || existingTo <= 0) return true;
        return newFrom < existingTo && newTo > existingFrom;
    }

    private Date toSqlDate(String raw) {
        try {
            if (raw == null || raw.isBlank()) return Date.valueOf(LocalDate.now());
            String t = raw.trim();
            if (t.length() >= 10) t = t.substring(0, 10);
            return Date.valueOf(t);
        } catch (Exception e) {
            return Date.valueOf(LocalDate.now());
        }
    }

    private boolean hasSeatConflictInBookings(int scheduleId, String seatNo, int newFrom, int newTo) {
        if (!tableExists("bookings")) return false;

        String scheduleCol = firstExistingColumn("bookings", "schedule_id", "schedule", "scheduleid", "sid");
        String seatCol = firstExistingColumn("bookings", "seat_no", "seat_number", "seat", "seatno", "seat_num");
        String fromOrderCol = firstExistingColumn("bookings", "from_order", "from_stop_order", "src_order", "fromorder", "srcorder");
        String toOrderCol = firstExistingColumn("bookings", "to_order", "to_stop_order", "dst_order", "toorder", "dstorder");
        String statusCol = firstExistingColumn("bookings", "status");

        if (scheduleCol == null || seatCol == null) return false;

        String sql =
                "SELECT " + seatCol + " AS seat_value" +
                (fromOrderCol != null ? ", " + fromOrderCol + " AS from_ord" : "") +
                (toOrderCol != null ? ", " + toOrderCol + " AS to_ord" : "") +
                " FROM bookings WHERE " + scheduleCol + "=?" +
                " AND " + seatCol + "=?" +
                (statusCol != null ? " AND " + statusCol + "='CONFIRMED'" : "");

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, scheduleId);
            ps.setString(2, seatNo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (fromOrderCol == null || toOrderCol == null) {
                        return true;
                    }
                    int existingFrom = rs.getInt("from_ord");
                    int existingTo = rs.getInt("to_ord");
                    if (segmentsOverlap(newFrom, newTo, existingFrom, existingTo)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= HELPER ================= */

    private String[] buildBookingRow(ResultSet rs) throws SQLException {

        return new String[]{
                rs.getString("id"),
                rs.getString("src"),
                rs.getString("dst"),
                rs.getString("seat_value"),
                rs.getString("amount_value"),
                rs.getString("jdate"),
                rs.getString("status")
        };
    }

    /* ================= UPCOMING ================= */

    public String getUpcomingBookingText(int userId) {

        if (!tableExists("bookings")) {
            return "No active booking";
        }

        String fromCol = columnExists("bookings", "from_city") ? "from_city"
                : (columnExists("bookings", "from_stop") ? "from_stop" : null);
        String toCol = columnExists("bookings", "to_city") ? "to_city"
                : (columnExists("bookings", "to_stop") ? "to_stop" : null);
        String userCol = firstExistingColumn("bookings", "user_id", "uid");
        String dateCol = columnExists("bookings", "journey_date") ? "journey_date"
                : (columnExists("bookings", "travel_date") ? "travel_date"
                : (columnExists("bookings", "created_at") ? "created_at"
                : (columnExists("bookings", "booking_time") ? "booking_time" : null)));
        String statusCol = firstExistingColumn("bookings", "status");

        if (fromCol == null || toCol == null || dateCol == null || userCol == null) {
            return "No active booking";
        }

        String sql =
                "SELECT " + fromCol + " AS src, " + toCol + " AS dst, " + dateCol + " AS jdate " +
                "FROM bookings " +
                "WHERE " + userCol + "=? " +
                "AND " + dateCol + " >= CURDATE() " +
                (statusCol != null ? "AND " + statusCol + "='CONFIRMED' " : "") +
                "ORDER BY " + dateCol + " ASC LIMIT 1";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("src") + " -> " +
                        rs.getString("dst") + " | " +
                        rs.getDate("jdate");
            }

        } catch (Exception e) {
            // no-op: dashboard should still load
        }

        return "No active booking";
    }

    /* ================= USER BOOKINGS ================= */

    public List<String[]> getBookingsByUser(int userId) {

        List<String[]> list = new ArrayList<>();

        if (!tableExists("bookings")) {
            return list;
        }

        String fromCol = columnExists("bookings", "from_city") ? "from_city"
                : (columnExists("bookings", "from_stop") ? "from_stop" : null);
        String toCol = columnExists("bookings", "to_city") ? "to_city"
                : (columnExists("bookings", "to_stop") ? "to_stop" : null);
        String userCol = firstExistingColumn("bookings", "user_id", "uid");
        String dateCol = columnExists("bookings", "journey_date") ? "journey_date"
                : (columnExists("bookings", "travel_date") ? "travel_date"
                : (columnExists("bookings", "created_at") ? "created_at"
                : (columnExists("bookings", "booking_time") ? "booking_time" : null)));
        String seatCol = firstExistingColumn("bookings", "seat_no", "seat_number", "seat");
        String statusCol = firstExistingColumn("bookings", "status");
        String amountCol = firstExistingColumn("bookings", "amount", "total_amount", "fare", "price", "ticket_amount");

        if (fromCol == null || toCol == null || dateCol == null || seatCol == null || userCol == null) {
            return list;
        }
        String amountExpr = amountCol != null ? amountCol : "0";

        String sql =
                "SELECT id," + fromCol + " AS src," + toCol + " AS dst," +
                seatCol + " AS seat_value," + amountExpr + " AS amount_value," + dateCol + " AS jdate," +
                (statusCol != null ? statusCol : "'CONFIRMED'") + " AS status " +
                "FROM bookings WHERE " + userCol + "=? ORDER BY " + dateCol + " DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(buildBookingRow(rs));
            }

        } catch (Exception e) {
            // no-op
        }

        return list;
    }

    public boolean deleteBookingHistoryForUser(int bookingId, int userId) {
        if (bookingId <= 0 || userId <= 0 || !tableExists("bookings")) return false;

        String userCol = firstExistingColumn("bookings", "user_id", "uid", "user", "userid", "customer_id");
        String statusCol = firstExistingColumn("bookings", "status", "booking_status", "state");
        if (userCol == null) return false;

        // Avoid deleting active confirmed ticket from history directly.
        String sql = "DELETE FROM bookings WHERE id=? AND " + userCol + "=?" +
                (statusCol != null ? " AND UPPER(" + statusCol + ") <> 'CONFIRMED'" : "");
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /* ================= SEAT CHECK ================= */

    public boolean isSeatAvailable(int scheduleId,
                                   String seatNo,
                                   int newFrom,
                                   int newTo) {

        if (tableExists("booked_seats")) {
            String scheduleCol = firstExistingColumn("booked_seats", "schedule_id", "schedule", "scheduleid", "sid");
            String seatCol = firstExistingColumn("booked_seats", "seat_no", "seat_number", "seat", "seatno", "seat_num");
            String fromOrderCol = firstExistingColumn("booked_seats", "from_order", "from_stop_order", "src_order", "fromorder", "srcorder");
            String toOrderCol = firstExistingColumn("booked_seats", "to_order", "to_stop_order", "dst_order", "toorder", "dstorder");

            if (scheduleCol != null && seatCol != null) {
                String sql =
                        "SELECT " +
                                (fromOrderCol != null ? fromOrderCol + " AS from_ord, " : "") +
                                (toOrderCol != null ? toOrderCol + " AS to_ord, " : "") +
                                seatCol + " AS seat_value " +
                                "FROM booked_seats WHERE " + scheduleCol + "=? AND " + seatCol + "=?";

                try (Connection con = DBConfig.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, scheduleId);
                    ps.setString(2, seatNo);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            if (fromOrderCol == null || toOrderCol == null) {
                                return false;
                            }
                            int existingFrom = rs.getInt("from_ord");
                            int existingTo = rs.getInt("to_ord");
                            if (segmentsOverlap(newFrom, newTo, existingFrom, existingTo)) {
                                return false;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return !hasSeatConflictInBookings(scheduleId, seatNo, newFrom, newTo);
    }

    /* ================= INSERT BOOKING ================= */

    public int insertBooking(
            int userId,
            int routeId,
            int scheduleId,
            String fromCity,
            String toCity,
            int fromOrder,
            int toOrder,
            String seatNo,
            double amount,
            String journeyDate,
            String passengerName,
            String passengerPhone,
            String passengerEmail) {

        lastErrorMessage = "";
        ensureBookingSchemaCompatibility();
        if (!tableExists("bookings")) return -1;

        String routeCol = firstExistingColumn("bookings", "route_id", "routeid", "rid");
        String userCol = firstExistingColumn("bookings", "user_id", "userid", "uid", "user", "customer_id", "customerid", "passenger_id");
        String scheduleCol = firstExistingColumn("bookings", "schedule_id", "scheduleid", "schedule", "sid", "trip_id", "tripid");
        String fromCityCol = firstExistingColumn("bookings", "from_city", "from_stop", "source", "src", "from_place", "source_city", "boarding_point");
        String toCityCol = firstExistingColumn("bookings", "to_city", "to_stop", "destination", "dst", "to_place", "destination_city", "drop_point");
        String fromOrderCol = firstExistingColumn("bookings", "from_order", "from_stop_order", "src_order", "fromorder", "srcorder", "boarding_order");
        String toOrderCol = firstExistingColumn("bookings", "to_order", "to_stop_order", "dst_order", "toorder", "dstorder", "drop_order");
        String seatCol = firstExistingColumn("bookings", "seat_no", "seat_number", "seat", "seatno", "seat_num", "seatname", "seat_id");
        String dateCol = firstExistingColumn("bookings", "journey_date", "travel_date", "date_of_journey", "journey", "journeyDate");
        String statusCol = firstExistingColumn("bookings", "status", "booking_status", "state");
        String createdCol = firstExistingColumn("bookings", "created_at");
        String amountCol = firstExistingColumn("bookings", "amount", "total_amount", "fare", "price", "ticket_amount", "total");
        String fromStopIdCol = firstExistingColumn("bookings", "from_stop_id", "src_stop_id");
        String toStopIdCol = firstExistingColumn("bookings", "to_stop_id", "dst_stop_id");
        String passengerNameCol = firstExistingColumn("bookings", "passenger_name", "pname", "customer_name");
        String passengerPhoneCol = firstExistingColumn("bookings", "passenger_phone", "phone", "mobile", "contact_no");
        String passengerEmailCol = firstExistingColumn("bookings", "passenger_email", "email", "mail");

        Integer fromStopId = resolveRouteStopId(scheduleId, fromCity);
        Integer toStopId = resolveRouteStopId(scheduleId, toCity);

        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        addColumn(columns, values, userCol, userId);
        addColumn(columns, values, routeCol, routeId);
        addColumn(columns, values, scheduleCol, scheduleId);
        addColumn(columns, values, fromCityCol, fromCity);
        addColumn(columns, values, toCityCol, toCity);
        addColumn(columns, values, fromOrderCol, fromOrder);
        addColumn(columns, values, toOrderCol, toOrder);
        addColumn(columns, values, fromStopIdCol, fromStopId);
        addColumn(columns, values, toStopIdCol, toStopId);
        addColumn(columns, values, seatCol, seatNo);
        addColumn(columns, values, amountCol, amount);
        addColumn(columns, values, passengerNameCol, passengerName);
        addColumn(columns, values, passengerPhoneCol, passengerPhone);
        addColumn(columns, values, passengerEmailCol, passengerEmail);
        if (dateCol != null) {
            columns.add(dateCol);
            values.add(toSqlDate(journeyDate));
        }
        addColumn(columns, values, statusCol, "CONFIRMED");
        if (createdCol != null) {
            columns.add(createdCol);
            values.add(new Timestamp(System.currentTimeMillis()));
        }

        if (columns.isEmpty()) {
            lastErrorMessage = "No writable booking columns found";
            return -1;
        }
        if (userCol == null || seatCol == null) {
            lastErrorMessage = "Required booking columns missing (user/seat)";
            return -1;
        }

        // Prevent duplicate booking row for the same user/schedule/seat/date
        StringBuilder checkSql = new StringBuilder("SELECT id FROM bookings WHERE " + userCol + "=?");
        List<Object> checkVals = new ArrayList<>();
        checkVals.add(userId);
        if (scheduleCol != null) {
            checkSql.append(" AND ").append(scheduleCol).append("=?");
            checkVals.add(scheduleId);
        } else if (routeCol != null) {
            checkSql.append(" AND ").append(routeCol).append("=?");
            checkVals.add(routeId);
        }
        if (seatCol != null) {
            checkSql.append(" AND ").append(seatCol).append("=?");
            checkVals.add(seatNo);
        }
        if (dateCol != null) {
            checkSql.append(" AND ").append(dateCol).append("=?");
            checkVals.add(toSqlDate(journeyDate));
        }
        if (statusCol != null) {
            checkSql.append(" AND ").append(statusCol).append("='CONFIRMED'");
        }
        checkSql.append(" ORDER BY id DESC LIMIT 1");

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(checkSql.toString())) {
            for (int i = 0; i < checkVals.size(); i++) {
                ps.setObject(i + 1, checkVals.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (Exception ignored) {
            // best-effort duplicate prevention
        }

        StringBuilder sql = new StringBuilder("INSERT INTO bookings (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append(columns.get(i));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString(),
                     Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }

            int affected = ps.executeUpdate();

            if (affected > 0) {

                ResultSet rs = ps.getGeneratedKeys();

                if (rs.next()) {

                    int bookingId = rs.getInt(1);

                    // Insert into booked_seats
                    insertBookedSeat(bookingId, scheduleId, seatNo, fromOrder, toOrder);

                    return bookingId;
                }
            }

        } catch (Exception e) {
            lastErrorMessage = e.getMessage();
            e.printStackTrace();
        }

        return -1;
    }

    /* ================= INSERT BOOKED SEAT ================= */

    public void insertBookedSeat(int bookingId,
                                 int scheduleId,
                                 String seatNo,
                                 int fromOrder,
                                 int toOrder) {

        if (!tableExists("booked_seats")) return;

        String bookingCol = firstExistingColumn("booked_seats", "booking_id", "booking");
        String scheduleCol = firstExistingColumn("booked_seats", "schedule_id", "schedule", "scheduleid", "sid");
        String seatCol = firstExistingColumn("booked_seats", "seat_no", "seat_number", "seat", "seatno", "seat_num");
        String fromOrderCol = firstExistingColumn("booked_seats", "from_order", "from_stop_order", "src_order", "fromorder", "srcorder");
        String toOrderCol = firstExistingColumn("booked_seats", "to_order", "to_stop_order", "dst_order", "toorder", "dstorder");

        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        addColumn(columns, values, bookingCol, bookingId);
        addColumn(columns, values, scheduleCol, scheduleId);
        addColumn(columns, values, seatCol, seatNo);
        addColumn(columns, values, fromOrderCol, fromOrder);
        addColumn(columns, values, toOrderCol, toOrder);

        if (scheduleCol == null || seatCol == null) return;
        if (columns.isEmpty()) return;

        StringBuilder sql = new StringBuilder("INSERT INTO booked_seats (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append(columns.get(i));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= CANCEL ================= */

    public boolean cancelBooking(int bookingId) {

        lastErrorMessage = "";
        if (!tableExists("bookings")) {
            lastErrorMessage = "bookings table not found";
            return false;
        }

        String statusCol = firstExistingColumn("bookings", "status", "booking_status");
        String flagCol = firstExistingColumn("bookings", "is_cancelled", "cancelled");
        String cancelledAtCol = firstExistingColumn("bookings", "cancelled_at");

        String cancelSql;
        if (statusCol != null) {
            cancelSql = "UPDATE bookings SET " + statusCol + "='CANCELLED' WHERE id=?";
        } else if (flagCol != null) {
            cancelSql = "UPDATE bookings SET " + flagCol + "=1 WHERE id=?";
        } else if (cancelledAtCol != null) {
            cancelSql = "UPDATE bookings SET " + cancelledAtCol + "=NOW() WHERE id=?";
        } else {
            lastErrorMessage = "No cancellation column available in bookings table";
            return false;
        }

        boolean canReleaseBookedSeats = tableExists("booked_seats") && columnExists("booked_seats", "booking_id");
        String releaseSql = "DELETE FROM booked_seats WHERE booking_id=?";

        try (Connection con = DBConfig.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement psCancel = con.prepareStatement(cancelSql)) {

                psCancel.setInt(1, bookingId);
                int updated = psCancel.executeUpdate();
                if (updated <= 0) {
                    con.rollback();
                    lastErrorMessage = "Booking not found for cancellation";
                    return false;
                }

                if (canReleaseBookedSeats) {
                    try (PreparedStatement psRelease = con.prepareStatement(releaseSql)) {
                        psRelease.setInt(1, bookingId);
                        psRelease.executeUpdate();
                    }
                }

                con.commit();
                return true;
            } catch (Exception ex) {
                con.rollback();
                lastErrorMessage = ex.getMessage();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception e) {
            lastErrorMessage = e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    /* ================= AMOUNT ================= */

    public double getBookingAmount(int bookingId) {

        String amountCol = firstExistingColumn("bookings", "amount", "total_amount", "fare", "price", "ticket_amount");
        if (amountCol == null) return 0;

        String sql =
                "SELECT " + amountCol + " AS amount_value FROM bookings WHERE id=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, bookingId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("amount_value");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /* ================= TICKET PREVIEW ================= */

    public int getLatestConfirmedBookingId(int userId) {
        if (userId <= 0 || !tableExists("bookings")) return -1;
        String userCol = firstExistingColumn("bookings", "user_id", "userid", "uid", "user", "customer_id");
        String statusCol = firstExistingColumn("bookings", "status", "booking_status");
        String createdCol = firstExistingColumn("bookings", "created_at", "booking_time");
        String dateCol = firstExistingColumn("bookings", "journey_date", "travel_date");
        if (userCol == null) return -1;

        String orderBy = createdCol != null ? createdCol : (dateCol != null ? dateCol : "id");
        String sql =
                "SELECT id FROM bookings WHERE " + userCol + "=? " +
                (statusCol != null ? "AND " + statusCol + "='CONFIRMED' " : "") +
                "ORDER BY " + orderBy + " DESC LIMIT 1";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception ignored) {
            // no-op
        }
        return -1;
    }

    public int getLatestBookingId(int userId) {
        if (userId <= 0 || !tableExists("bookings")) return -1;
        String userCol = firstExistingColumn("bookings", "user_id", "userid", "uid", "user", "customer_id");
        String createdCol = firstExistingColumn("bookings", "created_at", "booking_time");
        String dateCol = firstExistingColumn("bookings", "journey_date", "travel_date");
        if (userCol == null) return -1;

        String orderBy = createdCol != null ? createdCol : (dateCol != null ? dateCol : "id");
        String sql = "SELECT id FROM bookings WHERE " + userCol + "=? ORDER BY " + orderBy + " DESC LIMIT 1";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception ignored) {
            // no-op
        }
        return -1;
    }

    private String getById(String table, String idCol, String valueCol, int id) {
        if (table == null || idCol == null || valueCol == null) return "";
        String sql = "SELECT " + valueCol + " AS v FROM " + table + " WHERE " + idCol + "=? LIMIT 1";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String v = rs.getString("v");
                    return v == null ? "" : v;
                }
            }
        } catch (Exception ignored) {
            // best-effort lookup
        }
        return "";
    }

    private String resolveUserName(int userId) {
        if (userId <= 0) return "";
        String nameCol = firstExistingColumn("users", "name", "full_name", "username", "user_name");
        return getById("users", "id", nameCol, userId);
    }

    private String resolveRouteNameById(int routeId) {
        if (routeId <= 0) return "";
        String routeNameCol = firstExistingColumn("routes", "route_name", "name", "route");
        return getById("routes", "id", routeNameCol, routeId);
    }

    private String resolveRouteNameByScheduleId(int scheduleId) {
        if (scheduleId <= 0) return "";
        String scheduleRouteCol = firstExistingColumn("schedules", "route_id");
        String routeNameCol = firstExistingColumn("routes", "route_name", "name", "route");
        if (scheduleRouteCol == null || routeNameCol == null) return "";

        String sql =
                "SELECT r." + routeNameCol + " AS route_name " +
                "FROM schedules s JOIN routes r ON s." + scheduleRouteCol + " = r.id " +
                "WHERE s.id=? LIMIT 1";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("route_name");
            }
        } catch (Exception ignored) {
            // best-effort lookup
        }
        return "";
    }

    private String resolveDepartureByScheduleId(int scheduleId) {
        if (scheduleId <= 0) return "";
        String departureCol = firstExistingColumn("schedules", "departure_time", "start_time", "depart_at");
        return getById("schedules", "id", departureCol, scheduleId);
    }

    public String[] getTicketPreviewData(int bookingId) {

        if (!tableExists("bookings")) return null;

        String userIdCol = firstExistingColumn("bookings", "user_id", "uid");
        String scheduleCol = firstExistingColumn("bookings", "schedule_id", "schedule");
        String routeCol = firstExistingColumn("bookings", "route_id");
        String fromCol = firstExistingColumn("bookings", "from_city", "from_stop");
        String toCol = firstExistingColumn("bookings", "to_city", "to_stop");
        String seatCol = firstExistingColumn("bookings", "seat_no", "seat_number", "seat");
        String dateCol = firstExistingColumn("bookings", "journey_date", "travel_date");
        String amountCol = firstExistingColumn("bookings", "amount", "total_amount", "fare", "price", "ticket_amount");
        String passengerNameCol = firstExistingColumn("bookings", "passenger_name", "pname", "customer_name");
        String passengerPhoneCol = firstExistingColumn("bookings", "passenger_phone", "phone", "mobile", "contact_no");
        String passengerEmailCol = firstExistingColumn("bookings", "passenger_email", "email", "mail");
        String sql =
                "SELECT id," +
                (userIdCol != null ? userIdCol : "NULL") + " AS uid," +
                (scheduleCol != null ? scheduleCol : "NULL") + " AS sid," +
                (routeCol != null ? routeCol : "NULL") + " AS rid," +
                (fromCol != null ? fromCol : "''") + " AS src," +
                (toCol != null ? toCol : "''") + " AS dst," +
                (seatCol != null ? seatCol : "''") + " AS seat_value," +
                (amountCol != null ? amountCol : "0") + " AS amount_value," +
                (dateCol != null ? dateCol : "NULL") + " AS jdate," +
                (passengerNameCol != null ? passengerNameCol : "''") + " AS p_name," +
                (passengerPhoneCol != null ? passengerPhoneCol : "''") + " AS p_phone," +
                (passengerEmailCol != null ? passengerEmailCol : "''") + " AS p_email " +
                "FROM bookings WHERE id=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, bookingId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int uid = rs.getInt("uid");
                int sid = rs.getInt("sid");
                int rid = rs.getInt("rid");

                String passenger = rs.getString("p_name");
                if (passenger == null || passenger.isBlank()) {
                    passenger = resolveUserName(uid);
                }
                String routeName = resolveRouteNameById(rid);
                if (routeName == null || routeName.isBlank()) {
                    routeName = resolveRouteNameByScheduleId(sid);
                }
                String departure = resolveDepartureByScheduleId(sid);

                return new String[]{
                        rs.getString("id"),
                        passenger == null ? "" : passenger,
                        routeName == null ? "" : routeName,
                        rs.getString("src"),
                        rs.getString("dst"),
                        rs.getString("seat_value"),
                        rs.getString("amount_value"),
                        rs.getString("jdate"),
                        departure == null ? "" : departure,
                        rs.getString("p_phone"),
                        rs.getString("p_email")
                };
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}


