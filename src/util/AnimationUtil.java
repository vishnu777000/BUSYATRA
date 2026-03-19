package util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * AnimationUtil
 * ---------------------------------------
 * Lightweight UI interaction helpers
 * (SAFE for Swing, NO custom painting)
 */
public final class AnimationUtil {

    private AnimationUtil() {
        // utility class
    }

    /* =================================================
       BUTTON HOVER EFFECT
       ================================================= */
    public static void addButtonHover(
            JButton btn,
            Color normalBg,
            Color hoverBg
    ) {
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBackground(normalBg);
        btn.setFocusPainted(false);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normalBg);
            }
        });
    }

    /* =================================================
       CARD HOVER EFFECT (Dashboard / Panels)
       ================================================= */
    public static void addCardHover(
            JComponent card,
            Color normalBg,
            Color hoverBg
    ) {
        card.setBackground(normalBg);
        card.setOpaque(true);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(normalBg);
            }
        });
    }

    /* =================================================
       LOADING CURSOR HELPERS
       ================================================= */
    public static void showLoading(Component c) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public static void hideLoading(Component c) {
        c.setCursor(Cursor.getDefaultCursor());
    }

    /* =================================================
       DELAYED ACTION (UX POLISH)
       ================================================= */
    public static void delay(int ms, Runnable action) {
        Timer t = new Timer(ms, e -> action.run());
        t.setRepeats(false);
        t.start();
    }
}
