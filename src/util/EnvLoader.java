package util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class EnvLoader {
    private static final String ENV_FILE_NAME = ".env";

    private static final Map<String, String> ENV = new HashMap<>();

    static {
        load();
    }

    public static String get(String key) {
        String value = ENV.get(key);

        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }

        return value;
    }

    private static void load() {
        for (Path candidate : candidatePaths()) {
            if (loadFromFile(candidate)) {
                return;
            }
        }

        if (loadFromClasspath("/.env")) {
            return;
        }
        if (loadFromClasspath("/resources/.env")) {
            return;
        }

        System.out.println(".env file not found; using process environment only");
    }

    private static Set<Path> candidatePaths() {
        Set<Path> candidates = new LinkedHashSet<>();

        String explicit = System.getProperty("busyatra.env");
        if (explicit != null && !explicit.isBlank()) {
            addEnvCandidate(candidates, Paths.get(explicit.trim()));
        }

        addEnvCandidate(candidates, resolveLauncherDirectory());
        addEnvCandidate(candidates, Paths.get(".env"));

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            addEnvCandidate(candidates, Paths.get(userDir));
        }

        addEnvCandidates(candidates, resolveAppDirectory(), 4);

        return candidates;
    }

    private static Path resolveLauncherDirectory() {
        String jpackageAppPath = System.getProperty("jpackage.app-path");
        if (jpackageAppPath != null && !jpackageAppPath.isBlank()) {
            Path launcher = Paths.get(jpackageAppPath.trim());
            return normalizeDirectory(launcher);
        }

        try {
            String command = ProcessHandle.current().info().command().orElse(null);
            if (command == null || command.isBlank()) {
                return null;
            }

            Path launcher = Paths.get(command.trim());
            Path directory = normalizeDirectory(launcher);
            if (directory == null) {
                return null;
            }

            String name = launcher.getFileName() == null ? "" : launcher.getFileName().toString().toLowerCase();
            if ("java".equals(name) || "java.exe".equals(name)
                    || "javaw".equals(name) || "javaw.exe".equals(name)) {
                return null;
            }

            return directory;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path resolveAppDirectory() {
        try {
            URI uri = EnvLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path location = Paths.get(uri);
            return normalizeDirectory(location);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void addEnvCandidates(Set<Path> candidates, Path startDir, int maxParents) {
        Path current = normalizeDirectory(startDir);
        int remaining = maxParents;

        while (current != null && remaining >= 0) {
            addEnvCandidate(candidates, current);
            current = current.getParent();
            remaining--;
        }
    }

    private static void addEnvCandidate(Set<Path> candidates, Path path) {
        if (path == null) {
            return;
        }

        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.getFileName() != null && ENV_FILE_NAME.equalsIgnoreCase(normalized.getFileName().toString())) {
            candidates.add(normalized);
            return;
        }

        candidates.add(normalized.resolve(ENV_FILE_NAME));
    }

    private static Path normalizeDirectory(Path path) {
        if (path == null) {
            return null;
        }

        Path normalized = path.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized)) {
            return normalized;
        }
        if (Files.isRegularFile(normalized)) {
            return normalized.getParent();
        }
        return normalized;
    }

    private static boolean loadFromFile(Path path) {
        if (path == null) return false;
        try {
            Path normalized = path.toAbsolutePath().normalize();
            if (!Files.isRegularFile(normalized)) {
                return false;
            }
            try (BufferedReader br = Files.newBufferedReader(normalized, StandardCharsets.UTF_8)) {
                readLines(br);
                System.out.println("Loaded .env from " + normalized);
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean loadFromClasspath(String resourcePath) {
        try (InputStream in = EnvLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return false;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                readLines(br);
                System.out.println("Loaded .env from classpath resource " + resourcePath);
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void readLines(BufferedReader br) throws Exception {
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
