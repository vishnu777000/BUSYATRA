package ui.common;

import util.IconUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SidebarPanel extends JPanel {

    private final MainFrame frame;
    private final String role;

    private final List<JButton> buttons = new ArrayList<>();
    private JButton activeButton;

    /* LIGHT THEME COLORS */
    private static final Color BG = Color.WHITE;
    private static final Color HOVER = new Color(245, 245, 245);
    private static final Color ACTIVE = new Color(235, 245, 255);
    private static final Color TEXT = new Color(50, 50, 50);
    private static final Color ACCENT = new Color(0, 120, 215);

    public SidebarPanel(MainFrame frame, String role) {

        this.frame = frame;
        this.role = role;

        setPreferredSize(new Dimension(230, 700));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BG);
        setBorder(new EmptyBorder(20, 15, 20, 10));

        buildBrand();
        add(Box.createVerticalStrut(20));

        buildNavigation();
        buildFooter();
    }

    /* ================= BRAND ================= */

    private void buildBrand() {

        JLabel brand = new JLabel("BusYatra");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brand.setForeground(new Color(220, 53, 69)); // red branding
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(brand);
    }

    /* ================= NAV ================= */

    private void buildNavigation() {

        if ("USER".equalsIgnoreCase(role)) {

            addSection("MAIN");
            addMenu("Dashboard", "home.png", MainFrame.SCREEN_USER);
            addMenu("Book Ticket", "ticket.png", MainFrame.SCREEN_SEARCH);

            addSection("BOOKINGS");
            addMenu("My Tickets", "history.png", MainFrame.SCREEN_MY_TICKETS);

            addSection("ACCOUNT");
            addMenu("Wallet", "wallet.png", MainFrame.SCREEN_WALLET);
            addMenu("Complaints", "support.png", MainFrame.SCREEN_COMPLAINT);
        }

        if ("ADMIN".equalsIgnoreCase(role)) {

            addSection("ADMIN");
            addMenu("Dashboard", "home.png", MainFrame.SCREEN_ADMIN);

            addSection("MANAGEMENT");
            addMenu("Users", "user.png", "MANAGE_USERS");
            addMenu("Buses", "bus.png", "MANAGE_BUSES");
            addMenu("Routes", "route.png", "MANAGE_ROUTES");
            addMenu("Schedules", "schedule.png", "MANAGE_SCHEDULES");

            addSection("ANALYTICS");
            addMenu("Reports", "report.png", "REPORTS");
        }
    }

    /* ================= FOOTER ================= */

    private void buildFooter() {

        add(Box.createVerticalGlue());

        addSection("SYSTEM");
        addMenu("Settings", "settings.png", MainFrame.SCREEN_SETTINGS);
    }

    /* ================= SECTION ================= */

    private void addSection(String text) {

        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(140, 140, 140));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setBorder(new EmptyBorder(12, 8, 6, 8));

        add(lbl);
    }

    /* ================= MENU ================= */

    private void addMenu(String text, String icon, String screen) {

        JButton btn = new JButton(text, IconUtil.load(icon, 18, 18));

        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(220, 42));

        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);

        btn.setBackground(BG);
        btn.setForeground(TEXT);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(12);
        btn.setBorder(new EmptyBorder(10, 12, 10, 12));

        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        /* HOVER EFFECT */

        btn.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn != activeButton)
                    btn.setBackground(HOVER);
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn != activeButton)
                    btn.setBackground(BG);
            }
        });

        btn.addActionListener(e -> {
            setActive(btn);
            frame.showScreen(screen);
        });

        buttons.add(btn);
        add(btn);
        add(Box.createVerticalStrut(6));

        if (activeButton == null)
            setActive(btn);
    }

    /* ================= ACTIVE ================= */

    private void setActive(JButton btn) {

        if (activeButton != null) {
            activeButton.setBackground(BG);
            activeButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            activeButton.setBorder(new EmptyBorder(10, 12, 10, 12));
        }

        activeButton = btn;

        activeButton.setBackground(ACTIVE);
        activeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));

        activeButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, ACCENT),
                new EmptyBorder(10, 10, 10, 12)
        ));
    }
}
