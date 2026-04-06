package util;

public class SessionManager {

    public static void saveLogin(int userId, String role) {
        SafePreferences.putInt("userId", userId);
        SafePreferences.put("role", role);
        SafePreferences.putBoolean("loggedIn", true);
    }

    public static void saveUserMeta(String username, String email) {
        if (username != null) SafePreferences.put("username", username);
        if (email != null) SafePreferences.put("userEmail", email);
    }

    public static boolean isLoggedIn() {
        return SafePreferences.getBoolean("loggedIn", false);
    }

    public static int getUserId() {
        return SafePreferences.getInt("userId", -1);
    }

    public static String getRole() {
        return SafePreferences.get("role", null);
    }

    public static String getUsername() {
        return SafePreferences.get("username", null);
    }

    public static String getUserEmail() {
        return SafePreferences.get("userEmail", null);
    }

    public static void clear() {
        SafePreferences.remove("userId");
        SafePreferences.remove("role");
        SafePreferences.remove("username");
        SafePreferences.remove("userEmail");
        SafePreferences.putBoolean("loggedIn", false);
    }
}
