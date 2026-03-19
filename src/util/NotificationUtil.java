package util;

import javax.swing.*;
import java.awt.*;

/**
 * NotificationUtil
 * --------------------------------
 * Central utility for showing
 * user notifications across app
 */
public class NotificationUtil {

    private NotificationUtil() {
        // utility class
    }

    /* ================= BASIC ================= */

    public static void info(String message) {
        show(message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void success(String message) {
        show(message, "Success", JOptionPane.PLAIN_MESSAGE);
    }

    public static void warning(String message) {
        show(message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void error(String message) {
        show(message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /* ================= CORE ================= */

    private static void show(
            String message,
            String title,
            int type
    ) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                        null,
                        message,
                        title,
                        type
                )
        );
    }

    /* ================= CONFIRM ================= */

    public static boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(
                null,
                message,
                "Confirm",
                JOptionPane.YES_NO_OPTION
        ) == JOptionPane.YES_OPTION;
    }
}
