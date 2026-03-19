package config;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class UIConfig {

    /* ================= COLORS ================= */

    public static final Color PRIMARY = new Color(220, 53, 69);        // 🔥 RED THEME
    public static final Color PRIMARY_HOVER = new Color(200, 40, 60);

    public static final Color SUCCESS = new Color(40, 167, 69);
    public static final Color DANGER = new Color(220, 53, 69);
    public static final Color INFO = new Color(23, 162, 184);
    public static final Color WARNING = new Color(255, 193, 7);

    public static final Color BACKGROUND = new Color(245, 247, 250);
    public static final Color CARD = Color.WHITE;

    public static final Color TEXT = new Color(40, 40, 40);
    public static final Color TEXT_LIGHT = new Color(120, 120, 120);

    public static final Color BORDER = new Color(230, 230, 230);

    /* ================= SEAT COLORS ================= */

    public static final Color SEAT_AVAILABLE = new Color(40, 167, 69);
    public static final Color SEAT_BOOKED = new Color(220, 53, 69);
    public static final Color SEAT_LOCKED = new Color(255, 193, 7);

    /* ================= FONTS ================= */

    public static final Font FONT_TITLE =
            new Font("Segoe UI", Font.BOLD, 20);

    public static final Font FONT_SUBTITLE =
            new Font("Segoe UI", Font.BOLD, 15);

    public static final Font FONT_NORMAL =
            new Font("Segoe UI", Font.PLAIN, 13);

    public static final Font FONT_SMALL =
            new Font("Segoe UI", Font.PLAIN, 12);

    /* ================= CARD ================= */

    public static void styleCard(JPanel p) {

        p.setBackground(CARD);

        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
    }

    /* ================= SIDEBAR ================= */

    public static void styleSidebar(JPanel p) {
        p.setBackground(Color.WHITE);   // 🔥 clean sidebar
    }

    public static void styleSidebarButton(JButton b) {

        b.setFont(FONT_NORMAL);
        b.setForeground(TEXT);
        b.setBackground(Color.WHITE);

        b.setFocusPainted(false);
        b.setBorderPainted(false);

        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.setPreferredSize(new Dimension(200, 45));

        b.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(245, 245, 245));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(Color.WHITE);
            }
        });
    }

    /* ================= BUTTON SYSTEM ================= */

    private static void baseBtn(JButton b, Color base, Color hover) {

        b.setFont(FONT_NORMAL);
        b.setFocusPainted(false);
        b.setBorderPainted(false);

        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(140, 36));

        b.setBackground(base);
        b.setForeground(Color.WHITE);

        b.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(hover);
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(base);
            }
        });
    }

    public static void primaryBtn(JButton b) {
        baseBtn(b, PRIMARY, PRIMARY_HOVER);
    }

    public static void successBtn(JButton b) {
        baseBtn(b, SUCCESS, SUCCESS.darker());
    }

    public static void dangerBtn(JButton b) {
        baseBtn(b, DANGER, PRIMARY_HOVER);
    }

    public static void infoBtn(JButton b) {
        baseBtn(b, INFO, INFO.darker());
    }

    /* ================= TABLE ================= */

    public static void styleTable(JTable table) {

        table.setRowHeight(32);
        table.setFont(FONT_NORMAL);

        table.setSelectionBackground(new Color(255, 230, 230)); // light red

        JTableHeader h = table.getTableHeader();

        h.setFont(new Font("Segoe UI", Font.BOLD, 13));
        h.setBackground(PRIMARY);
        h.setForeground(Color.WHITE);
    }

    /* ================= INPUT ================= */

    public static void styleField(JTextField f) {

        f.setFont(FONT_NORMAL);

        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    /* ================= COMBO ================= */

    public static void styleCombo(JComboBox<?> c) {

        c.setFont(FONT_NORMAL);

        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    /* ================= SCROLL ================= */

    public static void styleScroll(JScrollPane sp) {
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
    }

    /* ================= DASHBOARD CARD ================= */

    public static JPanel dashboardCard(String title, String value) {

        JPanel card = new JPanel(new BorderLayout(10, 10));
        styleCard(card);

        JLabel t = new JLabel(title);
        t.setFont(FONT_SUBTITLE);
        t.setForeground(TEXT_LIGHT);

        JLabel v = new JLabel(value, SwingConstants.CENTER);
        v.setFont(new Font("Segoe UI", Font.BOLD, 22));
        v.setForeground(PRIMARY);

        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);

        return card;
    }

    /* ================= STATUS LABEL ================= */

    public static JLabel statusLabel(String text, Color c) {

        JLabel l = new JLabel(text, SwingConstants.CENTER);

        l.setOpaque(true);
        l.setBackground(c);
        l.setForeground(Color.WHITE);

        l.setFont(FONT_SMALL);

        l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        return l;
    }
}