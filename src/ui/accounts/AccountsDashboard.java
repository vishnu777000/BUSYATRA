package ui.accounts;

import config.UIConfig;
import dao.AccountsDAO;
import util.IconUtil;
import util.Refreshable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

public class AccountsDashboard extends JPanel implements Refreshable {

    private double bookedRevenue;
    private double refundedAmount;
    private ChartType chartType = ChartType.PIE;

    private JLabel bookedValue;
    private JLabel refundedValue;
    private JLabel netValue;
    private JLabel refundRateLabel;
    private JLabel collectionLabel;
    private JLabel statusLabel;

    private JButton pieBtn;
    private JButton barBtn;
    private JButton refreshBtn;
    private SwingWorker<double[], Void> activeWorker;
    private final AtomicLong refreshSequence = new AtomicLong(0);

    private ChartPanel chartPanel;

    enum ChartType { PIE, BAR }

    public AccountsDashboard() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(mainPanel(), BorderLayout.CENTER);
        refreshData();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel logo = new JLabel(IconUtil.load("buslogo.png", 24, 24));
        JLabel title = new JLabel("Accounts Dashboard");
        title.setFont(UIConfig.FONT_TITLE);
        left.add(logo);
        left.add(title);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        refreshBtn = createBtn("Refresh", "secondary");
        pieBtn = createBtn("Pie", "primary");
        barBtn = createBtn("Bar", "primary");

        refreshBtn.addActionListener(e -> refreshData());
        pieBtn.addActionListener(e -> switchChart(ChartType.PIE));
        barBtn.addActionListener(e -> switchChart(ChartType.BAR));

        actions.add(refreshBtn);
        actions.add(pieBtn);
        actions.add(barBtn);

        panel.add(left, BorderLayout.WEST);
        panel.add(actions, BorderLayout.EAST);
        return panel;
    }

    private JButton createBtn(String text, String type) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(105, 34));
        if ("secondary".equals(type)) {
            UIConfig.secondaryBtn(b);
        } else {
            UIConfig.primaryBtn(b);
        }
        return b;
    }

    private JPanel mainPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 3, 20, 20));
        cards.setOpaque(false);

        bookedValue = valueLabel();
        refundedValue = valueLabel();
        netValue = valueLabel();

        cards.add(metricCard("Booked Revenue", bookedValue, UIConfig.SUCCESS, "payment.png"));
        cards.add(metricCard("Refunded Amount", refundedValue, UIConfig.DANGER, "warning.png"));
        cards.add(metricCard("Net Revenue", netValue, UIConfig.PRIMARY, "download.png"));

        chartPanel = new ChartPanel();
        UIConfig.styleCard(chartPanel);

        JPanel financeHealth = new JPanel(new GridLayout(1, 2, 12, 0));
        UIConfig.styleCard(financeHealth);
        refundRateLabel = insight("Refund Ratio", "0.0%");
        collectionLabel = insight("Collection Health", "Normal");
        financeHealth.add(refundRateLabel);
        financeHealth.add(collectionLabel);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.add(financeHealth, BorderLayout.CENTER);
        footer.add(statusLabel, BorderLayout.SOUTH);

        panel.add(cards, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JLabel insight(String title, String value) {
        JLabel lbl = new JLabel(title + ": " + value);
        lbl.setFont(UIConfig.FONT_NORMAL);
        return lbl;
    }

    private JLabel valueLabel() {
        JLabel v = new JLabel("INR 0.00");
        v.setFont(new Font("Segoe UI", Font.BOLD, 22));
        return v;
    }

    private JPanel metricCard(String title, JLabel value, Color color, String iconName) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        UIConfig.styleCard(card);

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JLabel icon = new JLabel(IconUtil.load(iconName, 18, 18));
        JLabel t = new JLabel(title);
        t.setFont(UIConfig.FONT_SMALL);
        t.setForeground(UIConfig.TEXT_LIGHT);

        top.add(icon, BorderLayout.WEST);
        top.add(t, BorderLayout.CENTER);

        value.setForeground(color);

        card.add(top, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private void switchChart(ChartType type) {
        chartType = type;
        chartPanel.repaint();
    }

    @Override
    public void refreshData() {
        if (activeWorker != null && !activeWorker.isDone()) {
            activeWorker.cancel(true);
        }
        long requestId = refreshSequence.incrementAndGet();
        setBusy(true, "Loading accounts data...");

        activeWorker = new SwingWorker<>() {
            @Override
            protected double[] doInBackground() {
                AccountsDAO dao = new AccountsDAO();
                return new double[]{dao.getTotalRevenueBooked(), dao.getTotalRefunded()};
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != refreshSequence.get()) {
                    return;
                }
                try {
                    double[] data = get();
                    bookedRevenue = sanitize(data[0]);
                    refundedAmount = sanitize(data[1]);

                    bookedValue.setText("INR " + String.format("%.2f", bookedRevenue));
                    refundedValue.setText("INR " + String.format("%.2f", refundedAmount));
                    netValue.setText("INR " + String.format("%.2f", bookedRevenue - refundedAmount));

                    double refundRate = bookedRevenue <= 0 ? 0 : (refundedAmount * 100.0 / bookedRevenue);
                    refundRateLabel.setText("Refund Ratio: " + String.format("%.1f", refundRate) + "%");
                    String health = refundRate < 10 ? "Strong"
                            : refundRate < 20 ? "Watchlist"
                            : "Critical";
                    collectionLabel.setText("Collection Health: " + health);

                    chartPanel.repaint();
                    setBusy(false, "Accounts updated");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    setBusy(false, "Accounts update interrupted");
                } catch (ExecutionException ex) {
                    setBusy(false, "Failed to load accounts data");
                    JOptionPane.showMessageDialog(
                            AccountsDashboard.this,
                            "Failed to load accounts data",
                            "Accounts",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        activeWorker.execute();
    }

    private void setBusy(boolean busy, String message) {
        if (statusLabel != null) statusLabel.setText(message == null ? " " : message);
        if (refreshBtn != null) {
            refreshBtn.setEnabled(!busy);
            refreshBtn.setText(busy ? "Loading..." : "Refresh");
        }
        if (pieBtn != null) pieBtn.setEnabled(!busy);
        if (barBtn != null) barBtn.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private double sanitize(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0;
        return Math.max(0, value);
    }

    class ChartPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (chartType == ChartType.PIE) {
                drawPie(g2);
            } else {
                drawBar(g2);
            }
        }

        private void drawPie(Graphics2D g) {
            double total = bookedRevenue + refundedAmount;
            if (total <= 0) total = 1;

            int bookedAngle = (int) ((bookedRevenue * 360) / total);
            int size = Math.max(120, Math.min(getWidth(), getHeight()) - 120);
            int x = (getWidth() - size) / 2;
            int y = 72;

            g.setFont(UIConfig.FONT_SUBTITLE);
            g.setColor(UIConfig.TEXT);
            g.drawString("Revenue Distribution", 20, 30);

            g.setColor(UIConfig.SUCCESS);
            g.fillArc(x, y, size, size, 0, bookedAngle);

            g.setColor(UIConfig.DANGER);
            g.fillArc(x, y, size, size, bookedAngle, 360 - bookedAngle);

            g.setColor(UIConfig.TEXT);
            g.drawString("Booked: INR " + String.format("%.2f", bookedRevenue), 20, getHeight() - 40);
            g.drawString("Refund: INR " + String.format("%.2f", refundedAmount), 20, getHeight() - 20);
        }

        private void drawBar(Graphics2D g) {
            int baseY = getHeight() - 80;
            int barWidth = 90;

            double max = Math.max(bookedRevenue, refundedAmount);
            if (max <= 0) max = 1;

            int bookedHeight = (int) ((bookedRevenue / max) * 220);
            int refundHeight = (int) ((refundedAmount / max) * 220);

            g.setFont(UIConfig.FONT_SUBTITLE);
            g.setColor(UIConfig.TEXT);
            g.drawString("Revenue Comparison", 20, 30);

            int x1 = getWidth() / 2 - 130;
            int x2 = getWidth() / 2 + 30;

            g.setColor(UIConfig.SUCCESS);
            g.fillRoundRect(x1, baseY - bookedHeight, barWidth, bookedHeight, 20, 20);
            g.setColor(UIConfig.TEXT);
            g.drawString("INR " + String.format("%.2f", bookedRevenue), x1 - 8, baseY - bookedHeight - 10);

            g.setColor(UIConfig.DANGER);
            g.fillRoundRect(x2, baseY - refundHeight, barWidth, refundHeight, 20, 20);
            g.setColor(UIConfig.TEXT);
            g.drawString("INR " + String.format("%.2f", refundedAmount), x2 - 8, baseY - refundHeight - 10);
        }
    }
}
