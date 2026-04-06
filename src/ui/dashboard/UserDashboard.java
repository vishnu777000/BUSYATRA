package ui.dashboard;

import config.UIConfig;
import dao.BookingDAO;
import dao.CouponDAO;
import dao.NewsDAO;
import dao.WalletDAO;
import ui.common.MainFrame;
import util.IconUtil;
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
    private JPanel offersContainer;
    private JLabel offersStatusLabel;
    private JButton refreshBtn;
    private long lastRefreshMs = 0L;
    private volatile long refreshToken = 0L;

    public UserDashboard(MainFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout());
        setBackground(UIConfig.BACKGROUND);
        add(scrollContent(), BorderLayout.CENTER);
    }

    private JScrollPane scrollContent() {
        DashboardScrollPanel panel = new DashboardScrollPanel(new GridBagLayout());
        panel.setBackground(UIConfig.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 18, 0);

        panel.add(heroSection(), gbc);
        gbc.gridy++;
        panel.add(newsSection(), gbc);
        gbc.gridy++;
        panel.add(statsSection(), gbc);
        gbc.gridy++;
        panel.add(offersSection(), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(actionsSection(), gbc);

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(UIConfig.BACKGROUND);
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
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(233, 236, 241)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Trip Totals");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(UIConfig.TEXT);

        JLabel subtitle = new JLabel("Your wallet, bookings, next ride and live updates in one view.");
        subtitle.setFont(UIConfig.FONT_SMALL);
        subtitle.setForeground(UIConfig.TEXT_LIGHT);

        titleWrap.add(title);
        titleWrap.add(Box.createVerticalStrut(3));
        titleWrap.add(subtitle);

        JPanel logoWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoWrap.setOpaque(false);
        logoWrap.add(new JLabel(IconUtil.load("buslogo.png", 28, 28)));

        header.add(logoWrap, BorderLayout.WEST);
        header.add(titleWrap, BorderLayout.CENTER);

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

        card.add(header, BorderLayout.NORTH);
        card.add(row, BorderLayout.CENTER);
        return card;
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

    private JPanel offersSection() {
        JPanel card = new JPanel(new BorderLayout(10, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(233, 236, 241)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("My Offers");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JLabel subtitle = new JLabel("Available coupons, one-time redemptions, and trip unlocks.");
        subtitle.setFont(UIConfig.FONT_SMALL);
        subtitle.setForeground(UIConfig.TEXT_LIGHT);

        titleWrap.add(title);
        titleWrap.add(Box.createVerticalStrut(3));
        titleWrap.add(subtitle);

        JButton browseBtn = new JButton("Search Buses");
        UIConfig.secondaryBtn(browseBtn);
        browseBtn.addActionListener(e -> frame.showScreen(MainFrame.SCREEN_SEARCH));

        header.add(titleWrap, BorderLayout.CENTER);
        header.add(browseBtn, BorderLayout.EAST);

        offersContainer = new JPanel();
        offersContainer.setOpaque(false);
        offersContainer.setLayout(new BoxLayout(offersContainer, BoxLayout.Y_AXIS));
        offersContainer.add(offerMessage("Loading offers..."));

        offersStatusLabel = new JLabel(" ");
        offersStatusLabel.setFont(UIConfig.FONT_SMALL);
        offersStatusLabel.setForeground(UIConfig.TEXT_LIGHT);

        card.add(header, BorderLayout.NORTH);
        card.add(offersContainer, BorderLayout.CENTER);
        card.add(offersStatusLabel, BorderLayout.SOUTH);
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
        JPanel card = new GradientPanel(new Color(220, 38, 38), new Color(249, 115, 22));
        card.setLayout(new BorderLayout(12, 12));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("LIVE NEWS");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel sub = new JLabel("Top service alerts, route changes and offers");
        sub.setForeground(new Color(255, 237, 213));
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        titleWrap.add(title);
        titleWrap.add(Box.createVerticalStrut(4));
        titleWrap.add(sub);

        JLabel badge = new JLabel("REAL-TIME");
        badge.setOpaque(true);
        badge.setBackground(new Color(255, 255, 255, 48));
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        header.add(titleWrap, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);

        newsContainer = new JPanel();
        newsContainer.setLayout(new BoxLayout(newsContainer, BoxLayout.Y_AXIS));
        newsContainer.setOpaque(false);
        newsContainer.add(newsItem("Loading live updates...", "just now"));

        card.add(header, BorderLayout.NORTH);
        card.add(newsContainer, BorderLayout.CENTER);
        return card;
    }

    private JPanel newsItem(String message, String date) {
        JPanel item = new JPanel(new BorderLayout(12, 8));
        item.setOpaque(true);
        item.setBackground(new Color(255, 255, 255, 40));
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 58)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel tag = new JLabel("FLASH");
        tag.setOpaque(true);
        tag.setBackground(new Color(127, 29, 29, 140));
        tag.setForeground(Color.WHITE);
        tag.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tag.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

        JLabel msg = new JLabel("<html><div style='font-weight:600;'>" + escapeHtml(message) + "</div></html>");
        msg.setFont(UIConfig.FONT_NORMAL);
        msg.setForeground(Color.WHITE);

        JLabel dt = new JLabel(formatNewsDate(date));
        dt.setForeground(new Color(255, 237, 213));
        dt.setFont(UIConfig.FONT_SMALL);

        JPanel textWrap = new JPanel(new BorderLayout(8, 6));
        textWrap.setOpaque(false);
        textWrap.add(msg, BorderLayout.CENTER);
        textWrap.add(dt, BorderLayout.SOUTH);

        item.add(tag, BorderLayout.WEST);
        item.add(textWrap, BorderLayout.CENTER);
        return item;
    }

    private JComponent offerMessage(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIConfig.FONT_SMALL);
        label.setForeground(UIConfig.TEXT_LIGHT);
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        return label;
    }

    private JPanel offerItem(CouponDAO.CouponOffer offer) {
        JPanel item = new JPanel(new BorderLayout(12, 10));
        item.setBackground(new Color(249, 250, 251));
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(offer.code + "  |  Save INR " + String.format("%.2f", offer.discountAmount));
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(UIConfig.TEXT);

        JLabel helper = new JLabel(offer.helperText == null ? "" : offer.helperText);
        helper.setFont(UIConfig.FONT_SMALL);
        helper.setForeground(UIConfig.TEXT_LIGHT);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badges.setOpaque(false);
        badges.add(offerBadge(
                offer.badgeText == null || offer.badgeText.isBlank() ? "Offer" : offer.badgeText,
                offer.eligible ? new Color(220, 252, 231) : new Color(241, 245, 249),
                offer.eligible ? new Color(21, 128, 61) : new Color(71, 85, 105)
        ));

        if (offer.minBookingAmount > 0) {
            badges.add(offerBadge(
                    "Min INR " + String.format("%.0f", offer.minBookingAmount),
                    new Color(255, 247, 237),
                    new Color(194, 65, 12)
            ));
        }

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(helper);
        left.add(Box.createVerticalStrut(6));
        left.add(badges);

        JButton action = new JButton();
        action.setPreferredSize(new Dimension(120, 36));
        if (offer.usedUpByUser) {
            action.setText("Used");
            UIConfig.secondaryBtn(action);
            action.setEnabled(false);
        } else if (offer.eligible) {
            action.setText("Book With It");
            UIConfig.primaryBtn(action);
            action.addActionListener(e -> frame.showScreen(MainFrame.SCREEN_SEARCH));
        } else if (offer.expired || offer.exhausted) {
            action.setText("Unavailable");
            UIConfig.secondaryBtn(action);
            action.setEnabled(false);
        } else {
            action.setText("Unlock");
            UIConfig.secondaryBtn(action);
            action.addActionListener(e -> frame.showScreen(MainFrame.SCREEN_SEARCH));
        }

        item.add(left, BorderLayout.CENTER);
        item.add(action, BorderLayout.EAST);
        return item;
    }

    private JLabel offerBadge(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return label;
    }

    private void renderOffers(List<CouponDAO.CouponOffer> offers) {
        if (offersContainer == null) {
            return;
        }

        offersContainer.removeAll();
        if (offers == null || offers.isEmpty()) {
            offersContainer.add(offerMessage("No offers available right now."));
            if (offersStatusLabel != null) {
                offersStatusLabel.setText("Active coupons will appear here automatically.");
            }
        } else {
            int shown = 0;
            int eligibleCount = 0;
            for (CouponDAO.CouponOffer offer : offers) {
                if (offer == null) {
                    continue;
                }
                offersContainer.add(offerItem(offer));
                shown++;
                if (offer.eligible) {
                    eligibleCount++;
                }
                if (shown >= 3) {
                    break;
                }
                offersContainer.add(Box.createVerticalStrut(10));
            }
            if (offersStatusLabel != null) {
                offersStatusLabel.setText(
                        eligibleCount > 0
                                ? eligibleCount + " offer(s) ready for your next trip."
                                : "Some offers unlock on longer-distance bookings."
                );
            }
        }

        offersContainer.revalidate();
        offersContainer.repaint();
    }

    private void prepareOffersLoadingState() {
        if (offersContainer == null) {
            return;
        }
        offersContainer.removeAll();
        offersContainer.add(offerMessage("Loading offers..."));
        offersContainer.revalidate();
        offersContainer.repaint();
        if (offersStatusLabel != null) {
            offersStatusLabel.setText("Checking the best offers for your account...");
        }
    }

    private void renderNews(List<String[]> news) {
        if (newsContainer == null) {
            return;
        }

        newsContainer.removeAll();
        if (news == null || news.isEmpty()) {
            newsContainer.add(newsItem("No updates available right now.", "system"));
        } else {
            int shown = 0;
            for (String[] n : news) {
                if (n != null && n.length >= 3) {
                    newsContainer.add(newsItem(n[1], n[2]));
                    shown++;
                    if (shown >= 3) break;
                }
            }
            if (shown == 0) {
                newsContainer.add(newsItem("No updates available right now.", "system"));
            }
        }
        newsContainer.revalidate();
        newsContainer.repaint();
    }

    private void loadOffersAsync(long token) {
        SwingWorker<List<CouponDAO.CouponOffer>, Void> offersWorker = new SwingWorker<>() {
            @Override
            protected List<CouponDAO.CouponOffer> doInBackground() {
                return new CouponDAO().getPersonalizedOffers(Session.userId, 0, 4);
            }

            @Override
            protected void done() {
                if (token != refreshToken) {
                    return;
                }
                try {
                    renderOffers(get());
                } catch (Exception ex) {
                    renderOffers(List.of());
                    if (offersStatusLabel != null) {
                        offersStatusLabel.setText("Unable to load offers right now.");
                    }
                }
            }
        };
        offersWorker.execute();
    }

    @Override
    public void refreshData() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs < 12_000L) {
            setBusy(false, "Dashboard is up to date");
            return;
        }

        long token = ++refreshToken;
        setBusy(true, "Refreshing dashboard...");
        prepareOffersLoadingState();

        SwingWorker<DashboardData, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardData doInBackground() {
                WalletDAO walletDAO = new WalletDAO();
                BookingDAO bookingDAO = new BookingDAO();
                NewsDAO newsDAO = new NewsDAO();

                DashboardData data = new DashboardData();
                data.balance = walletDAO.getBalance(Session.userId);
                data.upcoming = bookingDAO.getUpcomingBookingText(Session.userId);
                data.bookingCount = bookingDAO.countBookingsByUser(Session.userId);
                data.news = newsDAO.getActiveNews();
                return data;
            }

            @Override
            protected void done() {
                if (token != refreshToken) {
                    return;
                }
                try {
                    DashboardData data = get();
                    walletValue.setText("INR " + String.format("%.2f", data.balance));
                    tripValue.setText(data.upcoming == null || data.upcoming.isBlank() ? "No active trip" : data.upcoming);
                    bookingCountValue.setText(String.valueOf(Math.max(0, data.bookingCount)));
                    newsCountValue.setText(String.valueOf(data.news == null ? 0 : data.news.size()));
                    renderNews(data.news);
                    lastRefreshMs = System.currentTimeMillis();
                    setBusy(false, "Dashboard updated");
                    loadOffersAsync(token);
                } catch (Exception ex) {
                    setBusy(false, "Failed to refresh dashboard");
                    if (token == refreshToken && offersStatusLabel != null) {
                        offersStatusLabel.setText("Unable to load offers right now.");
                    }
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

    @Override
    public boolean refreshOnFirstShow() {
        return true;
    }

    private String formatNewsDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return "live update";
        }

        String value = rawDate.trim().replace('T', ' ');
        return value.length() > 16 ? value.substring(0, 16) : value;
    }

    private String escapeHtml(String value) {
        if (value == null || value.isBlank()) {
            return "No details available";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static class DashboardData {
        double balance;
        String upcoming;
        int bookingCount;
        List<String[]> news = new ArrayList<>();
    }

    private static class DashboardScrollPanel extends JPanel implements Scrollable {
        private DashboardScrollPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            if (orientation == SwingConstants.VERTICAL) {
                return Math.max(visibleRect.height - 48, 48);
            }
            return Math.max(visibleRect.width - 48, 48);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
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
