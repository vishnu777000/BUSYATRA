package util;

import javax.swing.*;
import java.awt.*;







public class NotificationUtil {

    private NotificationUtil() {
        
    }

    

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

    

    public static boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(
                null,
                message,
                "Confirm",
                JOptionPane.YES_NO_OPTION
        ) == JOptionPane.YES_OPTION;
    }
}
