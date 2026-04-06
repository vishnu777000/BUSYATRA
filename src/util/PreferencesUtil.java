package util;

public class PreferencesUtil {

    

    public static void setTheme(String theme) {
        SafePreferences.put("theme", theme);
    }

    public static String getTheme() {
        return SafePreferences.get("theme", "LIGHT");
    }

    

    public static void setNotifications(boolean value) {
        SafePreferences.putBoolean("notifications", value);
    }

    public static boolean getNotifications() {
        return SafePreferences.getBoolean("notifications", true);
    }

    public static void setEmailNotifications(boolean value) {
        SafePreferences.putBoolean("emailNotifications", value);
    }

    public static boolean getEmailNotifications() {
        return SafePreferences.getBoolean("emailNotifications", true);
    }

    public static void setSMSNotifications(boolean value) {
        SafePreferences.putBoolean("smsNotifications", value);
    }

    public static boolean getSMSNotifications() {
        return SafePreferences.getBoolean("smsNotifications", false);
    }

    public static void setPromoNotifications(boolean value) {
        SafePreferences.putBoolean("promoNotifications", value);
    }

    public static boolean getPromoNotifications() {
        return SafePreferences.getBoolean("promoNotifications", true);
    }

    public static void setSoundNotifications(boolean value) {
        SafePreferences.putBoolean("soundNotifications", value);
    }

    public static boolean getSoundNotifications() {
        return SafePreferences.getBoolean("soundNotifications", true);
    }

    

    public static void setAutoLogout(boolean value) {
        SafePreferences.putBoolean("autoLogout", value);
    }

    public static boolean getAutoLogout() {
        return SafePreferences.getBoolean("autoLogout", false);
    }

    

    public static void setRememberFilters(boolean value) {
        SafePreferences.putBoolean("rememberFilters", value);
    }

    public static boolean getRememberFilters() {
        return SafePreferences.getBoolean("rememberFilters", true);
    }

    public static void setConfirmBeforePayment(boolean value) {
        SafePreferences.putBoolean("confirmPayment", value);
    }

    public static boolean getConfirmBeforePayment() {
        return SafePreferences.getBoolean("confirmPayment", true);
    }

    public static void setShowTooltips(boolean value) {
        SafePreferences.putBoolean("showTooltips", value);
    }

    public static boolean getShowTooltips() {
        return SafePreferences.getBoolean("showTooltips", true);
    }

    public static void setCompactMode(boolean value) {
        SafePreferences.putBoolean("compactMode", value);
    }

    public static boolean getCompactMode() {
        return SafePreferences.getBoolean("compactMode", false);
    }

    

    public static void setTwoFactor(boolean value) {
        SafePreferences.putBoolean("twoFactor", value);
    }

    public static boolean getTwoFactor() {
        return SafePreferences.getBoolean("twoFactor", false);
    }

    public static void setDeviceLock(boolean value) {
        SafePreferences.putBoolean("deviceLock", value);
    }

    public static boolean getDeviceLock() {
        return SafePreferences.getBoolean("deviceLock", false);
    }

    public static void setHideBalance(boolean value) {
        SafePreferences.putBoolean("hideBalance", value);
    }

    public static boolean getHideBalance() {
        return SafePreferences.getBoolean("hideBalance", false);
    }

    public static void setLoginAlerts(boolean value) {
        SafePreferences.putBoolean("loginAlerts", value);
    }

    public static boolean getLoginAlerts() {
        return SafePreferences.getBoolean("loginAlerts", true);
    }
}

