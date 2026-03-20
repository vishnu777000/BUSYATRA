package ui.admin;

import config.UIConfig;
import dao.ReportsDAO;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ReportsPanel extends JPanel implements Refreshable {

    private JTable table;
    private DefaultTableModel model;
    private JLabel totalLbl;
    private JLabel revenueLbl;
    private JLabel cancelledLbl;
    private JTextField searchField;
    private JLabel loadingLabel;
    private JButton searchBtn;

    public ReportsPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(statsPanel(), BorderLayout.CENTER);
        add(tableCard(), BorderLayout.SOUTH);

        loadData("");
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel title = new JLabel("Reports Dashboard");
        title.setFont(UIConfig.FONT_TITLE);

        searchField = new JTextField();
        UIConfig.styleField(searchField);
        searchField.setPreferredSize(new Dimension(240, 36));
        searchField.addActionListener(e -> search());

        searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Refresh");
        UIConfig.primaryBtn(searchBtn);
        UIConfig.secondaryBtn(refreshBtn);

        searchBtn.addActionListener(e -> search());
        refreshBtn.addActionListener(e -> loadData(""));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(searchField);
        right.add(searchBtn);
        right.add(refreshBtn);

        p.add(title, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel statsPanel() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 20, 20));
        grid.setOpaque(false);

        totalLbl = statCard("Total Bookings", grid);
        revenueLbl = statCard("Revenue (INR)", grid);
        cancelledLbl = statCard("Cancelled", grid);

        return grid;
    }

    private JLabel statCard(String title, JPanel parent) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        UIConfig.styleCard(card);

        JLabel t = new JLabel(title);
        t.setForeground(UIConfig.TEXT_LIGHT);
        t.setFont(UIConfig.FONT_SMALL);

        JLabel v = new JLabel("0");
        v.setFont(new Font("Segoe UI", Font.BOLD, 26));
        v.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        parent.add(card);
        return v;
    }

    private JPanel tableCard() {
        model = new DefaultTableModel(new Object[]{"ID", "Passenger", "Route", "Amount", "Status", "Time"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(model);
        UIConfig.styleTable(table);
        table.setRowHeight(30);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(right);

        JScrollPane sp = new JScrollPane(table);
        UIConfig.styleScroll(sp);

        loadingLabel = new JLabel(" ");
        loadingLabel.setForeground(UIConfig.TEXT_LIGHT);

        JPanel card = new JPanel(new BorderLayout(0, 10));
        UIConfig.styleCard(card);
        card.add(sp, BorderLayout.CENTER);
        card.add(loadingLabel, BorderLayout.SOUTH);
        return card;
    }

    private void setBusy(boolean busy, String message) {
        loadingLabel.setText(message == null ? " " : message);
        table.setEnabled(!busy);
        searchBtn.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void loadData(String query) {
        setBusy(true, query.isBlank() ? "Loading reports..." : "Searching...");
        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                ReportsDAO dao = new ReportsDAO();
                return query.isBlank() ? dao.getAllBookings() : dao.searchBooking(query);
            }

            @Override
            protected void done() {
                try {
                    List<String[]> list = get();
                    model.setRowCount(0);

                    int total = 0;
                    int cancelled = 0;
                    double revenue = 0;

                    for (String[] r : list) {
                        total++;
                        double amt = parseAmount(r[4]);
                        String status = r[5] == null ? "" : r[5];
                        if ("CANCELLED".equalsIgnoreCase(status)) {
                            cancelled++;
                        } else {
                            revenue += amt;
                        }
                        model.addRow(new Object[]{
                                r[0],
                                r[1],
                                r[2] + " -> " + r[3],
                                String.format("%.2f", amt),
                                status,
                                r[6]
                        });
                    }

                    totalLbl.setText(String.valueOf(total));
                    revenueLbl.setText(String.format("%.2f", revenue));
                    cancelledLbl.setText(String.valueOf(cancelled));
                    loadingLabel.setText("Loaded " + total + " bookings");
                } catch (Exception ex) {
                    loadingLabel.setText("Failed to load reports");
                    JOptionPane.showMessageDialog(ReportsPanel.this, "Failed to load reports");
                } finally {
                    setBusy(false, loadingLabel.getText());
                }
            }
        };
        worker.execute();
    }

    private double parseAmount(String raw) {
        if (raw == null) return 0;
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception ignore) {
            return 0;
        }
    }

    private void search() {
        loadData(searchField.getText() == null ? "" : searchField.getText().trim());
    }

    @Override
    public void refreshData() {
        loadData("");
    }
}
