package ui.common;

import config.UIConfig;
import util.IconUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SidebarPanel extends JPanel {

    private final MainFrame frame;
    private final String role;
    private final JPanel navPanel = new JPanel();
    private final JPanel footer = new JPanel();
    private final JLabel brandLabel = new JLabel("BusYatra");
    private final JButton toggleBtn = new JButton(createHamburgerIcon(18, 18, UIConfig.PRIMARY));

    private final List<JLabel> sectionLabels = new ArrayList<>();
    private final List<JButton> menuButtons = new ArrayList<>();
    private final Map<JButton, String> iconByButton = new HashMap<>();

    private JButton activeButton;
    private boolean collapsed = false;

    private static final int EXPANDED_WIDTH = 252;
    private static final int COLLAPSED_WIDTH = 78;

    private static final Color BG = Color.WHITE;
    private static final Color HOVER = new Color(245, 247, 251);
    private static final Color ACTIVE = new Color(255, 239, 240);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color SECTION = new Color(122, 141, 166);
    private static final Color ACCENT = UIConfig.PRIMARY;
    private static final Color ICON_NORMAL = new Color(100, 116, 139);

    public SidebarPanel(MainFrame frame, String role) {
        this.frame = frame;
        this.role = role == null ? "" : role.trim().toUpperCase();

        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(new EmptyBorder(14, 10, 12, 10));
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 0));

        add(buildTop(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        if (activeButton == null) {
            frame.showScreen(MainFrame.SCREEN_USER);
        }
    }

    private JComponent buildTop() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(2, 2, 10, 2));

        JPanel brandRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandRow.setOpaque(false);

        JLabel logo = new JLabel(IconUtil.load("buslogo.png", 26, 26));
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brandLabel.setForeground(ACCENT);

        brandRow.add(logo);
        brandRow.add(brandLabel);

        toggleBtn.setFocusPainted(false);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setOpaque(false);
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleBtn.setToolTipText("Collapse sidebar");
        toggleBtn.addActionListener(e -> toggleCollapsed());

        top.add(brandRow, BorderLayout.WEST);
        top.add(toggleBtn, BorderLayout.EAST);
        top.add(new JSeparator(), BorderLayout.SOUTH);
        return top;
    }

    private JComponent buildCenter() {
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);
        navPanel.setBorder(new EmptyBorder(10, 2, 10, 2));

        buildNavigation();

        JScrollPane scroll = new JScrollPane(navPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        return scroll;
    }

    private JComponent buildFooter() {
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 2, 0, 2));

        footer.add(new JSeparator());
        footer.add(Box.createVerticalStrut(8));
        addSection(footer, "SYSTEM");
        addMenu(footer, "Settings", "settings.png", MainFrame.SCREEN_SETTINGS);
        return footer;
    }

    private void buildNavigation() {
        if ("USER".equalsIgnoreCase(role)) {
            addSection(navPanel, "MAIN");
            addMenu(navPanel, "Dashboard", "home.png", MainFrame.SCREEN_USER);
            addMenu(navPanel, "Book Ticket", "search.png", MainFrame.SCREEN_SEARCH);

            addSection(navPanel, "BOOKINGS");
            addMenu(navPanel, "My Tickets", "ticket.png", MainFrame.SCREEN_MY_TICKETS);

            addSection(navPanel, "ACCOUNT");
            addMenu(navPanel, "Wallet", "wallet.png", MainFrame.SCREEN_WALLET);
            addMenu(navPanel, "Complaints", "support.png", MainFrame.SCREEN_COMPLAINT);
            return;
        }

        if ("ADMIN".equalsIgnoreCase(role)) {
            addSection(navPanel, "ADMIN");
            addMenu(navPanel, "Dashboard", "home.png", MainFrame.SCREEN_ADMIN);

            addSection(navPanel, "MANAGEMENT");
            addMenu(navPanel, "Users", "user.png", "MANAGE_USERS");
            addMenu(navPanel, "Buses", "bus.png", "MANAGE_BUSES");
            addMenu(navPanel, "Routes", "route.png", "MANAGE_ROUTES");
            addMenu(navPanel, "Route Map", "map.png", "ADMIN_ROUTE_MAP");
            addMenu(navPanel, "Schedules", "schedule.png", "MANAGE_SCHEDULES");
            addMenu(navPanel, "Banners", "notification.png", "ADMIN_BANNERS");

            addSection(navPanel, "ENGAGEMENT");
            addMenu(navPanel, "Complaints", "support.png", "ADMIN_COMPLAINTS");
            addMenu(navPanel, "News", "notification.png", "ADMIN_NEWS");

            addSection(navPanel, "ANALYTICS");
            addMenu(navPanel, "Reports", "report.png", "REPORTS");
            addMenu(navPanel, "Accounts", "wallet.png", "ACCOUNTS");
            addMenu(navPanel, "Manager Board", "home.png", MainFrame.SCREEN_MANAGER);
            addMenu(navPanel, "Clerk Board", "ticket.png", MainFrame.SCREEN_CLERK);
            return;
        }

        if ("MANAGER".equalsIgnoreCase(role)) {
            addSection(navPanel, "MANAGER");
            addMenu(navPanel, "Dashboard", "home.png", MainFrame.SCREEN_MANAGER);
            addMenu(navPanel, "Reports", "report.png", "REPORTS");
            addMenu(navPanel, "Accounts", "wallet.png", "ACCOUNTS");
            addMenu(navPanel, "Complaints", "support.png", "ADMIN_COMPLAINTS");
            addMenu(navPanel, "Schedules", "schedule.png", "MANAGE_SCHEDULES");
        }
    }

    private void addSection(JPanel container, String text) {
        container.add(Box.createVerticalStrut(10));

        JLabel lbl = new JLabel(text);
        lbl.setForeground(SECTION);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setBorder(new EmptyBorder(4, 8, 6, 6));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        sectionLabels.add(lbl);
        container.add(lbl);
    }

    private void addMenu(JPanel container, String text, String iconName, String screen) {
        JButton btn = new JButton(text, IconUtil.loadTinted(iconName, 19, 19, ICON_NORMAL));
        iconByButton.put(btn, iconName);

        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setPreferredSize(new Dimension(220, 42));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBackground(BG);
        btn.setForeground(TEXT);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(12);
        btn.setBorder(new EmptyBorder(10, 12, 10, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(text);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn != activeButton) btn.setBackground(HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn != activeButton) btn.setBackground(BG);
            }
        });

        btn.addActionListener(e -> {
            setActive(btn);
            frame.showScreen(screen);
        });

        menuButtons.add(btn);
        container.add(btn);
        container.add(Box.createVerticalStrut(4));

        if (activeButton == null) {
            setActive(btn);
        }
    }

    private void setActive(JButton btn) {
        if (activeButton != null) {
            activeButton.setBackground(BG);
            activeButton.setForeground(TEXT);
            activeButton.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            resetButtonPadding(activeButton);
            String iconName = iconByButton.get(activeButton);
            if (iconName != null) {
                activeButton.setIcon(IconUtil.loadTinted(iconName, 19, 19, ICON_NORMAL));
            }
        }

        activeButton = btn;
        activeButton.setBackground(ACTIVE);
        activeButton.setForeground(ACCENT);
        activeButton.setFont(new Font("Segoe UI", Font.BOLD, 15));

        if (collapsed) {
            activeButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT),
                    new EmptyBorder(10, 9, 10, 9)
            ));
        } else {
            activeButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, ACCENT),
                    new EmptyBorder(10, 8, 10, 12)
            ));
        }

        String iconName = iconByButton.get(activeButton);
        if (iconName != null) {
            activeButton.setIcon(IconUtil.loadTinted(iconName, 19, 19, ACCENT));
        }
    }

    private void resetButtonPadding(JButton btn) {
        if (collapsed) {
            btn.setBorder(new EmptyBorder(10, 10, 10, 10));
        } else {
            btn.setBorder(new EmptyBorder(10, 12, 10, 12));
        }
    }

    private void toggleCollapsed() {
        collapsed = !collapsed;

        setPreferredSize(new Dimension(collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH, 0));
        brandLabel.setVisible(!collapsed);
        toggleBtn.setToolTipText(collapsed ? "Expand sidebar" : "Collapse sidebar");

        for (JLabel section : sectionLabels) {
            section.setVisible(!collapsed);
        }

        for (JButton btn : menuButtons) {
            if (collapsed) {
                btn.setText("");
                btn.setHorizontalAlignment(SwingConstants.CENTER);
                btn.setIconTextGap(0);
                btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
                btn.setPreferredSize(new Dimension(COLLAPSED_WIDTH - 22, 42));
            } else {
                String tooltip = btn.getToolTipText();
                btn.setText(tooltip == null ? "" : tooltip);
                btn.setHorizontalAlignment(SwingConstants.LEFT);
                btn.setIconTextGap(12);
                btn.setPreferredSize(new Dimension(220, 42));
            }
            resetButtonPadding(btn);
        }

        if (activeButton != null) {
            setActive(activeButton);
        }

        revalidate();
        repaint();
        frame.revalidate();
        frame.repaint();
    }

    private static Icon createHamburgerIcon(int w, int h, Color color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        int left = 2;
        int right = w - 2;
        g2.fillRoundRect(left, 3, right - left, 2, 2, 2);
        g2.fillRoundRect(left, h / 2 - 1, right - left, 2, 2, 2);
        g2.fillRoundRect(left, h - 5, right - left, 2, 2, 2);
        g2.dispose();
        return new ImageIcon(img);
    }
}


