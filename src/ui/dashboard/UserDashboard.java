package ui.dashboard;

import config.UIConfig;
import dao.BookingDAO;
import dao.NewsDAO;
import dao.WalletDAO;
import ui.common.MainFrame;
import ui.common.BannerSliderPanel;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class UserDashboard extends JPanel implements Refreshable {

    private final MainFrame frame;

    private JLabel walletValue;
    private JLabel tripValue;
    private JLabel bookingCountValue;
    private JLabel newsCountValue;
    private JLabel statusLabel;
    private JPanel newsContainer;
    private JButton refreshBtn;
    private long lastRefreshMs = 0L;

    public UserDashboard(MainFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout());
        setBackground(UIConfig.BACKGROUND);
        add(scrollContent(), BorderLayout.CENTER);
        refreshData();
    }

    private JScrollPane scrollContent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConfig.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        panel.add(heroSection());
        panel.add(Box.createVerticalStrut(18));
        panel.add(new BannerSliderPanel());
        panel.add(Box.createVerticalStrut(18));
        panel.add(statsSection());
        panel.add(Box.createVerticalStrut(18));
        panel.add(actionsSection());
        panel.add(Box.createVerticalStrut(18));
        panel.add(newsSection());

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JPanel heroSection() {
        JPanel hero = new GradientPanel(new Color(208, 38, 60), new Color(229, 84, 56));
        hero.setLayout(new BorderLayout(16, 12));
        hero.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        String user = Session.username == null || Session.username.isBlank() ? "Traveler" : Session.username;
        if ("Traveler".equals(user) && Session.userEmail != null && Session.userEmail.contains("@")) {
            user = Session.userEmail.substring(0, Session.userEmail.indexOf('@'));
        }
        JLabel title = new JLabel("Welcome back, " + user);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JLabel sub = new JLabel("Plan faster, pay securely, and track every trip in one place.");
        sub.setForeground(new Color(255, 241, 242));
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton bookNow = new JButton("Search Buses");
        UIConfig.secondaryBtn(bookNow);
        bookNow.addActionListener(e -> frame.showScreen(MainFrame.SCREEN_SEARCH));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(6));
        left.add(sub);
        left.add(Box.createVerticalStrut(14));
        left.add(bookNow);

        JPanel right = new JPanel(new GridLayout(2, 1, 8, 8));
        right.setOpaque(false);
        right.add(heroTag("Live seat availability"));
        right.add(heroTag("Wallet + instant refund flow"));

        hero.add(left, BorderLayout.CENTER);
        hero.add(right, BorderLayout.EAST);
        return hero;
    }

    private JComponent heroTag(String text) {
        JLabel tag = new JLabel(text, SwingConstants.CENTER);
        tag.setOpaque(true);
        tag.setBackground(new Color(255, 255, 255, 45));
        tag.setForeground(Color.WHITE);
        tag.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tag.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return tag;
    }

    private JPanel statsSection() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 16));
        row.setOpaque(false);

        walletValue = statValue("INR 0.00");
        tripValue = statValue("No active trip");
        bookingCountValue = statValue("0");
        newsCountValue = statValue("0");

        row.add(statCard("Wallet Balance", walletValue, new Color(16, 185, 129)));
        row.add(statCard("Upcoming Trip", tripValue, new Color(59, 130, 246)));
        row.add(statCard("My Bookings", bookingCountValue, new Color(124, 58, 237)));
        row.add(statCard("Live Updates", newsCountValue, new Color(234, 88, 12)));

        return row;
    }

    private JLabel statValue(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 20));
        return label;
    }

    private JPanel statCard(String title, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(233, 236, 241)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JLabel t = new JLabel(title);
        t.setForeground(UIConfig.TEXT_LIGHT);
        t.setFont(UIConfig.FONT_SMALL);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(t, BorderLayout.WEST);

        JPanel dot = new JPanel();
        dot.setBackground(accent);
        dot.setPreferredSize(new Dimension(10, 10));
        top.add(dot, BorderLayout.EAST);

        value.setForeground(accent.darker());

        card.add(top, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel actionsSection() {
        JPanel card = new JPanel(new BorderLayout(10, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(233, 236, 241)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JLabel title = new JLabel("Quick Actions");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        refreshBtn = new JButton("Refresh Dashboard");
        UIConfig.secondaryBtn(refreshBtn);
        refreshBtn.addActionListener(e -> refreshData());

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(title, BorderLayout.WEST);
        head.add(refreshBtn, BorderLayout.EAST);

        JPanel grid = new JPanel(new GridLayout(2, 3, 12, 12));
        grid.setOpaque(false);
        grid.add(actionTile("Book Ticket", MainFrame.SCREEN_SEARCH, UIConfig.PRIMARY));
        grid.add(actionTile("My Tickets", MainFrame.SCREEN_MY_TICKETS, UIConfig.INFO));
        grid.add(actionTile("Cancel Ticket", MainFrame.SCREEN_CANCEL, UIConfig.DANGER));
        grid.add(actionTile("Wallet", MainFrame.SCREEN_WALLET, UIConfig.SUCCESS));
        grid.add(actionTile("Complaints", MainFrame.SCREEN_COMPLAINT, UIConfig.WARNING.darker()));
        grid.add(actionTile("Settings", MainFrame.SCREEN_SETTINGS, new Color(107, 114, 128)));

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        card.add(head, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        card.add(statusLabel, BorderLayout.SOUTH);
        return card;
    }

    private JButton actionTile(String text, String screen, Color color) {
        JButton btn = new JButton("<html><div style='text-align:left'>" + text + "</div></html>");
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(new Color(248, 250, 252));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        btn.setForeground(color.darker());
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> frame.showScreen(screen));
        return btn;
    }

    private JPanel newsSection() {
        JPanel card = new JPanel(new BorderLayout(10, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(233, 236, 241)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JLabel title = new JLabel("Service Alerts and Offers");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        newsContainer = new JPanel();
        newsContainer.setLayout(new BoxLayout(newsContainer, BoxLayout.Y_AXIS));
        newsContainer.setOpaque(false);
        newsContainer.add(newsItem("Loading updates...", "just now"));

        card.add(title, BorderLayout.NORTH);
        card.add(newsContainer, BorderLayout.CENTER);
        return card;
    }

    private JPanel newsItem(String message, String date) {
        JPanel item = new JPanel(new BorderLayout(10, 6));
        item.setOpaque(false);
        item.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JLabel msg = new JLabel(message);
        msg.setFont(UIConfig.FONT_NORMAL);

        JLabel dt = new JLabel(date);
        dt.setForeground(UIConfig.TEXT_LIGHT);
        dt.setFont(UIConfig.FONT_SMALL);

        item.add(msg, BorderLayout.CENTER);
        item.add(dt, BorderLayout.EAST);
        return item;
    }

    @Override
    public void refreshData() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs < 12_000L) {
            setBusy(false, "Dashboard is up to date");
            return;
        }
        setBusy(true, "Refreshing dashboard...");

        SwingWorker<DashboardData, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardData doInBackground() {
                WalletDAO walletDAO = new WalletDAO();
                BookingDAO bookingDAO = new BookingDAO();
                NewsDAO newsDAO = new NewsDAO();

                DashboardData data = new DashboardData();
                data.balance = walletDAO.getBalance(Session.userId);
                data.upcoming = bookingDAO.getUpcomingBookingText(Session.userId);
                data.bookings = bookingDAO.getBookingsByUser(Session.userId);
                data.news = newsDAO.getActiveNews();
                return data;
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    walletValue.setText("INR " + String.format("%.2f", data.balance));
                    tripValue.setText(data.upcoming == null || data.upcoming.isBlank() ? "No active trip" : data.upcoming);
                    bookingCountValue.setText(String.valueOf(data.bookings == null ? 0 : data.bookings.size()));
                    newsCountValue.setText(String.valueOf(data.news == null ? 0 : data.news.size()));

                    newsContainer.removeAll();
                    if (data.news == null || data.news.isEmpty()) {
                        newsContainer.add(newsItem("No updates available right now.", "system"));
                    } else {
                        for (String[] n : data.news) {
                            if (n != null && n.length >= 3) {
                                newsContainer.add(newsItem(n[1], n[2]));
                            }
                        }
                    }
                    newsContainer.revalidate();
                    newsContainer.repaint();
                    lastRefreshMs = System.currentTimeMillis();
                    setBusy(false, "Dashboard updated");
                } catch (Exception ex) {
                    setBusy(false, "Failed to refresh dashboard");
                }
            }
        };
        worker.execute();
    }

    private void setBusy(boolean busy, String message) {
        if (refreshBtn != null) {
            refreshBtn.setEnabled(!busy);
            refreshBtn.setText(busy ? "Refreshing..." : "Refresh Dashboard");
        }
        if (statusLabel != null) {
            statusLabel.setText(message == null ? " " : message);
        }
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private static class DashboardData {
        double balance;
        String upcoming;
        List<String[]> bookings = new ArrayList<>();
        List<String[]> news = new ArrayList<>();
    }

    private static class GradientPanel extends JPanel {
        private final Color start;
        private final Color end;

        private GradientPanel(Color start, Color end) {
            this.start = start;
            this.end = end;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
