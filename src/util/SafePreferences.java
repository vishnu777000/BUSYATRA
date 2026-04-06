package util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;

public final class SafePreferences {

    private static final Preferences PREFS = initPreferences();
    private static final Map<String, String> FALLBACK = new ConcurrentHashMap<>();

    private SafePreferences() {
    }

    private static Preferences initPreferences() {
        try {
            Preferences prefs = Preferences.userRoot().node("BusYatra");
            prefs.get("__startup_probe__", null);
            return prefs;
        } catch (Throwable error) {
            System.err.println("[SafePreferences] Falling back to in-memory preferences: " + error.getMessage());
            return null;
        }
    }

    public static void put(String key, String value) {
        if (key == null || value == null) return;
        FALLBACK.put(key, value);
        if (PREFS == null) return;
        try {
            PREFS.put(key, value);
        } catch (Throwable error) {
            System.err.println("[SafePreferences] put failed for key '" + key + "': " + error.getMessage());
        }
    }

    public static String get(String key, String fallback) {
        String local = FALLBACK.get(key);
        if (local != null) {
            return local;
        }
        if (PREFS == null) {
            return fallback;
        }
        try {
            String value = PREFS.get(key, fallback);
            if (value != null) {
                FALLBACK.putIfAbsent(key, value);
            }
            return value;
        } catch (Throwable error) {
            System.err.println("[SafePreferences] get failed for key '" + key + "': " + error.getMessage());
            return fallback;
        }
    }

    public static void putBoolean(String key, boolean value) {
        put(key, String.valueOf(value));
    }

    public static boolean getBoolean(String key, boolean fallback) {
        String value = get(key, String.valueOf(fallback));
        return Boolean.parseBoolean(value);
    }

    public static void putInt(String key, int value) {
        put(key, String.valueOf(value));
    }

    public static int getInt(String key, int fallback) {
        String value = get(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static void remove(String key) {
        if (key == null) return;
        FALLBACK.remove(key);
        if (PREFS == null) return;
        try {
            PREFS.remove(key);
        } catch (Throwable error) {
            System.err.println("[SafePreferences] remove failed for key '" + key + "': " + error.getMessage());
        }
    }
}
