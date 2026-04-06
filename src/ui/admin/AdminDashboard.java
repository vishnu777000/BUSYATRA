package ui.admin;

import config.UIConfig;
import dao.AdminStatsDAO;
import ui.common.MainFrame;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import java.awt.*;

public class AdminDashboard extends JPanel implements Refreshable {

    private JLabel usersLbl;
    private JLabel ticketsLbl;
    private JLabel revenueLbl;
    private JLabel cancelledLbl;
    private JProgressBar cancelRateBar;
    private JProgressBar revenueHealthBar;
    private JLabel statusLabel;
    private JButton refreshBtn;

    public AdminDashboard() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);
        refreshData();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Admin Command Center");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JLabel sub = new JLabel("Real-time control for bookings, users and revenue");
        sub.setForeground(UIConfig.TEXT_LIGHT);

        String name = (Session.username != null && !Session.username.isBlank()) ? Session.username : "Admin";
        String role = (Session.role != null && !Session.role.isBlank()) ? Session.role.toUpperCase() : "ADMIN";
        JLabel who = new JLabel("Signed in: " + name + " (" + role + ")");
        who.setFont(UIConfig.FONT_SMALL);
        who.setForeground(new Color(100, 116, 139));

        JPanel left = new JPanel(new GridLayout(3, 1));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);
        left.add(who);

        refreshBtn = new JButton("Refresh");
        UIConfig.secondaryBtn(refreshBtn);
        refreshBtn.addActionListener(e -> refreshData());

        panel.add(left, BorderLayout.WEST);
        panel.add(refreshBtn, BorderLayout.EAST);
        return panel;
    }

    private JPanel content() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.add(statsRow(), BorderLayout.NORTH);
        panel.add(bottomSection(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel statsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 16));
        row.setOpaque(false);

        usersLbl = createValue();
        ticketsLbl = createValue();
        revenueLbl = createValue();
        cancelledLbl = createValue();

        row.add(gradientCard("Active Users", usersLbl, new Color(37, 99, 235), new Color(59, 130, 246)));
        row.add(gradientCard("Total Tickets", ticketsLbl, new Color(124, 58, 237), new Color(147, 51, 234)));
        row.add(gradientCard("Revenue", revenueLbl, new Color(5, 150, 105), new Color(16, 185, 129)));
        row.add(gradientCard("Cancelled", cancelledLbl, new Color(220, 38, 38), new Color(239, 68, 68)));
        return row;
    }

    private JLabel createValue() {
        JLabel lbl = new JLabel("0");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lbl.setForeground(Color.WHITE);
        return lbl;
    }

    private JPanel gradientCard(String title, JLabel value, Color start, Color end) {
        JPanel card = new GradientPanel(start, end);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel t = new JLabel(title);
        t.setForeground(new Color(255, 245, 245));
        t.setFont(UIConfig.FONT_SMALL);

        card.add(t, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel bottomSection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 20));
        panel.setOpaque(false);
        panel.add(healthCard());
        panel.add(opsActionsCard());
        return panel;
    }

    private JPanel healthCard() {
        JPanel card = new JPanel(new BorderLayout(10, 14));
        UIConfig.styleCard(card);

        JLabel title = new JLabel("System Health");
        title.setFont(UIConfig.FONT_SUBTITLE);

        JPanel metrics = new JPanel(new GridLayout(2, 1, 0, 12));
        metrics.setOpaque(false);

        cancelRateBar = new JProgressBar(0, 100);
        cancelRateBar.setStringPainted(true);
        cancelRateBar.setForeground(UIConfig.DANGER);

        revenueHealthBar = new JProgressBar(0, 100);
        revenueHealthBar.setStringPainted(true);
        revenueHealthBar.setForeground(UIConfig.SUCCESS);

        metrics.add(progressRow("Cancellation rate", cancelRateBar));
        metrics.add(progressRow("Revenue target", revenueHealthBar));

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        card.add(title, BorderLayout.NORTH);
        card.add(metrics, BorderLayout.CENTER);
        card.add(statusLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel progressRow(String label, JProgressBar bar) {
        JPanel row = new JPanel(new BorderLayout(10, 6));
        row.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(UIConfig.FONT_NORMAL);
        row.add(l, BorderLayout.NORTH);
        row.add(bar, BorderLayout.CENTER);
        return row;
    }

    private JPanel opsActionsCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        UIConfig.styleCard(card);

        JLabel title = new JLabel("Operations Workbench");
        title.setFont(UIConfig.FONT_SUBTITLE);

        JPanel actions = new JPanel(new GridLayout(4, 2, 10, 10));
        actions.setOpaque(false);

        actions.add(actionBtn("Manage Banners", "ADMIN_BANNERS"));
        actions.add(actionBtn("Route Map & Lines", "MANAGE_ROUTES"));
        actions.add(actionBtn("Schedules", "MANAGE_SCHEDULES"));
        actions.add(actionBtn("Accounts Dashboard", "ACCOUNTS"));
        actions.add(actionBtn("Reports", "REPORTS"));
        actions.add(actionBtn("Complaints", "ADMIN_COMPLAINTS"));
        actions.add(actionBtn("Manager Dashboard", MainFrame.SCREEN_MANAGER));
        actions.add(actionBtn("Clerk Dashboard", MainFrame.SCREEN_CLERK));

        JTextArea notes = new JTextArea();
        notes.setEditable(false);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setFont(UIConfig.FONT_NORMAL);
        notes.setText(
                "Daily admin flow:\n" +
                "- Publish banner updates and service advisories.\n" +
                "- Review route map, stops and schedule consistency.\n" +
                "- Track cancellation spikes before peak departures."
        );

        card.add(title, BorderLayout.NORTH);
        card.add(actions, BorderLayout.CENTER);
        card.add(notes, BorderLayout.SOUTH);
        return card;
    }

    private JButton actionBtn(String text, String screenKey) {
        JButton btn = new JButton(text);
        UIConfig.secondaryBtn(btn);
        btn.addActionListener(e -> openScreen(screenKey));
        return btn;
    }

    private void openScreen(String screenKey) {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof MainFrame) {
            ((MainFrame) w).showScreen(screenKey);
        }
    }

    @Override
    public void refreshData() {
        setBusy(true, "Refreshing admin metrics...");
        SwingWorker<StatsData, Void> worker = new SwingWorker<>() {
            @Override
            protected StatsData doInBackground() {
                AdminStatsDAO dao = new AdminStatsDAO();
                StatsData d = new StatsData();
                AdminStatsDAO.StatsSnapshot snapshot = dao.getDashboardSnapshot();
                d.users = snapshot.users;
                d.tickets = snapshot.tickets;
                d.cancelled = snapshot.cancelled;
                d.revenue = snapshot.revenue;
                return d;
            }

            @Override
            protected void done() {
                try {
                    StatsData d = get();
                    usersLbl.setText(String.valueOf(d.users));
                    ticketsLbl.setText(String.valueOf(d.tickets));
                    cancelledLbl.setText(String.valueOf(d.cancelled));
                    revenueLbl.setText("INR " + String.format("%.0f", d.revenue));

                    int cancelRate = d.tickets <= 0 ? 0 : (int) ((d.cancelled * 100.0) / d.tickets);
                    cancelRateBar.setValue(cancelRate);
                    cancelRateBar.setString(cancelRate + "%");

                    int revenueProgress = (int) Math.min(100, (d.revenue / 100000.0) * 100);
                    revenueHealthBar.setValue(revenueProgress);
                    revenueHealthBar.setString(revenueProgress + "% of INR 100000");

                    setBusy(false, "Dashboard updated");
                } catch (Exception ex) {
                    setBusy(false, "Failed to load admin metrics");
                }
            }
        };
        worker.execute();
    }

    @Override
    public boolean refreshOnFirstShow() {
        return false;
    }

    private void setBusy(boolean busy, String message) {
        refreshBtn.setEnabled(!busy);
        refreshBtn.setText(busy ? "Refreshing..." : "Refresh");
        statusLabel.setText(message == null ? " " : message);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private static class StatsData {
        int users;
        int tickets;
        int cancelled;
        double revenue;
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
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
