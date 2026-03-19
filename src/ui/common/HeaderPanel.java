package ui.common;

import util.IconUtil;
import util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class HeaderPanel extends JPanel {

    private JLabel pageTitle;
    private JLabel notificationBadge;

    public HeaderPanel(Runnable onLogout) {

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 65));

        /* LIGHT HEADER */
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230,230,230)));
        setBorder(new EmptyBorder(10, 20, 10, 20));

        add(leftSection(), BorderLayout.WEST);
        add(centerSection(), BorderLayout.CENTER);
        add(rightSection(onLogout), BorderLayout.EAST);
    }

    /* ================= LEFT ================= */

    private JComponent leftSection() {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);

        JLabel brand = new JLabel("BusYatra");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brand.setForeground(new Color(220, 53, 69)); // red branding

        panel.add(brand);

        return panel;
    }

    /* ================= CENTER ================= */

    private JComponent centerSection() {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);

        pageTitle = new JLabel("Dashboard");
        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        pageTitle.setForeground(new Color(50, 50, 50));

        panel.add(pageTitle);

        return panel;
    }

    /* ================= RIGHT ================= */

    private JComponent rightSection(Runnable onLogout) {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        panel.setOpaque(false);

        panel.add(notificationSection());
        panel.add(profileSection(onLogout));

        return panel;
    }

    /* ================= PROFILE ================= */

    private JComponent profileSection(Runnable onLogout) {

        JPanel profile = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        profile.setBackground(new Color(245, 245, 245));
        profile.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        profile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel avatar = new JLabel(IconUtil.load("user.png", 24, 24));

        String userName = (Session.username != null) ? Session.username : "User";

        JLabel username = new JLabel(userName);
        username.setForeground(new Color(50,50,50));
        username.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JLabel arrow = new JLabel("▾");
        arrow.setForeground(Color.GRAY);

        profile.add(avatar);
        profile.add(username);
        profile.add(arrow);

        /* DROPDOWN */

        JPopupMenu menu = new JPopupMenu();

        JMenuItem profileItem = new JMenuItem("My Profile");
        JMenuItem settingsItem = new JMenuItem("Settings");
        JMenuItem logoutItem = new JMenuItem("Logout");

        logoutItem.addActionListener(e -> onLogout.run());

        menu.add(profileItem);
        menu.add(settingsItem);
        menu.addSeparator();
        menu.add(logoutItem);

        profile.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menu.show(profile, 0, profile.getHeight());
            }
        });

        return profile;
    }

    /* ================= NOTIFICATION ================= */

    private JComponent notificationSection() {

        JPanel container = new JPanel(null);
        container.setOpaque(false);
        container.setPreferredSize(new Dimension(36, 36));

        JButton bell = new JButton(
                IconUtil.load("notification.png", 20, 20)
        );

        bell.setBounds(8, 8, 20, 20);
        bell.setBorderPainted(false);
        bell.setContentAreaFilled(false);
        bell.setFocusPainted(false);

        notificationBadge = new JLabel("0");
        notificationBadge.setOpaque(true);
        notificationBadge.setBackground(new Color(255, 59, 48));
        notificationBadge.setForeground(Color.WHITE);
        notificationBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        notificationBadge.setHorizontalAlignment(SwingConstants.CENTER);

        notificationBadge.setBounds(20, 2, 16, 16);
        notificationBadge.setVisible(false);

        container.add(bell);
        container.add(notificationBadge);

        return container;
    }

    /* ================= METHODS ================= */

    public void setPageTitle(String title) {
        pageTitle.setText(title);
    }

    public void setNotificationCount(int count) {

        if (count <= 0) {
            notificationBadge.setVisible(false);
        } else {
            notificationBadge.setVisible(true);
            notificationBadge.setText(String.valueOf(count));
        }
    }
}
