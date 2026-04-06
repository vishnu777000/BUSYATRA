package ui.clerk;

import config.UIConfig;
import dao.ReportsDAO;
import ui.common.MainFrame;
import util.BookingContext;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

public class BookingClerkDashboard extends JPanel implements Refreshable {

    private JTable table;
    private DefaultTableModel model;
    private final MainFrame frame;
    private JLabel statusLabel;
    private JButton refreshBtn;
    private ClerkTodaySchedulePanel todaySchedulePanel;

    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private ClerkKPIPanel kpiPanel;

    public BookingClerkDashboard(MainFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout(16, 16));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(centerLayout(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);

        refreshData();
    }

    private JComponent header() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("Clerk Operations Desk");
        title.setFont(UIConfig.FONT_TITLE);

        JLabel sub = new JLabel("Handle walk-in bookings, quick ticket issues and day schedule from one place");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(UIConfig.TEXT_LIGHT);

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        top.add(left, BorderLayout.WEST);
        top.add(statusLabel, BorderLayout.EAST);

        kpiPanel = new ClerkKPIPanel();
        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(kpiPanel, BorderLayout.CENTER);

        return wrapper;
    }

    private JComponent centerLayout() {
        JPanel content = new JPanel(new GridLayout(1, 2, 16, 16));
        content.setOpaque(false);

        JPanel left = new JPanel(new BorderLayout(12, 12));
        left.setOpaque(false);
        left.add(new ClerkQuickActionsPanel(frame), BorderLayout.NORTH);
        todaySchedulePanel = new ClerkTodaySchedulePanel();
        left.add(todaySchedulePanel, BorderLayout.CENTER);

        content.add(left);
        content.add(tableCard());
        return content;
    }

    private JComponent tableCard() {
        model = new DefaultTableModel(
                new Object[]{"Ticket ID", "Passenger", "Route", "Amount (INR)", "Status", "Booking Time"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(model);
        UIConfig.styleTable(table);
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                syncQuickActionSelection();
            }
        });

        JScrollPane sp = new JScrollPane(table);
        UIConfig.styleScroll(sp);
        sp.setColumnHeaderView(table.getTableHeader());

        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);

        JLabel title = new JLabel("Recent Booking Queue");
        title.setFont(UIConfig.FONT_SUBTITLE);

        card.add(title, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JComponent actions() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        p.setOpaque(false);

        JButton newBooking = new JButton("New Booking");
        UIConfig.primaryBtn(newBooking);
        newBooking.addActionListener(e -> frame.showScreen(MainFrame.SCREEN_SEARCH));

        refreshBtn = new JButton("Refresh");
        UIConfig.secondaryBtn(refreshBtn);
        refreshBtn.addActionListener(e -> refreshData());

        p.add(newBooking);
        p.add(refreshBtn);
        return p;
    }

    @Override
    public void refreshData() {
        setBusy(true, "Loading clerk queue...");
        if (kpiPanel != null) {
            kpiPanel.refreshData();
        }
        if (todaySchedulePanel != null) {
            todaySchedulePanel.refreshData();
        }

        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                return new ReportsDAO().getRecentBookings(30);
            }

            @Override
            protected void done() {
                try {
                    List<String[]> list = get();
                    model.setRowCount(0);

                    if (list == null || list.isEmpty()) {
                        model.addRow(new Object[]{"-", "No bookings found", "-", "-", "-", "-"});
                        BookingContext.clearRecentTicketIds();
                    } else {
                        int shown = 0;
                        for (String[] r : list) {
                            if (r == null || r.length < 7) continue;
                            String route = r[2] + " -> " + r[3];
                            double amount = 0;
                            try {
                                amount = Double.parseDouble(r[4]);
                            } catch (Exception ignore) {
                                
                            }

                            model.addRow(new Object[]{
                                    r[0], r[1], route, df.format(amount), r[5], r[6]
                            });
                            shown++;
                            if (shown >= 30) break;
                        }
                        if (model.getRowCount() > 0 && !"-".equals(String.valueOf(model.getValueAt(0, 0)))) {
                            table.setRowSelectionInterval(0, 0);
                        } else {
                            BookingContext.clearRecentTicketIds();
                        }
                    }
                    setBusy(false, "Queue updated");
                } catch (Exception e) {
                    setBusy(false, "Failed to load clerk queue");
                    JOptionPane.showMessageDialog(BookingClerkDashboard.this, "Failed to load bookings");
                }
            }
        };
        worker.execute();
    }

    @Override
    public boolean refreshOnFirstShow() {
        return false;
    }

    private void syncQuickActionSelection() {
        if (table == null) {
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            BookingContext.clearRecentTicketIds();
            return;
        }
        Object raw = table.getValueAt(row, 0);
        if (raw == null) {
            BookingContext.clearRecentTicketIds();
            return;
        }
        try {
            int ticketId = Integer.parseInt(String.valueOf(raw).trim());
            if (ticketId > 0) {
                BookingContext.setActiveTicketId(ticketId);
            } else {
                BookingContext.clearRecentTicketIds();
            }
        } catch (Exception ignored) {
            BookingContext.clearRecentTicketIds();
        }
    }

    private void setBusy(boolean busy, String message) {
        if (table != null) table.setEnabled(!busy);
        if (refreshBtn != null) {
            refreshBtn.setEnabled(!busy);
            refreshBtn.setText(busy ? "Refreshing..." : "Refresh");
        }
        if (statusLabel != null) statusLabel.setText(message == null ? " " : message);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }
}
