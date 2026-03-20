package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class EnvLoader {

    private static final Map<String, String> ENV = new HashMap<>();

    static {
        try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {

            String line;
            while ((line = br.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("=", 2);

                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = sanitize(parts[1]);
                    ENV.put(key, value);
                }
            }

        } catch (Exception e) {
            System.out.println(".env file not found or failed to load");
        }
    }

    public static String get(String key) {
        String value = ENV.get(key);

        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }

        return value;
    }

    private static String sanitize(String rawValue) {
        String value = rawValue.trim();

        if (value.endsWith(";")) {
            value = value.substring(0, value.length() - 1).trim();
        }

        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }

        return value.trim();
    }
}
