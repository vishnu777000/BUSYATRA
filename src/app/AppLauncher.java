package app;

import javax.swing.*;

import ui.auth.LoginFrame;
import ui.common.MainFrame;

import util.IconUtil;
import util.DatabaseSeeder;
import util.Session;
import util.SessionManager;
import util.UiLagMonitor;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class AppLauncher {
    private static final boolean PERF_LOG = true;
    private static volatile boolean startupErrorShown = false;

    public static void main(String[] args) {
        final long appStart = System.nanoTime();
        installCrashHandler();
        DatabaseSeeder.bootstrapIfNeeded();

        SwingUtilities.invokeLater(() -> {
            final long uiStart = System.nanoTime();
            try {
                setupLookAndFeel();
                UiLagMonitor.install();
                warmupResources();

                if (SessionManager.isLoggedIn()) {

                    Session.userId = SessionManager.getUserId();
                    Session.role = SessionManager.getRole();
                    Session.username = SessionManager.getUsername();
                    Session.userEmail = SessionManager.getUserEmail();

                    if (Session.userId > 0 && isSupportedRole(Session.role)) {
                        openMainFrame();
                    } else {
                        SessionManager.clear();
                        Session.clear();
                        openLogin();
                    }
                } else {
                    openLogin();
                }
                logPerf("app boot", appStart);
                logPerf("EDT startup", uiStart);
            } catch (Throwable error) {
                reportStartupFailure("BusYatra failed to start.", error);
            }
        });
    }

    private static void setupLookAndFeel() {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void openMainFrame() {
        new MainFrame(null, Session.role);
    }

    private static void warmupResources() {
        Thread warmup = new Thread(IconUtil::preloadCommonIcons, "icon-warmup");
        warmup.setDaemon(true);
        warmup.start();
    }

    public static void launch() {
        main(new String[]{});
    }

    private static void openLogin() {
        new LoginFrame();
    }

    private static boolean isSupportedRole(String role) {
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return "USER".equals(r) || "ADMIN".equals(r) || "MANAGER".equals(r)
                || "CLERK".equals(r) || "BOOKING_CLERK".equals(r);
    }

    private static void logPerf(String action, long startNanos) {
        if (!PERF_LOG) return;
        long ms = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
        System.out.println("[Perf] " + action + " took " + ms + "ms");
    }

    private static void installCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                reportStartupFailure("BusYatra crashed during startup.", error));
    }

    private static synchronized void reportStartupFailure(String title, Throwable error) {
        if (startupErrorShown) {
            error.printStackTrace();
            return;
        }
        startupErrorShown = true;

        error.printStackTrace();

        Path logPath = writeStartupLog(error);
        String message = buildStartupMessage(error, logPath);

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(message);
            return;
        }

        JOptionPane.showMessageDialog(
                null,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static String buildStartupMessage(Throwable error, Path logPath) {
        String base = rootCauseMessage(error);
        String lower = base.toLowerCase();
        if (lower.contains("missing db config key") || lower.contains("db_host")
                || lower.contains("db_port") || lower.contains("db_user")
                || lower.contains("db_pass") || lower.contains("db_db")) {
            base = "Database config was not found. Keep a .env file next to the .exe or the jar, or set the DB_* environment variables.";
        }

        StringBuilder msg = new StringBuilder("BusYatra could not start.\n\n");
        msg.append(base);
        if (logPath != null) {
            msg.append("\n\nLog file:\n").append(logPath.toAbsolutePath());
        }
        return msg.toString();
    }

    private static String rootCauseMessage(Throwable error) {
        Throwable current = error;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current == null ? null : current.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : message.trim();
    }

    private static Path writeStartupLog(Throwable error) {
        try {
            Path dir = Path.of(System.getProperty("user.home"), "BusYatra", "logs");
            Files.createDirectories(dir);
            Path file = dir.resolve("startup-error.log");

            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                pw.println("BusYatra startup failure");
                pw.println("Time: " + LocalDateTime.now());
                pw.println("Java: " + System.getProperty("java.version"));
                pw.println("User dir: " + System.getProperty("user.dir"));
                pw.println();
                error.printStackTrace(pw);
            }

            Files.writeString(file, sw.toString(), StandardCharsets.UTF_8);
            return file;
        } catch (Exception ignored) {
            return null;
        }
    }
}
