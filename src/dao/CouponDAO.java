package dao;

import util.DBConnectionUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CouponDAO {

    private static final Object SCHEMA_LOCK = new Object();
    private static volatile boolean schemaReady = false;

    private final ThreadLocal<String> lastError = new ThreadLocal<>();
    private final Map<String, Boolean> tableCache = new HashMap<>();
    private final Map<String, Boolean> columnCache = new HashMap<>();

    public void clearLastError() {
        lastError.remove();
    }

    public String getLastError() {
        String message = lastError.get();
        return message == null ? "" : message;
    }

    private void setLastError(Exception error) {
        lastError.set(DBConnectionUtil.userMessage(error));
    }

    private void setLastError(String message) {
        if (message == null || message.isBlank()) {
            lastError.remove();
            return;
        }
        lastError.set(message.trim());
    }

    private boolean tableExists(String tableName) {
        if (tableCache.containsKey(tableName)) {
            return tableCache.get(tableName);
        }

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
            setLastError(e);
            return false;
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        String key = tableName + "." + columnName;
        if (columnCache.containsKey(key)) {
            return columnCache.get(key);
        }

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
            setLastError(e);
            return false;
        }
    }

    private void ensureColumn(String table, String column, String ddlType) {
        if (!tableExists(table) || columnExists(table, column)) {
            return;
        }

        String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddlType;
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
            columnCache.put(table + "." + column, true);
        } catch (Exception e) {
            setLastError(e);
        }
    }

    private synchronized void ensureCouponSchema() {
        if (schemaReady) {
            return;
        }

        synchronized (SCHEMA_LOCK) {
            if (schemaReady) {
                return;
            }
            if (!tableExists("coupons")) {
                return;
            }

            ensureColumn("coupons", "min_booking_amount", "DECIMAL(10,2) DEFAULT 0.00");
            ensureColumn("coupons", "max_uses", "INT NULL");
            ensureColumn("coupons", "per_user_limit", "INT DEFAULT 1");
            ensureColumn("coupons", "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

            String createUsageSql =
                    "CREATE TABLE IF NOT EXISTS coupon_usage (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "coupon_id INT NOT NULL," +
                    "user_id INT NOT NULL," +
                    "order_ref VARCHAR(80) NOT NULL," +
                    "coupon_code VARCHAR(50) NOT NULL," +
                    "discount_amount DECIMAL(10,2) NOT NULL," +
                    "base_amount DECIMAL(10,2) NOT NULL," +
                    "final_amount DECIMAL(10,2) NOT NULL," +
                    "status VARCHAR(20) NOT NULL DEFAULT 'APPLIED'," +
                    "used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE KEY uq_coupon_order (coupon_id, order_ref)" +
                    ")";

            try (Connection con = DBConnectionUtil.getConnection();
                 PreparedStatement ps = con.prepareStatement(createUsageSql)) {
                ps.executeUpdate();
                tableCache.put("coupon_usage", true);
            } catch (Exception e) {
                setLastError(e);
            }

            if (tableExists("coupon_usage")) {
                ensureColumn("coupon_usage", "coupon_code", "VARCHAR(50) NOT NULL DEFAULT ''");
                ensureColumn("coupon_usage", "discount_amount", "DECIMAL(10,2) NOT NULL DEFAULT 0.00");
                ensureColumn("coupon_usage", "base_amount", "DECIMAL(10,2) NOT NULL DEFAULT 0.00");
                ensureColumn("coupon_usage", "final_amount", "DECIMAL(10,2) NOT NULL DEFAULT 0.00");
                ensureColumn("coupon_usage", "status", "VARCHAR(20) NOT NULL DEFAULT 'APPLIED'");
                ensureColumn("coupon_usage", "used_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            }

            schemaReady = true;
        }
    }

    private String[] buildRow(ResultSet rs) throws Exception {
        return new String[]{
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("discount_amount"),
                rs.getString("expiry_date"),
                rs.getInt("active") == 1 ? "ACTIVE" : "INACTIVE"
        };
    }

    private CouponOffer buildOffer(ResultSet rs) throws Exception {
        CouponOffer offer = new CouponOffer();
        offer.id = rs.getInt("id");
        offer.code = rs.getString("code");
        offer.discountAmount = rs.getDouble("discount_amount");
        offer.minBookingAmount = rs.getDouble("min_booking_amount");
        int maxUses = rs.getInt("max_uses");
        offer.maxUses = rs.wasNull() ? null : maxUses;
        int perUserLimit = rs.getInt("per_user_limit");
        offer.perUserLimit = perUserLimit <= 0 ? 1 : perUserLimit;
        offer.expiryDate = rs.getDate("expiry_date");
        offer.active = rs.getInt("active") == 1;
        offer.totalUsed = rs.getInt("total_used");
        offer.userUsed = rs.getInt("user_used");
        return offer;
    }

    private List<CouponOffer> loadOffers(int userId, String codeFilter, int limit) {
        ensureCouponSchema();
        List<CouponOffer> offers = new ArrayList<>();
        if (!tableExists("coupons")) {
            return offers;
        }

        String limitClause = limit > 0 ? " LIMIT " + limit : "";
        String sql =
                "SELECT c.id, c.code, c.discount_amount, " +
                "COALESCE(c.min_booking_amount, 0) AS min_booking_amount, " +
                "c.max_uses, COALESCE(c.per_user_limit, 1) AS per_user_limit, " +
                "c.expiry_date, CASE WHEN c.active IS NULL THEN 1 ELSE c.active END AS active, " +
                (tableExists("coupon_usage")
                        ? "COALESCE((SELECT COUNT(*) FROM coupon_usage cu WHERE cu.coupon_id = c.id AND UPPER(COALESCE(cu.status, 'APPLIED')) = 'APPLIED'), 0) AS total_used, " +
                          "COALESCE((SELECT COUNT(*) FROM coupon_usage cu WHERE cu.coupon_id = c.id AND cu.user_id = ? AND UPPER(COALESCE(cu.status, 'APPLIED')) = 'APPLIED'), 0) AS user_used "
                        : "0 AS total_used, 0 AS user_used ") +
                "FROM coupons c " +
                (codeFilter == null || codeFilter.isBlank() ? "" : "WHERE UPPER(TRIM(c.code)) = UPPER(TRIM(?)) ") +
                "ORDER BY COALESCE(c.discount_amount, 0) DESC, c.code ASC" +
                limitClause;

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int index = 1;
            if (tableExists("coupon_usage")) {
                ps.setInt(index++, userId);
            }
            if (codeFilter != null && !codeFilter.isBlank()) {
                ps.setString(index, codeFilter.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    offers.add(buildOffer(rs));
                }
            }
        } catch (Exception e) {
            setLastError(e);
            DBConnectionUtil.logIfUnexpected(e);
        }

        return offers;
    }

    private CouponValidation evaluateOffer(CouponOffer offer, double baseAmount) {
        CouponValidation result = new CouponValidation();
        result.offer = offer;

        if (offer == null) {
            result.valid = false;
            result.message = "Invalid coupon.";
            return result;
        }

        boolean expired = offer.expiryDate != null && offer.expiryDate.toLocalDate().isBefore(LocalDate.now());
        boolean exhausted = offer.maxUses != null && offer.totalUsed >= offer.maxUses;
        boolean userLimitReached = offer.userUsed >= Math.max(1, offer.perUserLimit);
        boolean minAmountNotMet = baseAmount + 0.001 < Math.max(0, offer.minBookingAmount);

        offer.expired = expired;
        offer.exhausted = exhausted;
        offer.usedUpByUser = userLimitReached;
        offer.eligible = offer.active && !expired && !exhausted && !userLimitReached && !minAmountNotMet;

        if (!offer.active) {
            offer.badgeText = "Inactive";
            offer.helperText = "This coupon is currently disabled.";
            result.valid = false;
            result.message = offer.helperText;
            return result;
        }
        if (expired) {
            offer.badgeText = "Expired";
            offer.helperText = "This coupon has expired.";
            result.valid = false;
            result.message = offer.helperText;
            return result;
        }
        if (exhausted) {
            offer.badgeText = "Claimed";
            offer.helperText = "This coupon has reached its overall usage limit.";
            result.valid = false;
            result.message = offer.helperText;
            return result;
        }
        if (userLimitReached) {
            offer.badgeText = "Used";
            offer.helperText = "You have already used this coupon.";
            result.valid = false;
            result.message = offer.helperText;
            return result;
        }
        if (minAmountNotMet) {
            offer.badgeText = "Unlock";
            offer.helperText = "Valid on bookings above INR " + String.format("%.2f", offer.minBookingAmount);
            result.valid = false;
            result.message = offer.helperText;
            return result;
        }

        offer.badgeText = baseAmount >= Math.max(700.0, offer.minBookingAmount) && offer.discountAmount >= 50
                ? "Long Trip"
                : "Available";
        offer.helperText = "Save INR " + String.format("%.2f", offer.discountAmount) + " on this booking.";
        result.valid = true;
        result.message = "Coupon applied.";
        return result;
    }

    public CouponValidation validateCouponForUser(String code, int userId, double baseAmount) {
        clearLastError();
        if (code == null || code.isBlank()) {
            setLastError("Enter a coupon code first.");
            return CouponValidation.fail(getLastError());
        }

        List<CouponOffer> offers = loadOffers(userId, code.trim().toUpperCase(), 1);
        if (offers.isEmpty()) {
            return CouponValidation.fail("Invalid or expired coupon.");
        }

        CouponValidation validation = evaluateOffer(offers.get(0), baseAmount);
        if (!validation.valid) {
            setLastError(validation.message);
        }
        return validation;
    }

    public List<CouponOffer> getPersonalizedOffers(int userId, double baseAmount, int limit) {
        clearLastError();
        List<CouponOffer> offers = loadOffers(userId, null, 0);
        List<CouponOffer> evaluated = new ArrayList<>();

        for (CouponOffer offer : offers) {
            evaluateOffer(offer, baseAmount);
            evaluated.add(offer);
        }

        evaluated.sort(Comparator
                .comparing((CouponOffer offer) -> !offer.eligible)
                .thenComparing(offer -> offer.usedUpByUser)
                .thenComparing(offer -> offer.expired)
                .thenComparing((CouponOffer offer) -> -offer.discountAmount)
                .thenComparing(offer -> offer.code == null ? "" : offer.code));

        if (limit > 0 && evaluated.size() > limit) {
            return new ArrayList<>(evaluated.subList(0, limit));
        }
        return evaluated;
    }

    public List<String[]> getNotificationItemsForUser(int userId, int limit) {
        List<String[]> items = new ArrayList<>();
        int safeLimit = Math.max(1, limit);
        List<CouponOffer> offers = getPersonalizedOffers(userId, 0, safeLimit * 3);

        for (CouponOffer offer : offers) {
            if (offer == null || offer.code == null || offer.code.isBlank()) {
                continue;
            }

            if (offer.active && !offer.expired && !offer.exhausted && !offer.usedUpByUser) {
                String message;
                if (offer.minBookingAmount > 0) {
                    message = "Offer: " + offer.code + " gives INR " +
                            String.format("%.2f", offer.discountAmount) +
                            " off on bookings above INR " +
                            String.format("%.2f", offer.minBookingAmount) + ".";
                } else {
                    message = "Offer: " + offer.code + " gives INR " +
                            String.format("%.2f", offer.discountAmount) +
                            " off on your next booking.";
                }
                items.add(new String[]{"coupon-" + offer.id, message, "offer"});
            } else if (items.isEmpty() && offer.usedUpByUser) {
                String message = "Coupon " + offer.code + " is already used on your account.";
                items.add(new String[]{"coupon-used-" + offer.id, message, "used"});
            }

            if (items.size() >= safeLimit) {
                break;
            }
        }

        return items;
    }

    private boolean usageExists(int couponId, String orderRef) {
        if (!tableExists("coupon_usage")) {
            return false;
        }

        String sql = "SELECT 1 FROM coupon_usage WHERE coupon_id=? AND order_ref=? LIMIT 1";
        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ps.setString(2, orderRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            setLastError(e);
            return false;
        }
    }

    public boolean recordCouponUsage(int couponId,
                                     int userId,
                                     String orderRef,
                                     String couponCode,
                                     double discountAmount,
                                     double baseAmount,
                                     double finalAmount) {
        clearLastError();
        ensureCouponSchema();
        if (!tableExists("coupon_usage")) {
            setLastError("coupon_usage table is not available.");
            return false;
        }
        if (couponId <= 0 || userId <= 0 || orderRef == null || orderRef.isBlank()) {
            setLastError("Coupon usage data is incomplete.");
            return false;
        }

        String sql =
                "INSERT INTO coupon_usage " +
                "(coupon_id, user_id, order_ref, coupon_code, discount_amount, base_amount, final_amount, status, used_at) " +
                "VALUES (?,?,?,?,?,?,?,'APPLIED',CURRENT_TIMESTAMP)";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ps.setInt(2, userId);
            ps.setString(3, orderRef.trim());
            ps.setString(4, couponCode == null ? "" : couponCode.trim().toUpperCase());
            ps.setDouble(5, Math.max(0, discountAmount));
            ps.setDouble(6, Math.max(0, baseAmount));
            ps.setDouble(7, Math.max(0, finalAmount));
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException duplicate) {
            return usageExists(couponId, orderRef);
        } catch (Exception e) {
            setLastError(e);
            DBConnectionUtil.logIfUnexpected(e);
            return false;
        }
    }

    public double getDiscountByCode(String code) {
        CouponValidation validation = validateCouponForUser(code, 0, Double.MAX_VALUE);
        return validation.valid && validation.offer != null ? validation.offer.discountAmount : 0;
    }

    public boolean isValidCoupon(String code) {
        return validateCouponForUser(code, 0, Double.MAX_VALUE).valid;
    }

    public boolean addCoupon(String code, double discount, String expiryDate) {
        clearLastError();
        ensureCouponSchema();

        String sql =
                "INSERT INTO coupons " +
                "(code, discount_amount, min_booking_amount, max_uses, per_user_limit, expiry_date, active) " +
                "VALUES (?,?,0.00,NULL,1,?,1)";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code == null ? "" : code.trim().toUpperCase());
            ps.setDouble(2, discount);
            if (expiryDate == null || expiryDate.isBlank()) {
                ps.setNull(3, java.sql.Types.DATE);
            } else {
                ps.setDate(3, java.sql.Date.valueOf(expiryDate));
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            setLastError(e);
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    public List<String[]> getAllCoupons() {
        clearLastError();
        ensureCouponSchema();

        List<String[]> list = new ArrayList<>();
        if (!tableExists("coupons")) {
            return list;
        }

        String sql =
                "SELECT id, code, discount_amount, expiry_date, CASE WHEN active IS NULL THEN 1 ELSE active END AS active " +
                "FROM coupons ORDER BY id DESC";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(buildRow(rs));
            }
        } catch (Exception e) {
            setLastError(e);
            DBConnectionUtil.logIfUnexpected(e);
        }

        return list;
    }

    public boolean setCouponStatus(int id, boolean active) {
        clearLastError();
        String sql = "UPDATE coupons SET active=? WHERE id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            setLastError(e);
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    public boolean deleteCoupon(int id) {
        clearLastError();
        String sql = "DELETE FROM coupons WHERE id=?";

        try (Connection con = DBConnectionUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            setLastError(e);
            DBConnectionUtil.logIfUnexpected(e);
        }

        return false;
    }

    public static class CouponOffer {
        public int id;
        public String code = "";
        public double discountAmount;
        public double minBookingAmount;
        public Integer maxUses;
        public int perUserLimit = 1;
        public Date expiryDate;
        public boolean active;
        public int totalUsed;
        public int userUsed;
        public boolean eligible;
        public boolean usedUpByUser;
        public boolean exhausted;
        public boolean expired;
        public String badgeText = "";
        public String helperText = "";
    }

    public static class CouponValidation {
        public boolean valid;
        public String message = "";
        public CouponOffer offer;

        private static CouponValidation fail(String message) {
            CouponValidation validation = new CouponValidation();
            validation.valid = false;
            validation.message = message == null ? "Coupon validation failed." : message;
            return validation;
        }
    }
}
