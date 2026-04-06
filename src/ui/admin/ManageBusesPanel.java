package ui.admin;

import config.UIConfig;
import dao.BusDAO;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ManageBusesPanel extends JPanel implements Refreshable {

    private JTable table;
    private DefaultTableModel model;

    private JTextField operatorField;
    private JTextField typeField;
    private JTextField seatsField;
    private JTextField fareField;
    private JTextField filterField;
    private JLabel loadingLabel;

    private JButton addBtn;
    private JButton updateBtn;
    private JButton statusBtn;
    private JButton deleteBtn;
    private JButton refreshBtn;

    private int selectedBusId = -1;

    public ManageBusesPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(tableCard(), BorderLayout.CENTER);
        add(formCard(), BorderLayout.EAST);

        loadBuses();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Manage Buses");
        title.setFont(UIConfig.FONT_TITLE);

        JLabel sub = new JLabel("Add, update and control fleet status");
        sub.setForeground(UIConfig.TEXT_LIGHT);

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        filterField = new JTextField();
        UIConfig.styleField(filterField);
        filterField.setPreferredSize(new Dimension(220, 36));

        JButton searchBtn = new JButton("Search");
        refreshBtn = new JButton("Refresh");
        UIConfig.primaryBtn(searchBtn);
        UIConfig.secondaryBtn(refreshBtn);

        searchBtn.addActionListener(e -> searchBuses());
        refreshBtn.addActionListener(e -> loadBuses());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(filterField);
        right.add(searchBtn);
        right.add(refreshBtn);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JPanel tableCard() {
        model = new DefaultTableModel(new Object[]{"ID", "Operator", "Type", "Seats", "Fare", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(model);
        UIConfig.styleTable(table);
        table.setRowHeight(32);
        table.getSelectionModel().addListSelectionListener(e -> fillForm());

        JScrollPane sp = new JScrollPane(table);
        UIConfig.styleScroll(sp);
        sp.setColumnHeaderView(table.getTableHeader());

        loadingLabel = new JLabel(" ");
        loadingLabel.setForeground(UIConfig.TEXT_LIGHT);

        JPanel card = new JPanel(new BorderLayout(0, 10));
        UIConfig.styleCard(card);
        card.add(sp, BorderLayout.CENTER);
        card.add(loadingLabel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel formCard() {
        JPanel card = new JPanel(new BorderLayout(12, 12));
        UIConfig.styleCard(card);
        card.setPreferredSize(new Dimension(340, 0));

        JLabel heading = new JLabel("Bus Details");
        heading.setFont(UIConfig.FONT_SUBTITLE);

        operatorField = field("Operator");
        typeField = field("Bus Type");
        seatsField = field("Seats");
        fareField = field("Fare");

        JPanel fields = new JPanel(new GridLayout(4, 1, 10, 10));
        fields.setOpaque(false);
        fields.add(operatorField);
        fields.add(typeField);
        fields.add(seatsField);
        fields.add(fareField);

        addBtn = btn("Add");
        updateBtn = btn("Update");
        statusBtn = btn("Toggle Status");
        deleteBtn = btn("Delete");

        UIConfig.primaryBtn(addBtn);
        UIConfig.successBtn(updateBtn);
        UIConfig.infoBtn(statusBtn);
        UIConfig.dangerBtn(deleteBtn);

        addBtn.addActionListener(e -> addBus());
        updateBtn.addActionListener(e -> updateBus());
        statusBtn.addActionListener(e -> toggleStatus());
        deleteBtn.addActionListener(e -> deleteBus());

        JPanel btns = new JPanel(new GridLayout(4, 1, 10, 10));
        btns.setOpaque(false);
        btns.add(addBtn);
        btns.add(updateBtn);
        btns.add(statusBtn);
        btns.add(deleteBtn);

        card.add(heading, BorderLayout.NORTH);
        card.add(fields, BorderLayout.CENTER);
        card.add(btns, BorderLayout.SOUTH);
        return card;
    }

    private JTextField field(String title) {
        JTextField f = new JTextField();
        UIConfig.styleField(f);
        f.setBorder(BorderFactory.createTitledBorder(title));
        return f;
    }

    private JButton btn(String text) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(140, 36));
        return b;
    }

    private void setBusy(boolean busy, String message) {
        loadingLabel.setText(message == null ? " " : message);
        table.setEnabled(!busy);
        if (refreshBtn != null) refreshBtn.setEnabled(!busy);
        addBtn.setEnabled(!busy);
        updateBtn.setEnabled(!busy);
        statusBtn.setEnabled(!busy);
        deleteBtn.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void loadBuses() {
        setBusy(true, "Loading buses...");
        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                return new BusDAO().getAllBuses();
            }

            @Override
            protected void done() {
                try {
                    model.setRowCount(0);
                    for (String[] row : get()) {
                        model.addRow(row);
                    }
                    loadingLabel.setText("Loaded " + model.getRowCount() + " buses");
                } catch (Exception ex) {
                    loadingLabel.setText("Failed to load buses");
                    JOptionPane.showMessageDialog(ManageBusesPanel.this, "Failed to load buses");
                } finally {
                    setBusy(false, loadingLabel.getText());
                }
            }
        };
        worker.execute();
    }

    private void searchBuses() {
        String q = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            loadBuses();
            return;
        }
        setBusy(true, "Searching...");
        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                return new BusDAO().getAllBuses();
            }

            @Override
            protected void done() {
                try {
                    model.setRowCount(0);
                    for (String[] row : get()) {
                        String text = (row[1] + " " + row[2] + " " + row[5]).toLowerCase();
                        if (text.contains(q)) {
                            model.addRow(row);
                        }
                    }
                    loadingLabel.setText("Found " + model.getRowCount() + " buses");
                } catch (Exception ex) {
                    loadingLabel.setText("Search failed");
                } finally {
                    setBusy(false, loadingLabel.getText());
                }
            }
        };
        worker.execute();
    }

    private void fillForm() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        selectedBusId = Integer.parseInt(model.getValueAt(row, 0).toString());
        operatorField.setText(model.getValueAt(row, 1).toString());
        typeField.setText(model.getValueAt(row, 2).toString());
        seatsField.setText(model.getValueAt(row, 3).toString());
        fareField.setText(model.getValueAt(row, 4).toString());
    }

    private void addBus() {
        try {
            String op = operatorField.getText().trim();
            String type = typeField.getText().trim();
            int seats = Integer.parseInt(seatsField.getText().trim());
            double fare = Double.parseDouble(fareField.getText().trim());

            if (op.isEmpty() || type.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill all fields");
                return;
            }

            setBusy(true, "Adding bus...");
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return new BusDAO().addBus(op, type, seats, fare);
                }

                @Override
                protected void done() {
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            clear();
                            loadBuses();
                            JOptionPane.showMessageDialog(ManageBusesPanel.this, "Bus added");
                        } else {
                            setBusy(false, "Add failed");
                            JOptionPane.showMessageDialog(ManageBusesPanel.this, "Add failed");
                        }
                    } catch (Exception ex) {
                        setBusy(false, "Add failed");
                        JOptionPane.showMessageDialog(ManageBusesPanel.this, "Add failed");
                    }
                }
            };
            worker.execute();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid input");
        }
    }

    private void updateBus() {
        if (selectedBusId == -1) {
            JOptionPane.showMessageDialog(this, "Select bus");
            return;
        }
        try {
            String op = operatorField.getText().trim();
            String type = typeField.getText().trim();
            int seats = Integer.parseInt(seatsField.getText().trim());
            double fare = Double.parseDouble(fareField.getText().trim());

            setBusy(true, "Updating bus...");
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return new BusDAO().updateBus(selectedBusId, op, type, seats, fare);
                }

                @Override
                protected void done() {
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            loadBuses();
                            JOptionPane.showMessageDialog(ManageBusesPanel.this, "Bus updated");
                        } else {
                            setBusy(false, "Update failed");
                            JOptionPane.showMessageDialog(ManageBusesPanel.this, "Update failed");
                        }
                    } catch (Exception ex) {
                        setBusy(false, "Update failed");
                        JOptionPane.showMessageDialog(ManageBusesPanel.this, "Update failed");
                    }
                }
            };
            worker.execute();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid input");
        }
    }

    private void toggleStatus() {
        if (selectedBusId == -1) {
            JOptionPane.showMessageDialog(this, "Select bus");
            return;
        }
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select bus");
            return;
        }

        String status = model.getValueAt(row, 5).toString();
        String newStatus = "ACTIVE".equalsIgnoreCase(status) ? "INACTIVE" : "ACTIVE";

        setBusy(true, "Updating status...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return new BusDAO().setBusStatus(selectedBusId, newStatus);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        loadBuses();
                    } else {
                        setBusy(false, "Status update failed");
                        JOptionPane.showMessageDialog(ManageBusesPanel.this, "Status update failed");
                    }
                } catch (Exception ex) {
                    setBusy(false, "Status update failed");
                    JOptionPane.showMessageDialog(ManageBusesPanel.this, "Status update failed");
                }
            }
        };
        worker.execute();
    }

    private void deleteBus() {
        if (selectedBusId == -1) {
            JOptionPane.showMessageDialog(this, "Select bus");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete bus?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        setBusy(true, "Deleting bus...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return new BusDAO().deleteBus(selectedBusId);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        clear();
                        loadBuses();
                    } else {
                        setBusy(false, "Delete failed");
                        JOptionPane.showMessageDialog(ManageBusesPanel.this, "Delete failed");
                    }
                } catch (Exception ex) {
                    setBusy(false, "Delete failed");
                    JOptionPane.showMessageDialog(ManageBusesPanel.this, "Delete failed");
                }
            }
        };
        worker.execute();
    }

    private void clear() {
        selectedBusId = -1;
        operatorField.setText("");
        typeField.setText("");
        seatsField.setText("");
        fareField.setText("");
    }

    @Override
    public void refreshData() {
        loadBuses();
    }

    @Override
    public boolean refreshOnFirstShow() {
        return false;
    }
}
