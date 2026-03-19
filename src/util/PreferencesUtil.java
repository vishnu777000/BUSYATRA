package util;

import java.util.prefs.Preferences;

public class PreferencesUtil {

    private static final Preferences prefs =
            Preferences.userRoot().node("BusYatra");

    /* ================= THEME ================= */

    public static void setTheme(String theme) {
        prefs.put("theme", theme);
    }

    public static String getTheme() {
        return prefs.get("theme", "LIGHT");
    }

    /* ================= NOTIFICATIONS ================= */

    public static void setNotifications(boolean value) {
        prefs.putBoolean("notifications", value);
    }

    public static boolean getNotifications() {
        return prefs.getBoolean("notifications", true);
    }

    public static void setEmailNotifications(boolean value) {
        prefs.putBoolean("emailNotifications", value);
    }

    public static boolean getEmailNotifications() {
        return prefs.getBoolean("emailNotifications", true);
    }

    public static void setSMSNotifications(boolean value) {
        prefs.putBoolean("smsNotifications", value);
    }

    public static boolean getSMSNotifications() {
        return prefs.getBoolean("smsNotifications", false);
    }

    public static void setPromoNotifications(boolean value) {
        prefs.putBoolean("promoNotifications", value);
    }

    public static boolean getPromoNotifications() {
        return prefs.getBoolean("promoNotifications", true);
    }

    public static void setSoundNotifications(boolean value) {
        prefs.putBoolean("soundNotifications", value);
    }

    public static boolean getSoundNotifications() {
        return prefs.getBoolean("soundNotifications", true);
    }

    /* ================= AUTO LOGOUT ================= */

    public static void setAutoLogout(boolean value) {
        prefs.putBoolean("autoLogout", value);
    }

    public static boolean getAutoLogout() {
        return prefs.getBoolean("autoLogout", false);
    }

    /* ================= UI SETTINGS ================= */

    public static void setRememberFilters(boolean value) {
        prefs.putBoolean("rememberFilters", value);
    }

    public static boolean getRememberFilters() {
        return prefs.getBoolean("rememberFilters", true);
    }

    public static void setConfirmBeforePayment(boolean value) {
        prefs.putBoolean("confirmPayment", value);
    }

    public static boolean getConfirmBeforePayment() {
        return prefs.getBoolean("confirmPayment", true);
    }

    public static void setShowTooltips(boolean value) {
        prefs.putBoolean("showTooltips", value);
    }

    public static boolean getShowTooltips() {
        return prefs.getBoolean("showTooltips", true);
    }

    public static void setCompactMode(boolean value) {
        prefs.putBoolean("compactMode", value);
    }

    public static boolean getCompactMode() {
        return prefs.getBoolean("compactMode", false);
    }

    /* ================= SECURITY ================= */

    public static void setTwoFactor(boolean value) {
        prefs.putBoolean("twoFactor", value);
    }

    public static boolean getTwoFactor() {
        return prefs.getBoolean("twoFactor", false);
    }

    public static void setDeviceLock(boolean value) {
        prefs.putBoolean("deviceLock", value);
    }

    public static boolean getDeviceLock() {
        return prefs.getBoolean("deviceLock", false);
    }

    public static void setHideBalance(boolean value) {
        prefs.putBoolean("hideBalance", value);
    }

    public static boolean getHideBalance() {
        return prefs.getBoolean("hideBalance", false);
    }

    public static void setLoginAlerts(boolean value) {
        prefs.putBoolean("loginAlerts", value);
    }

    public static boolean getLoginAlerts() {
        return prefs.getBoolean("loginAlerts", true);
    }
}

