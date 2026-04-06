package dao;

import config.DBConfig;
import util.SchemaCache;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.StringJoiner;

public class BookingDAO {

    private static final Object SCHEMA_LOCK = new Object();
    private static volatile boolean schemaCompatibilityEnsured = false;
    private final Map<String, Integer> routeStopCache = new HashMap<>();
    private volatile String lastErrorMessage = "";

    private boolean tableExists(String tableName) {
        return SchemaCache.tableExists(tableName);
    }

    private boolean columnExists(String tableName, String columnName) {
        return SchemaCache.columnExists(tableName, columnName);
    }

    private String firstExistingColumn(String table, String... candidates) {
        return SchemaCache.firstExistingColumn(table, candidates);
    }

    private void ensureColumn(String table, String column, String ddlType) {
        if (!tableExists(table) || columnExists(table, column)) return;
        String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddlType;
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
            SchemaCache.markColumnPresent(table, column);
        } catch (Exception ignored) {
            
        }
    }

    private void ensureBookingSchemaCompatibility() {
        if (schemaCompatibilityEnsured) return;

        synchronized (SCHEMA_LOCK) {
            if (schemaCompatibilityEnsured) return;
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
            ensureColumn("bookings", "booking_ref", "VARCHAR(80) NULL");
            ensureColumn("bookings", "status", "VARCHAR(24) NULL");
            ensureColumn("bookings", "created_at", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP");

            if (tableExists("booked_seats")) {
                ensureColumn("booked_seats", "booking_id", "INT NULL");
                ensureColumn("booked_seats", "schedule_id", "INT NULL");
                ensureColumn("booked_seats", "seat_no", "VARCHAR(24) NULL");
                ensureColumn("booked_seats", "from_order", "INT NULL");
                ensureColumn("booked_seats", "to_order", "INT NULL");
            }

            schemaCompatibilityEnsured = true;
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

    private static class BookingHistoryEntry {
        private int id;
        private int scheduleId;
        private String bookingRef = "";
        private String createdValue = "";
        private String src = "";
        private String dst = "";
        private String seatValue = "";
        private double amount;
        private String journeyDate = "";
        private String status = "CONFIRMED";
    }

    private static class BookingHistoryGroup {
        private final List<Integer> bookingIds = new ArrayList<>();
        private final List<String> seatValues = new ArrayList<>();
        private String displayRef = "";
        private String src = "";
        private String dst = "";
        private String journeyDate = "";
        private String status = "CONFIRMED";
        private double totalAmount;
    }

    private BookingHistoryEntry readHistoryEntry(ResultSet rs) throws SQLException {
        BookingHistoryEntry entry = new BookingHistoryEntry();
        entry.id = rs.getInt("id");
        entry.scheduleId = rs.getInt("schedule_value");
        entry.bookingRef = safeText(rs.getString("booking_ref_value"));
        entry.createdValue = safeText(rs.getString("created_value"));
        entry.src = safeText(rs.getString("src"));
        entry.dst = safeText(rs.getString("dst"));
        entry.seatValue = safeText(rs.getString("seat_value"));
        entry.amount = rs.getDouble("amount_value");
        entry.journeyDate = safeText(rs.getString("jdate"));
        String rawStatus = safeText(rs.getString("status"));
        entry.status = rawStatus.isBlank() ? "CONFIRMED" : rawStatus.toUpperCase(Locale.ENGLISH);
        return entry;
    }

    private String bookingHistoryGroupKey(BookingHistoryEntry entry) {
        if (!entry.bookingRef.isBlank()) {
            return "REF|" + entry.bookingRef + "|" + entry.status;
        }
        if (!entry.createdValue.isBlank()) {
            return "LEGACY|" +
                    entry.scheduleId + "|" +
                    entry.src.toUpperCase(Locale.ENGLISH) + "|" +
                    entry.dst.toUpperCase(Locale.ENGLISH) + "|" +
                    entry.journeyDate + "|" +
                    entry.status + "|" +
                    entry.createdValue;
        }
        return "ROW|" + entry.id;
    }

    private void appendHistoryEntry(BookingHistoryGroup group, BookingHistoryEntry entry) {
        group.bookingIds.add(entry.id);
        if (!entry.seatValue.isBlank()) {
            group.seatValues.add(entry.seatValue);
        }
        if (group.displayRef.isBlank()) {
            group.displayRef = !entry.bookingRef.isBlank() ? entry.bookingRef : String.valueOf(entry.id);
        }
        if (group.src.isBlank()) group.src = entry.src;
        if (group.dst.isBlank()) group.dst = entry.dst;
        if (group.journeyDate.isBlank()) group.journeyDate = entry.journeyDate;
        if (group.status.isBlank()) group.status = entry.status;
        group.totalAmount += entry.amount;
    }

    private String[] toHistoryRow(BookingHistoryGroup group) {
        group.seatValues.sort(Comparator.comparingInt(this::seatSortKey).thenComparing(String::compareToIgnoreCase));
        StringJoiner idJoiner = new StringJoiner(",");
        for (Integer id : group.bookingIds) {
            idJoiner.add(String.valueOf(id));
        }

        String displayRef;
        if (group.bookingIds.size() > 1) {
            displayRef = group.bookingIds.get(0) + "-" + group.bookingIds.get(group.bookingIds.size() - 1);
        } else if (!group.bookingIds.isEmpty()) {
            displayRef = String.valueOf(group.bookingIds.get(0));
        } else if (group.displayRef != null && !group.displayRef.isBlank()) {
            displayRef = group.displayRef;
        } else {
            displayRef = "-";
        }

        return new String[]{
                displayRef,
                group.src,
                group.dst,
                String.join(", ", group.seatValues),
                String.format(Locale.ENGLISH, "%.2f", group.totalAmount),
                group.journeyDate,
                group.status,
                idJoiner.toString()
        };
    }

    

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
            
        }

        return "No active booking";
    }

    

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
        String bookingRefCol = firstExistingColumn("bookings", "booking_ref", "order_ref", "order_id", "booking_code");
        String scheduleCol = firstExistingColumn("bookings", "schedule_id", "schedule", "scheduleid", "sid", "trip_id", "tripid");
        String createdCol = firstExistingColumn("bookings", "created_at", "booking_time");

        if (fromCol == null || toCol == null || dateCol == null || seatCol == null || userCol == null) {
            return list;
        }
        String amountExpr = amountCol != null ? amountCol : "0";

        String sql =
                "SELECT id," + fromCol + " AS src," + toCol + " AS dst," +
                seatCol + " AS seat_value," + amountExpr + " AS amount_value," + dateCol + " AS jdate," +
                (statusCol != null ? statusCol : "'CONFIRMED'") + " AS status," +
                (bookingRefCol != null ? bookingRefCol : "''") + " AS booking_ref_value," +
                (scheduleCol != null ? scheduleCol : "0") + " AS schedule_value," +
                (createdCol != null ? createdCol : "NULL") + " AS created_value " +
                "FROM bookings WHERE " + userCol + "=? ORDER BY " + (createdCol != null ? createdCol : dateCol) + " DESC, id DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                Map<String, BookingHistoryGroup> groups = new LinkedHashMap<>();
                while (rs.next()) {
                    BookingHistoryEntry entry = readHistoryEntry(rs);
                    String groupKey = bookingHistoryGroupKey(entry);
                    BookingHistoryGroup group = groups.computeIfAbsent(groupKey, key -> new BookingHistoryGroup());
                    appendHistoryEntry(group, entry);
                }

                for (BookingHistoryGroup group : groups.values()) {
                    list.add(toHistoryRow(group));
                }
            }

        } catch (Exception e) {
            
        }

        return list;
    }

    public int countBookingsByUser(int userId) {
        if (userId <= 0 || !tableExists("bookings")) {
            return 0;
        }

        String userCol = firstExistingColumn("bookings", "user_id", "uid");
        String bookingRefCol = firstExistingColumn("bookings", "booking_ref", "order_ref", "order_id", "booking_code");
        if (userCol == null) {
            return 0;
        }

        String sql;
        if (bookingRefCol != null) {
            sql =
                    "SELECT COUNT(*) FROM (" +
                    "SELECT CASE " +
                    "WHEN " + bookingRefCol + " IS NOT NULL AND TRIM(" + bookingRefCol + ") <> '' " +
                    "THEN CONCAT('REF|', TRIM(" + bookingRefCol + ")) " +
                    "ELSE CONCAT('ID|', id) END AS booking_key " +
                    "FROM bookings WHERE " + userCol + "=? " +
                    "GROUP BY booking_key" +
                    ") grouped_bookings";
        } else {
            sql = "SELECT COUNT(*) FROM bookings WHERE " + userCol + "=?";
        }

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean deleteBookingHistoryForUser(int bookingId, int userId) {
        if (bookingId <= 0 || userId <= 0 || !tableExists("bookings")) return false;

        String userCol = firstExistingColumn("bookings", "user_id", "uid", "user", "userid", "customer_id");
        String statusCol = firstExistingColumn("bookings", "status", "booking_status", "state");
        if (userCol == null) return false;

        
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

    private List<Integer> normalizeBookingIds(Collection<Integer> bookingIds) {
        List<Integer> normalized = new ArrayList<>();
        if (bookingIds == null) return normalized;
        for (Integer bookingId : bookingIds) {
            if (bookingId != null && bookingId > 0 && !normalized.contains(bookingId)) {
                normalized.add(bookingId);
            }
        }
        return normalized;
    }

    public boolean deleteBookingHistoryForUser(Collection<Integer> bookingIds, int userId) {
        List<Integer> normalized = normalizeBookingIds(bookingIds);
        if (normalized.isEmpty() || userId <= 0 || !tableExists("bookings")) return false;

        String userCol = firstExistingColumn("bookings", "user_id", "uid", "user", "userid", "customer_id");
        String statusCol = firstExistingColumn("bookings", "status", "booking_status", "state");
        if (userCol == null) return false;

        StringBuilder sql = new StringBuilder("DELETE FROM bookings WHERE " + userCol + "=? AND id IN (");
        for (int i = 0; i < normalized.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");
        if (statusCol != null) {
            sql.append(" AND UPPER(").append(statusCol).append(") <> 'CONFIRMED'");
        }

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setInt(1, userId);
            for (int i = 0; i < normalized.size(); i++) {
                ps.setInt(i + 2, normalized.get(i));
            }
            return ps.executeUpdate() == normalized.size();
        } catch (Exception e) {
            return false;
        }
    }

    

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
        return insertBooking(
                userId,
                routeId,
                scheduleId,
                fromCity,
                toCity,
                fromOrder,
                toOrder,
                seatNo,
                amount,
                journeyDate,
                passengerName,
                passengerPhone,
                passengerEmail,
                null
        );
    }

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
            String passengerEmail,
            String bookingRef) {

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
        String bookingRefCol = firstExistingColumn("bookings", "booking_ref", "order_ref", "order_id", "booking_code");

        Integer fromStopId = resolveRouteStopId(scheduleId, fromCity);
        Integer toStopId = resolveRouteStopId(scheduleId, toCity);
        String normalizedBookingRef = safeText(bookingRef);

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
        addColumn(columns, values, bookingRefCol, normalizedBookingRef.isBlank() ? null : normalizedBookingRef);
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

    public boolean cancelBookings(Collection<Integer> bookingIds) {
        List<Integer> normalized = normalizeBookingIds(bookingIds);
        if (normalized.isEmpty()) return false;

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

            try (PreparedStatement psCancel = con.prepareStatement(cancelSql);
                 PreparedStatement psRelease = canReleaseBookedSeats ? con.prepareStatement(releaseSql) : null) {

                for (Integer bookingId : normalized) {
                    psCancel.setInt(1, bookingId);
                    int updated = psCancel.executeUpdate();
                    if (updated <= 0) {
                        con.rollback();
                        lastErrorMessage = "Booking not found for cancellation";
                        return false;
                    }

                    if (psRelease != null) {
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

    public double getBookingAmount(Collection<Integer> bookingIds) {
        List<Integer> normalized = normalizeBookingIds(bookingIds);
        if (normalized.isEmpty()) return 0;

        String amountCol = firstExistingColumn("bookings", "amount", "total_amount", "fare", "price", "ticket_amount");
        if (amountCol == null) return 0;

        StringBuilder sql = new StringBuilder(
                "SELECT IFNULL(SUM(" + amountCol + "),0) AS amount_value FROM bookings WHERE id IN ("
        );
        for (int i = 0; i < normalized.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < normalized.size(); i++) {
                ps.setInt(i + 1, normalized.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("amount_value");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    

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
            
        }
        return "";
    }

    private String resolveDepartureByScheduleId(int scheduleId) {
        if (scheduleId <= 0) return "";
        String departureCol = firstExistingColumn("schedules", "departure_time", "start_time", "depart_at");
        return getById("schedules", "id", departureCol, scheduleId);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private double parseAmount(String raw) {
        try {
            return Double.parseDouble(safeText(raw));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int seatSortKey(String seatNo) {
        try {
            return Integer.parseInt(safeText(seatNo).replaceAll("\\D", ""));
        } catch (Exception ignored) {
            return Integer.MAX_VALUE;
        }
    }

    public String[] getCombinedTicketPreviewData(List<Integer> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) return null;

        List<Integer> normalizedIds = new ArrayList<>();
        List<String> seatValues = new ArrayList<>();
        String passenger = "";
        String routeName = "";
        String src = "";
        String dst = "";
        String journeyDate = "";
        String departure = "";
        String phone = "";
        String email = "";
        double totalAmount = 0;

        for (Integer bookingId : bookingIds) {
            if (bookingId == null || bookingId <= 0) continue;

            String[] row = getTicketPreviewData(bookingId);
            if (row == null || row.length < 11) continue;

            normalizedIds.add(bookingId);

            if (passenger.isBlank()) passenger = safeText(row[1]);
            if (routeName.isBlank()) routeName = safeText(row[2]);
            if (src.isBlank()) src = safeText(row[3]);
            if (dst.isBlank()) dst = safeText(row[4]);
            if (journeyDate.isBlank()) journeyDate = safeText(row[7]);
            if (departure.isBlank()) departure = safeText(row[8]);
            if (phone.isBlank()) phone = safeText(row[9]);
            if (email.isBlank()) email = safeText(row[10]);

            String seatValue = safeText(row[5]);
            if (!seatValue.isBlank()) {
                seatValues.add(seatValue);
            }

            totalAmount += parseAmount(row[6]);
        }

        if (normalizedIds.isEmpty()) return null;

        seatValues.sort(Comparator.comparingInt(this::seatSortKey).thenComparing(String::compareToIgnoreCase));

        int firstId = normalizedIds.get(0);
        int lastId = normalizedIds.get(normalizedIds.size() - 1);
        String ticketRef = normalizedIds.size() == 1
                ? String.valueOf(firstId)
                : firstId + "-" + lastId;

        return new String[]{
                ticketRef,
                passenger,
                routeName,
                src,
                dst,
                String.join(", ", seatValues),
                String.format(Locale.ENGLISH, "%.2f", totalAmount),
                journeyDate,
                departure,
                phone,
                email
        };
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


