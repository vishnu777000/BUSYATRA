package util;

import config.DBConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DatabaseSeeder {
    private static final List<String> DEFAULT_BASE_SEED_CANDIDATES = Arrays.asList(
            "busyatra_live_ap_routes_300_compat.sql",
            "busyatra_seed_apr_to_may_2026.sql"
    );
    private static final List<String> DEFAULT_ADDON_SEED_NAMES = Arrays.asList(
            "busyatra_seed_major_city_links_apr_to_may_2026.sql"
    );
    private static final String DEFAULT_SEED_KEY = "busyatra_seed_apr_to_may_2026_routes_300_v2_major_links";
    private static final String MARKER_TABLE = "app_seed_runs";
    private static final List<String> CORE_TABLES = Arrays.asList(
            "cities", "buses", "routes", "route_stops", "schedules"
    );

    private static volatile boolean attempted;

    private DatabaseSeeder() {
    }

    public static synchronized void bootstrapIfNeeded() {
        if (attempted) {
            return;
        }
        attempted = true;

        if (!isEnabled()) {
            log("Auto-seed disabled.");
            return;
        }

        long start = System.nanoTime();
        String seedKey = resolveSeedKey();
        List<SeedScript> seedScripts = locateSeedScripts();

        if (seedScripts.isEmpty()) {
            logError("Seed file was not found. Skipping auto-seed.");
            return;
        }

        String scriptSummary = summarizeScriptNames(seedScripts);
        String sourceSummary = summarizeSourceDescriptions(seedScripts);

        Connection connection = null;
        boolean success = false;
        boolean rolledBack = false;

        try {
            connection = DBConfig.getConnection();

            if (!coreTablesPresent(connection)) {
                log("Core tables are missing, so auto-seed was skipped.");
                return;
            }

            ensureMarkerTable(connection);

            String priorStatus = loadSeedStatus(connection, seedKey);
            if (isSuccess(priorStatus)) {
                log("Seed '" + seedKey + "' already applied.");
                return;
            }

            boolean existingTransportData = hasExistingTransportData(connection);
            boolean hasPreviousBusyatraSeed = hasSuccessfulBusyatraSeed(connection);
            if (shouldSkipForExistingData(priorStatus, existingTransportData, hasPreviousBusyatraSeed)) {
                upsertSeedStatus(
                        connection,
                        seedKey,
                        scriptSummary,
                        "SKIPPED",
                        "Existing transport data detected before first auto-seed.",
                        true
                );
                log("Existing transport data found. Leaving sample auto-seed untouched.");
                return;
            }

            upsertSeedStatus(
                    connection,
                    seedKey,
                    scriptSummary,
                    "RUNNING",
                    "Applying startup seed from " + sourceSummary + ".",
                    false
            );

            executeScripts(connection, seedScripts);

            upsertSeedStatus(
                    connection,
                    seedKey,
                    scriptSummary,
                    "SUCCESS",
                    "Seed applied automatically from " + sourceSummary + ".",
                    true
            );

            success = true;
            log("Seeded startup data from " + sourceSummary + " in " + elapsedMs(start) + "ms.");
        } catch (SQLException error) {
            rollbackQuietly(connection);
            rolledBack = true;
            markFailure(connection, seedKey, scriptSummary, error);
            if (DBConfig.isConnectionUnavailable(error)) {
                log("Database is unavailable, so auto-seed was skipped for now.");
            } else {
                logError("Auto-seed failed: " + DBConfig.userFriendlyMessage(error));
                error.printStackTrace();
            }
        } catch (Exception error) {
            rollbackQuietly(connection);
            rolledBack = true;
            markFailure(connection, seedKey, scriptSummary, error);
            logError("Auto-seed failed: " + rootMessage(error));
            error.printStackTrace();
        } finally {
            if (!success && !rolledBack) {
                rollbackQuietly(connection);
            }
            DBConnectionUtil.close(connection);
        }
    }

    private static boolean isEnabled() {
        String raw = firstNonBlank(
                System.getProperty("busyatra.autoSeed"),
                EnvLoader.get("DB_AUTO_SEED")
        );
        if (raw == null) {
            return true;
        }

        String value = raw.trim().toLowerCase(Locale.ENGLISH);
        return !("false".equals(value) || "0".equals(value) || "no".equals(value) || "off".equals(value));
    }

    private static String resolveSeedKey() {
        String configured = firstNonBlank(
                System.getProperty("busyatra.seedKey"),
                EnvLoader.get("DB_SEED_KEY")
        );
        return configured == null ? DEFAULT_SEED_KEY : configured.trim();
    }

    private static List<SeedScript> locateSeedScripts() {
        String explicit = firstNonBlank(
                System.getProperty("busyatra.seed"),
                EnvLoader.get("DB_SEED_FILE")
        );

        if (explicit != null) {
            SeedScript fromExplicit = loadFromFile(Paths.get(explicit.trim()), "configured file");
            if (fromExplicit != null) {
                return List.of(fromExplicit);
            }
        }

        List<SeedScript> scripts = new ArrayList<>();
        SeedScript base = loadFirstAvailableDefault(DEFAULT_BASE_SEED_CANDIDATES);
        if (base != null) {
            scripts.add(base);
        }
        for (String addonName : DEFAULT_ADDON_SEED_NAMES) {
            SeedScript addon = loadDefaultSeed(addonName);
            if (addon != null) {
                scripts.add(addon);
            }
        }
        return scripts;
    }

    private static SeedScript loadFirstAvailableDefault(List<String> fileNames) {
        if (fileNames == null) {
            return null;
        }
        for (String fileName : fileNames) {
            SeedScript script = loadDefaultSeed(fileName);
            if (script != null) {
                return script;
            }
        }
        return null;
    }

    private static SeedScript loadDefaultSeed(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        for (Path directory : candidateDirectories()) {
            SeedScript script = loadFromFile(directory.resolve("sql").resolve(fileName), "file");
            if (script != null) {
                return script;
            }
            script = loadFromFile(directory.resolve("src").resolve("resources").resolve("sql").resolve(fileName), "file");
            if (script != null) {
                return script;
            }
        }

        String resourcePath = "/resources/sql/" + fileName;
        try (InputStream in = DatabaseSeeder.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            String sql = readAll(in);
            return new SeedScript(fileName, sql, "packaged resource " + resourcePath);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Set<Path> candidateDirectories() {
        Set<Path> directories = new LinkedHashSet<>();
        addDirectory(directories, Paths.get("."));

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            addDirectory(directories, Paths.get(userDir.trim()));
        }

        addParentDirectories(directories, resolveLauncherDirectory(), 4);
        addParentDirectories(directories, resolveAppDirectory(), 4);

        Path userHomeAppDir = Paths.get(System.getProperty("user.home", "."), "BusYatra");
        addDirectory(directories, userHomeAppDir);
        addDirectory(directories, userHomeAppDir.resolve("app"));
        return directories;
    }

    private static Path resolveLauncherDirectory() {
        String jpackageAppPath = System.getProperty("jpackage.app-path");
        if (jpackageAppPath != null && !jpackageAppPath.isBlank()) {
            return normalizeDirectory(Paths.get(jpackageAppPath.trim()));
        }

        try {
            String command = ProcessHandle.current().info().command().orElse(null);
            if (command == null || command.isBlank()) {
                return null;
            }
            Path launcherPath = Paths.get(command.trim());
            String name = launcherPath.getFileName() == null ? "" : launcherPath.getFileName().toString().toLowerCase(Locale.ENGLISH);
            if ("java".equals(name) || "java.exe".equals(name)
                    || "javaw".equals(name) || "javaw.exe".equals(name)) {
                return null;
            }
            return normalizeDirectory(launcherPath);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path resolveAppDirectory() {
        try {
            URI uri = DatabaseSeeder.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            return normalizeDirectory(Paths.get(uri));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void addParentDirectories(Set<Path> directories, Path start, int maxParents) {
        Path current = normalizeDirectory(start);
        int remaining = maxParents;
        while (current != null && remaining >= 0) {
            addDirectory(directories, current);
            current = current.getParent();
            remaining--;
        }
    }

    private static void addDirectory(Set<Path> directories, Path path) {
        if (path == null) {
            return;
        }
        directories.add(normalizeDirectory(path));
    }

    private static Path normalizeDirectory(Path path) {
        if (path == null) {
            return null;
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.isRegularFile(normalized)) {
            return normalized.getParent();
        }
        return normalized;
    }

    private static SeedScript loadFromFile(Path path, String sourceLabel) {
        if (path == null) {
            return null;
        }
        try {
            Path normalized = path.toAbsolutePath().normalize();
            if (!Files.isRegularFile(normalized)) {
                return null;
            }
            String sql = Files.readString(normalized, StandardCharsets.UTF_8);
            return new SeedScript(normalized.getFileName().toString(), sql, sourceLabel + " " + normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean coreTablesPresent(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name IN ('cities','buses','routes','route_stops','schedules')";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) == CORE_TABLES.size();
            }
            return false;
        }
    }

    private static void ensureMarkerTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + MARKER_TABLE + " ("
                + "seed_key VARCHAR(120) PRIMARY KEY,"
                + "script_name VARCHAR(255) NOT NULL,"
                + "status VARCHAR(20) NOT NULL,"
                + "details TEXT NULL,"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "completed_at TIMESTAMP NULL DEFAULT NULL"
                + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String loadSeedStatus(Connection connection, String seedKey) throws SQLException {
        String sql = "SELECT status FROM " + MARKER_TABLE + " WHERE seed_key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, seedKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        }
    }

    private static boolean hasExistingTransportData(Connection connection) throws SQLException {
        return countRows(connection, "buses") > 0
                || countRows(connection, "routes") > 0
                || countRows(connection, "schedules") > 0;
    }

    private static int countRows(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static boolean hasSuccessfulBusyatraSeed(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + MARKER_TABLE
                + " WHERE seed_key LIKE 'busyatra_seed_%' AND status = 'SUCCESS'";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static boolean shouldSkipForExistingData(
            String priorStatus,
            boolean existingTransportData,
            boolean hasPreviousBusyatraSeed
    ) {
        if (!existingTransportData) {
            return false;
        }
        if (hasPreviousBusyatraSeed) {
            return false;
        }
        return priorStatus == null || "SKIPPED".equalsIgnoreCase(priorStatus);
    }

    private static boolean isSuccess(String status) {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    private static void upsertSeedStatus(
            Connection connection,
            String seedKey,
            String scriptName,
            String status,
            String details,
            boolean completed
    ) throws SQLException {
        String sql = "INSERT INTO " + MARKER_TABLE
                + " (seed_key, script_name, status, details, completed_at) VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE script_name = VALUES(script_name), status = VALUES(status), "
                + "details = VALUES(details), completed_at = VALUES(completed_at)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, seedKey);
            ps.setString(2, scriptName);
            ps.setString(3, status);
            ps.setString(4, details);
            if (completed) {
                ps.setTimestamp(5, Timestamp.from(Instant.now()));
            } else {
                ps.setTimestamp(5, null);
            }
            ps.executeUpdate();
        }
    }

    private static void executeScripts(Connection connection, List<SeedScript> scripts) throws SQLException {
        if (scripts == null) {
            return;
        }
        for (SeedScript script : scripts) {
            if (script == null || script.sql == null || script.sql.isBlank()) {
                continue;
            }
            executeScript(connection, script.sql);
        }
    }

    private static void executeScript(Connection connection, String script) throws SQLException {
        List<String> statements = splitStatements(script);
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
    }

    private static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBackticks = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!inSingleQuote && !inDoubleQuote && !inBackticks
                        && (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("#"))) {
                    continue;
                }

                for (int i = 0; i < line.length(); i++) {
                    char ch = line.charAt(i);

                    if (ch == '\'' && !inDoubleQuote && !inBackticks && !isEscaped(line, i)) {
                        inSingleQuote = !inSingleQuote;
                    } else if (ch == '"' && !inSingleQuote && !inBackticks && !isEscaped(line, i)) {
                        inDoubleQuote = !inDoubleQuote;
                    } else if (ch == '`' && !inSingleQuote && !inDoubleQuote && !isEscaped(line, i)) {
                        inBackticks = !inBackticks;
                    }

                    if (ch == ';' && !inSingleQuote && !inDoubleQuote && !inBackticks) {
                        statements.add(current.toString());
                        current.setLength(0);
                    } else {
                        current.append(ch);
                    }
                }
                current.append('\n');
            }
        } catch (Exception ignored) {
        }

        if (current.length() > 0) {
            statements.add(current.toString());
        }
        return statements;
    }

    private static boolean isEscaped(String line, int index) {
        int slashCount = 0;
        for (int i = index - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 != 0;
    }

    private static String readAll(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private static void markFailure(Connection connection, String seedKey, String scriptName, Throwable error) {
        if (connection == null) {
            return;
        }
        try {
            ensureMarkerTable(connection);
            upsertSeedStatus(
                    connection,
                    seedKey,
                    scriptName,
                    "FAILED",
                    rootMessage(error),
                    true
            );
        } catch (Exception ignored) {
        }
    }

    private static void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ROLLBACK");
        } catch (Exception ignored) {
        }
    }

    private static long elapsedMs(long start) {
        return Math.max(0L, (System.nanoTime() - start) / 1_000_000L);
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current == null ? null : current.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : message.trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String summarizeScriptNames(List<SeedScript> scripts) {
        List<String> names = new ArrayList<>();
        if (scripts != null) {
            for (SeedScript script : scripts) {
                if (script != null && script.name != null && !script.name.isBlank()) {
                    names.add(script.name.trim());
                }
            }
        }
        return names.isEmpty() ? "seed bundle" : String.join(", ", names);
    }

    private static String summarizeSourceDescriptions(List<SeedScript> scripts) {
        List<String> descriptions = new ArrayList<>();
        if (scripts != null) {
            for (SeedScript script : scripts) {
                if (script != null && script.sourceDescription != null && !script.sourceDescription.isBlank()) {
                    descriptions.add(script.sourceDescription.trim());
                }
            }
        }
        return descriptions.isEmpty() ? "configured sources" : String.join(" + ", descriptions);
    }

    private static void log(String message) {
        System.out.println("[DatabaseSeeder] " + message);
    }

    private static void logError(String message) {
        System.err.println("[DatabaseSeeder] " + message);
    }

    private static final class SeedScript {
        private final String name;
        private final String sql;
        private final String sourceDescription;

        private SeedScript(String name, String sql, String sourceDescription) {
            this.name = name;
            this.sql = sql;
            this.sourceDescription = sourceDescription;
        }
    }
}
