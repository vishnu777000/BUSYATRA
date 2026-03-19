package util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class HoverEffect {

    public static void applyButtonHover(JButton btn, Color normal, Color hover) {

        btn.setBackground(normal);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hover);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(normal);
            }
        });
    }

    public static void applyCardHover(JPanel panel) {

        panel.setBackground(Color.WHITE);

        panel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(new Color(250, 250, 250));
            }

            public void mouseExited(MouseEvent e) {
                panel.setBackground(Color.WHITE);
            }
        });
    }
}