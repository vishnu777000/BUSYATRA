package app;

import javax.swing.*;

import ui.auth.LoginFrame;
import ui.common.MainFrame;

import util.IconUtil;
import util.Session;
import util.SessionManager;

public class AppLauncher {
    private static final boolean PERF_LOG = true;

    public static void main(String[] args) {
        final long appStart = System.nanoTime();

        SwingUtilities.invokeLater(() -> {
            final long uiStart = System.nanoTime();

            setupLookAndFeel();
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
        MainFrame main = new MainFrame(null, Session.role);
        main.setVisible(true);
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
        LoginFrame login = new LoginFrame();
        login.setVisible(true);
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
}
