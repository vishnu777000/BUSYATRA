package ui.common;

import config.UIConfig;
import dao.CouponDAO;
import dao.NewsDAO;
import util.IconUtil;
import util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HeaderPanel extends JPanel {

    private final MainFrame frame;
    private final Runnable onLogout;
    private final NewsDAO newsDAO = new NewsDAO();
    private final CouponDAO couponDAO = new CouponDAO();

    private JLabel pageTitle;
    private JLabel notificationBadge;
    private JButton bellBtn;
    private List<String[]> cachedNotifications = new ArrayList<>();
    private long lastNotificationRefreshMs = 0L;
    private boolean notificationsLoaded = false;
    private boolean notificationLoading = false;

    public HeaderPanel(MainFrame frame, Runnable onLogout) {
        this.frame = frame;
        this.onLogout = onLogout;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 72));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 20, 10, 20));

        add(leftSection(), BorderLayout.WEST);
        add(centerSection(), BorderLayout.CENTER);
        add(rightSection(), BorderLayout.EAST);

        refreshNotifications();
    }

    private JComponent leftSection() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);

        JLabel logo = new JLabel(IconUtil.load("buslogo.png", 28, 28));
        JLabel brand = new JLabel("BusYatra");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brand.setForeground(UIConfig.PRIMARY);

        panel.add(logo);
        panel.add(brand);
        return panel;
    }

    private JComponent centerSection() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);

        pageTitle = new JLabel("Dashboard");
        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        pageTitle.setForeground(UIConfig.TEXT);
        panel.add(pageTitle);
        return panel;
    }

    private JComponent rightSection() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        panel.setOpaque(false);
        panel.add(notificationSection());
        panel.add(profileSection());
        return panel;
    }

    private JComponent profileSection() {
        JPanel profile = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        profile.setBackground(new Color(248, 250, 252));
        profile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        profile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel avatar = new JLabel(IconUtil.loadTinted("user.png", 20, 20, new Color(71, 85, 105)));
        String userName = (Session.username != null && !Session.username.isBlank())
                ? Session.username
                : ((Session.userEmail != null && Session.userEmail.contains("@"))
                ? Session.userEmail.substring(0, Session.userEmail.indexOf('@'))
                : "User");
        String roleText = (Session.role == null || Session.role.isBlank()) ? "USER" : Session.role.trim().toUpperCase();

        JLabel username = new JLabel(userName);
        username.setForeground(UIConfig.TEXT);
        username.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JLabel roleBadge = new JLabel(roleText);
        roleBadge.setOpaque(true);
        roleBadge.setBackground(new Color(239, 246, 255));
        roleBadge.setForeground(new Color(30, 64, 175));
        roleBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        roleBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        JLabel arrow = new JLabel("v");
        arrow.setForeground(UIConfig.TEXT_LIGHT);

        profile.add(avatar);
        profile.add(username);
        profile.add(roleBadge);
        profile.add(arrow);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem profileItem = new JMenuItem("My Profile");
        JMenuItem settingsItem = new JMenuItem("Settings");
        JMenuItem logoutItem = new JMenuItem("Logout");

        profileItem.addActionListener(e -> new ProfileDialog(frame, frame));
        settingsItem.addActionListener(e -> frame.showScreen(MainFrame.SCREEN_SETTINGS));
        logoutItem.addActionListener(e -> onLogout.run());

        menu.add(profileItem);
        menu.add(settingsItem);
        menu.addSeparator();
        menu.add(logoutItem);

        profile.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menu.show(profile, 0, profile.getHeight());
            }
        });

        return profile;
    }

    private JComponent notificationSection() {
        JPanel container = new JPanel(null);
        container.setOpaque(false);
        container.setPreferredSize(new Dimension(36, 36));

        bellBtn = new JButton(IconUtil.loadTinted("notification.png", 18, 18, new Color(71, 85, 105)));
        bellBtn.setBounds(8, 8, 20, 20);
        bellBtn.setBorderPainted(false);
        bellBtn.setContentAreaFilled(false);
        bellBtn.setFocusPainted(false);
        bellBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bellBtn.addActionListener(e -> showNewsPopup());

        notificationBadge = new JLabel("0");
        notificationBadge.setOpaque(true);
        notificationBadge.setBackground(new Color(255, 59, 48));
        notificationBadge.setForeground(Color.WHITE);
        notificationBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        notificationBadge.setHorizontalAlignment(SwingConstants.CENTER);
        notificationBadge.setBounds(20, 2, 16, 16);
        notificationBadge.setVisible(false);

        container.add(bellBtn);
        container.add(notificationBadge);
        return container;
    }

    private void showNewsPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(new Color(225, 229, 236)));

        if (cachedNotifications == null || cachedNotifications.isEmpty()) {
            JMenuItem empty = new JMenuItem("No notifications");
            empty.setEnabled(false);
            popup.add(empty);
        } else {
            int limit = Math.min(6, cachedNotifications.size());
            for (int i = 0; i < limit; i++) {
                String[] n = cachedNotifications.get(i);
                String msg = n.length > 1 ? n[1] : "Update";
                if (msg.length() > 70) {
                    msg = msg.substring(0, 67) + "...";
                }
                JMenuItem item = new JMenuItem(msg);
                item.addActionListener(e -> frame.showScreen(notificationTargetScreen()));
                popup.add(item);
            }
        }

        popup.show(bellBtn, -170, bellBtn.getHeight() + 6);
    }

    private String notificationTargetScreen() {
        String role = Session.role == null ? "" : Session.role.trim().toUpperCase();
        switch (role) {
            case "ADMIN":
                return MainFrame.SCREEN_ADMIN;
            case "MANAGER":
                return MainFrame.SCREEN_MANAGER;
            case "CLERK":
            case "BOOKING_CLERK":
                return MainFrame.SCREEN_CLERK;
            case "USER":
            default:
                return MainFrame.SCREEN_USER;
        }
    }

    private void refreshNotifications() {
        long now = System.currentTimeMillis();
        if (notificationsLoaded && now - lastNotificationRefreshMs < 20_000L) {
            setNotificationCount(cachedNotifications == null ? 0 : cachedNotifications.size());
            return;
        }
        if (notificationLoading) {
            return;
        }
        notificationLoading = true;
        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                List<String[]> notifications = new ArrayList<>();
                List<String[]> news = newsDAO.getActiveNews();
                if (news != null) {
                    notifications.addAll(news);
                }
                if (Session.userId > 0 && "USER".equalsIgnoreCase(Session.role)) {
                    notifications.addAll(couponDAO.getNotificationItemsForUser(Session.userId, 3));
                }
                return notifications;
            }

            @Override
            protected void done() {
                try {
                    cachedNotifications = get();
                    lastNotificationRefreshMs = System.currentTimeMillis();
                    notificationsLoaded = true;
                    setNotificationCount(cachedNotifications == null ? 0 : cachedNotifications.size());
                } catch (Exception ignored) {
                    notificationsLoaded = true;
                    cachedNotifications = new ArrayList<>();
                    setNotificationCount(0);
                } finally {
                    notificationLoading = false;
                }
            }
        };
        worker.execute();
    }

    public void setPageTitle(String title) {
        pageTitle.setText(title);
        refreshNotifications();
    }

    public void setNotificationCount(int count) {
        if (count <= 0) {
            notificationBadge.setVisible(false);
        } else {
            notificationBadge.setVisible(true);
            notificationBadge.setText(String.valueOf(Math.min(count, 9)));
        }
    }
}
