package ui.clerk;

import config.UIConfig;
import dao.ClerkDashboardDAO;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ClerkTodaySchedulePanel extends JPanel implements Refreshable {

    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel statusLabel;

    public ClerkTodaySchedulePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIConfig.CARD);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Today's Schedule");
        title.setFont(UIConfig.FONT_TITLE);

        model = new DefaultTableModel(new Object[]{
                "Route",
                "Bus",
                "Departure",
                "Seats Left",
                "Status"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        UIConfig.styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        UIConfig.styleScroll(scroll);
        scroll.setColumnHeaderView(table.getTableHeader());

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);
        statusLabel.setFont(UIConfig.FONT_SMALL);

        add(title, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    @Override
    public void refreshData() {
        statusLabel.setText("Loading today's schedules...");

        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                return new ClerkDashboardDAO().getTodaySchedules(8);
            }

            @Override
            protected void done() {
                try {
                    List<String[]> rows = get();
                    model.setRowCount(0);

                    if (rows == null || rows.isEmpty()) {
                        model.addRow(new Object[]{"No schedules", "-", "-", "-", "-"});
                        statusLabel.setText("No active schedules found for today.");
                        return;
                    }

                    for (String[] row : rows) {
                        model.addRow(new Object[]{
                                safe(row, 0),
                                safe(row, 1),
                                safe(row, 2),
                                safe(row, 3),
                                safe(row, 4)
                        });
                    }
                    statusLabel.setText("Showing " + rows.size() + " active departures for today.");
                } catch (Exception ex) {
                    model.setRowCount(0);
                    model.addRow(new Object[]{"Unable to load", "-", "-", "-", "-"});
                    statusLabel.setText("Failed to load today's schedule.");
                }
            }
        };
        worker.execute();
    }

    @Override
    public boolean refreshOnFirstShow() {
        return false;
    }

    private String safe(String[] row, int index) {
        if (row == null || index < 0 || index >= row.length || row[index] == null || row[index].isBlank()) {
            return "-";
        }
        return row[index];
    }
}
