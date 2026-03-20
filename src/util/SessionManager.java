package util;

import java.util.prefs.Preferences;

public class SessionManager {

    private static final Preferences prefs =
            Preferences.userRoot().node("BusYatra");

    public static void saveLogin(int userId, String role) {
        prefs.putInt("userId", userId);
        prefs.put("role", role);
        prefs.putBoolean("loggedIn", true);
    }

    public static void saveUserMeta(String username, String email) {
        if (username != null) prefs.put("username", username);
        if (email != null) prefs.put("userEmail", email);
    }

    public static boolean isLoggedIn() {
        return prefs.getBoolean("loggedIn", false);
    }

    public static int getUserId() {
        return prefs.getInt("userId", -1);
    }

    public static String getRole() {
        return prefs.get("role", null);
    }

    public static String getUsername() {
        return prefs.get("username", null);
    }

    public static String getUserEmail() {
        return prefs.get("userEmail", null);
    }

    public static void clear() {
        prefs.remove("userId");
        prefs.remove("role");
        prefs.remove("username");
        prefs.remove("userEmail");
        prefs.putBoolean("loggedIn", false);
    }
}
