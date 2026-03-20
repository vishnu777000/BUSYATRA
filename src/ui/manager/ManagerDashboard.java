package ui.manager;

import config.UIConfig;
import dao.AdminStatsDAO;
import ui.common.MainFrame;
import util.Refreshable;

import javax.swing.*;
import java.awt.*;

public class ManagerDashboard extends JPanel implements Refreshable {

    private JLabel usersValue;
    private JLabel bookingsValue;
    private JLabel cancelledValue;
    private JLabel revenueValue;
    private JProgressBar bookingHealthBar;
    private JProgressBar cancellationBar;
    private JLabel statusLabel;
    private JButton refreshBtn;

    public ManagerDashboard() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        add(header(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        refreshData();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Manager Operations Board");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JLabel sub = new JLabel("Snapshot of traffic, cancellations and revenue efficiency");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(UIConfig.TEXT_LIGHT);

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        refreshBtn = new JButton("Refresh");
        UIConfig.secondaryBtn(refreshBtn);
        refreshBtn.addActionListener(e -> refreshData());

        panel.add(left, BorderLayout.WEST);
        panel.add(refreshBtn, BorderLayout.EAST);
        return panel;
    }

    private JPanel center() {
        JPanel panel = new JPanel(new BorderLayout(18, 18));
        panel.setOpaque(false);
        panel.add(kpiRow(), BorderLayout.NORTH);
        panel.add(bodySection(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel bodySection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 16));
        panel.setOpaque(false);
        panel.add(healthSection());
        panel.add(actionsCard());
        return panel;
    }

    private JPanel kpiRow() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 14, 0));
        grid.setOpaque(false);

        usersValue = createValueLabel();
        bookingsValue = createValueLabel();
        cancelledValue = createValueLabel();
        revenueValue = createValueLabel();

        grid.add(kpiCard("Total Users", usersValue, UIConfig.INFO));
        grid.add(kpiCard("Total Bookings", bookingsValue, UIConfig.PRIMARY));
        grid.add(kpiCard("Cancelled", cancelledValue, UIConfig.DANGER));
        grid.add(kpiCard("Revenue", revenueValue, UIConfig.SUCCESS));
        return grid;
    }

    private JLabel createValueLabel() {
        JLabel lbl = new JLabel("0");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        return lbl;
    }

    private JPanel kpiCard(String title, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        UIConfig.styleCard(card);

        JLabel t = new JLabel(title);
        t.setFont(UIConfig.FONT_SMALL);
        t.setForeground(UIConfig.TEXT_LIGHT);

        value.setForeground(accent.darker());

        card.add(t, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel healthSection() {
        JPanel card = new JPanel(new BorderLayout(10, 12));
        UIConfig.styleCard(card);

        JLabel title = new JLabel("Operational Health");
        title.setFont(UIConfig.FONT_SUBTITLE);

        bookingHealthBar = new JProgressBar(0, 100);
        bookingHealthBar.setStringPainted(true);
        bookingHealthBar.setForeground(UIConfig.SUCCESS);

        cancellationBar = new JProgressBar(0, 100);
        cancellationBar.setStringPainted(true);
        cancellationBar.setForeground(UIConfig.DANGER);

        JPanel body = new JPanel(new GridLayout(2, 1, 0, 12));
        body.setOpaque(false);
        body.add(progressRow("Fulfillment rate", bookingHealthBar));
        body.add(progressRow("Cancellation rate", cancellationBar));

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        card.add(title, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(statusLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel actionsCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        UIConfig.styleCard(card);

        JLabel title = new JLabel("Manager Actions");
        title.setFont(UIConfig.FONT_SUBTITLE);

        JPanel actions = new JPanel(new GridLayout(4, 1, 10, 10));
        actions.setOpaque(false);
        actions.add(actionBtn("Review Reports", "REPORTS"));
        actions.add(actionBtn("Open Accounts", "ACCOUNTS"));
        actions.add(actionBtn("Resolve Complaints", "ADMIN_COMPLAINTS"));
        actions.add(actionBtn("Adjust Schedules", "MANAGE_SCHEDULES"));

        JTextArea notes = new JTextArea(
                "Operational priorities:\n" +
                "- Keep fulfillment above 85%.\n" +
                "- Investigate cancellation surges by route/time.\n" +
                "- Align schedules with peak demand windows."
        );
        notes.setEditable(false);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setFont(UIConfig.FONT_NORMAL);

        card.add(title, BorderLayout.NORTH);
        card.add(actions, BorderLayout.CENTER);
        card.add(notes, BorderLayout.SOUTH);
        return card;
    }

    private JButton actionBtn(String text, String screen) {
        JButton btn = new JButton(text);
        UIConfig.secondaryBtn(btn);
        btn.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof MainFrame) {
                ((MainFrame) w).showScreen(screen);
            }
        });
        return btn;
    }

    private JPanel progressRow(String title, JProgressBar bar) {
        JPanel panel = new JPanel(new BorderLayout(8, 6));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(UIConfig.FONT_NORMAL);
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(bar, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refreshData() {
        setBusy(true, "Refreshing manager board...");

        SwingWorker<double[], Void> worker = new SwingWorker<>() {
            @Override
            protected double[] doInBackground() {
                AdminStatsDAO dao = new AdminStatsDAO();
                return new double[]{
                        dao.getTotalUsers(),
                        dao.getTotalTickets(),
                        dao.getCancelledTickets(),
                        dao.getTotalRevenue()
                };
            }

            @Override
            protected void done() {
                try {
                    double[] data = get();
                    int users = (int) data[0];
                    int total = (int) data[1];
                    int cancelled = (int) data[2];
                    double revenue = data[3];

                    usersValue.setText(String.valueOf(users));
                    bookingsValue.setText(String.valueOf(total));
                    cancelledValue.setText(String.valueOf(cancelled));
                    revenueValue.setText("INR " + String.format("%.0f", revenue));

                    int cancelRate = total <= 0 ? 0 : (int) ((cancelled * 100.0) / total);
                    int fulfillment = 100 - cancelRate;

                    bookingHealthBar.setValue(fulfillment);
                    bookingHealthBar.setString(fulfillment + "%");

                    cancellationBar.setValue(cancelRate);
                    cancellationBar.setString(cancelRate + "%");

                    setBusy(false, "Manager board updated");
                } catch (Exception ex) {
                    setBusy(false, "Failed to load manager metrics");
                }
            }
        };
        worker.execute();
    }

    private void setBusy(boolean busy, String message) {
        refreshBtn.setEnabled(!busy);
        refreshBtn.setText(busy ? "Refreshing..." : "Refresh");
        statusLabel.setText(message == null ? " " : message);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }
}
