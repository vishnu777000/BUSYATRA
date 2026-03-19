package app;

import javax.swing.*;

import ui.auth.LoginFrame;
import ui.common.MainFrame;

import util.Session;
import util.SessionManager;

public class AppLauncher {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            setupLookAndFeel();

            // 🔐 Check session
            if (SessionManager.isLoggedIn()) {

                Session.userId = SessionManager.getUserId();
                Session.role   = SessionManager.getRole();

                // 🚀 Launch main app
                openMainFrame();

            } else {

                // 🔐 Launch login
                openLogin();
            }

        });
    }

    /* ================= UI SETUP ================= */

    private static void setupLookAndFeel() {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // 🔥 Global UI Tweaks (modern feel)
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= NAVIGATION ================= */

    private static void openMainFrame() {

        MainFrame main = new MainFrame(null, Session.role);
        main.setVisible(true);
    }
public static void launch() {
    main(new String[]{});
}
    private static void openLogin() {

        LoginFrame login = new LoginFrame();
        login.setVisible(true);
    }
}