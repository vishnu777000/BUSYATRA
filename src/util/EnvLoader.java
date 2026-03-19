package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class EnvLoader {

    private static final Map<String, String> env = new HashMap<>();

    static {
        try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {

            String line;
            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("=", 2);

                if (parts.length == 2) {
                    env.put(parts[0].trim(), parts[1].trim());
                }
            }

        } catch (Exception e) {
            System.out.println("⚠ .env file not found or failed to load");
        }
    }

    public static String get(String key) {
        return env.get(key);
    }
}