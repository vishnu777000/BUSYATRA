package ui.admin;

import com.toedter.calendar.JDateChooser;
import config.UIConfig;
import dao.BusDAO;
import dao.RouteDAO;
import dao.ScheduleDAO;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class ManageSchedulesPanel extends JPanel implements Refreshable {

    private JTable table;
    private DefaultTableModel model;

    private JComboBox<Item> busBox;
    private JComboBox<Item> routeBox;
    private JDateChooser departDateChooser;
    private JDateChooser arriveDateChooser;
    private JSpinner departTimeSpinner;
    private JSpinner arriveTimeSpinner;
    private JTextField searchField;
    private JLabel loadingLabel;

    private JButton searchBtn;
    private JButton addBtn;
    private JButton updateBtn;
    private JButton deleteBtn;

    private int selectedId = -1;

    public ManageSchedulesPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(tableCard(), BorderLayout.CENTER);
        add(formCard(), BorderLayout.EAST);

        refreshData();
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel title = new JLabel("Schedule Management");
        title.setFont(UIConfig.FONT_TITLE);

        searchField = new JTextField();
        UIConfig.styleField(searchField);
        searchField.setPreferredSize(new Dimension(220, 36));

        searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Refresh");
        UIConfig.primaryBtn(searchBtn);
        UIConfig.secondaryBtn(refreshBtn);

        searchBtn.addActionListener(e -> search());
        refreshBtn.addActionListener(e -> refreshData());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(searchField);
        right.add(searchBtn);
        right.add(refreshBtn);

        p.add(title, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel tableCard() {
        model = new DefaultTableModel(new Object[]{"ID", "Bus", "Route", "Departure", "Arrival"}, 0) {
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
        card.setPreferredSize(new Dimension(420, 0));

        JLabel h = new JLabel("Schedule Details");
        h.setFont(UIConfig.FONT_SUBTITLE);

        busBox = new JComboBox<>();
        routeBox = new JComboBox<>();
        UIConfig.styleCombo(busBox);
        UIConfig.styleCombo(routeBox);

        departDateChooser = dateChooser();
        arriveDateChooser = dateChooser();
        departTimeSpinner = timeSpinner();
        arriveTimeSpinner = timeSpinner();

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        form.add(label("Bus"));
        form.add(busBox);
        form.add(Box.createVerticalStrut(8));

        form.add(label("Route"));
        form.add(routeBox);
        form.add(Box.createVerticalStrut(8));

        form.add(label("Departure Date"));
        form.add(departDateChooser);
        form.add(Box.createVerticalStrut(8));

        form.add(label("Departure Time"));
        form.add(departTimeSpinner);
        form.add(Box.createVerticalStrut(8));

        form.add(label("Arrival Date"));
        form.add(arriveDateChooser);
        form.add(Box.createVerticalStrut(8));

        form.add(label("Arrival Time"));
        form.add(arriveTimeSpinner);

        addBtn = btn("Add");
        updateBtn = btn("Update");
        deleteBtn = btn("Delete");

        UIConfig.primaryBtn(addBtn);
        UIConfig.successBtn(updateBtn);
        UIConfig.dangerBtn(deleteBtn);

        addBtn.addActionListener(e -> add());
        updateBtn.addActionListener(e -> update());
        deleteBtn.addActionListener(e -> delete());

        JPanel btns = new JPanel(new GridLayout(3, 1, 10, 10));
        btns.setOpaque(false);
        btns.add(addBtn);
        btns.add(updateBtn);
        btns.add(deleteBtn);

        card.add(h, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(btns, BorderLayout.SOUTH);
        return card;
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(UIConfig.FONT_SMALL);
        l.setForeground(UIConfig.TEXT_LIGHT);
        return l;
    }

    private JButton btn(String t) {
        JButton b = new JButton(t);
        b.setPreferredSize(new Dimension(140, 36));
        return b;
    }

    private JDateChooser dateChooser() {
        JDateChooser dc = new JDateChooser();
        dc.setDate(new Date());
        return dc;
    }

    private JSpinner timeSpinner() {
        JSpinner sp = new JSpinner(new SpinnerDateModel());
        sp.setEditor(new JSpinner.DateEditor(sp, "HH:mm:ss"));
        return sp;
    }

    private void setBusy(boolean busy, String message) {
        loadingLabel.setText(message == null ? " " : message);
        table.setEnabled(!busy);
        searchBtn.setEnabled(!busy);
        addBtn.setEnabled(!busy);
        updateBtn.setEnabled(!busy);
        deleteBtn.setEnabled(!busy);
        busBox.setEnabled(!busy);
        routeBox.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void loadAllDataAsync(boolean keepSearchFilter) {
        String searchText = keepSearchFilter ? searchField.getText().trim() : "";
        setBusy(true, "Loading schedules...");

        SwingWorker<ScheduleData, Void> worker = new SwingWorker<>() {
            @Override
            protected ScheduleData doInBackground() {
                ScheduleData data = new ScheduleData();
                data.buses = new BusDAO().getAllBuses();
                data.routes = new RouteDAO().getAllRoutes();
                data.schedules = searchText.isBlank()
                        ? new ScheduleDAO().getAllSchedules()
                        : new ScheduleDAO().searchSchedule(searchText);
                return data;
            }

            @Override
            protected void done() {
                try {
                    ScheduleData data = get();
                    fillBusBox(data.buses);
                    fillRouteBox(data.routes);
                    fillTable(data.schedules);
                    loadingLabel.setText("Loaded " + model.getRowCount() + " schedules");
                } catch (Exception ex) {
                    loadingLabel.setText("Failed to load schedules");
                    JOptionPane.showMessageDialog(ManageSchedulesPanel.this, "Failed to load schedules");
                } finally {
                    setBusy(false, loadingLabel.getText());
                }
            }
        };
        worker.execute();
    }

    private void fillTable(List<String[]> list) {
        model.setRowCount(0);
        for (String[] row : list) {
            model.addRow(row);
        }
    }

    private void fillBusBox(List<String[]> list) {
        int selectedId = selectedItemId(busBox);
        busBox.removeAllItems();
        for (String[] b : list) {
            busBox.addItem(new Item(Integer.parseInt(b[0]), b[1] + " - " + b[2]));
        }
        selectById(busBox, selectedId);
        if (busBox.getSelectedItem() == null && busBox.getItemCount() > 0) {
            busBox.setSelectedIndex(0);
        }
    }

    private void fillRouteBox(List<String[]> list) {
        int selectedId = selectedItemId(routeBox);
        routeBox.removeAllItems();
        for (String[] r : list) {
            routeBox.addItem(new Item(Integer.parseInt(r[0]), r[1]));
        }
        selectById(routeBox, selectedId);
        if (routeBox.getSelectedItem() == null && routeBox.getItemCount() > 0) {
            routeBox.setSelectedIndex(0);
        }
    }

    private void search() {
        loadAllDataAsync(true);
    }

    private void fillForm() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        selectedId = Integer.parseInt(model.getValueAt(row, 0).toString());
    }

    private void add() {
        try {
            Item bus = (Item) busBox.getSelectedItem();
            Item route = (Item) routeBox.getSelectedItem();
            if (bus == null || route == null) {
                JOptionPane.showMessageDialog(this, "Bus and route are required");
                return;
            }

            Timestamp dep = getTimestamp(departDateChooser, departTimeSpinner);
            Timestamp arr = getTimestamp(arriveDateChooser, arriveTimeSpinner);
            if (arr.before(dep)) {
                JOptionPane.showMessageDialog(this, "Arrival must be after departure");
                return;
            }

            setBusy(true, "Creating schedule...");
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return new ScheduleDAO().addSchedule(bus.id, route.id, dep, arr);
                }

                @Override
                protected void done() {
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            loadAllDataAsync(false);
                        } else {
                            setBusy(false, "Create failed");
                            JOptionPane.showMessageDialog(ManageSchedulesPanel.this, "Create failed");
                        }
                    } catch (Exception ex) {
                        setBusy(false, "Create failed");
                        JOptionPane.showMessageDialog(ManageSchedulesPanel.this, "Create failed");
                    }
                }
            };
            worker.execute();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid schedule data");
        }
    }

    private void update() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Select row");
            return;
        }
        try {
            Item bus = (Item) busBox.getSelectedItem();
            Item route = (Item) routeBox.getSelectedItem();
            if (bus == null || route == null) {
                JOptionPane.showMessageDialog(this, "Bus and route are required");
                return;
            }

            Timestamp dep = getTimestamp(departDateChooser, departTimeSpinner);
            Timestamp arr = getTimestamp(arriveDateChooser, arriveTimeSpinner);
            if (arr.before(dep)) {
                JOptionPane.showMessageDialog(this, "Arrival must be after departure");
                return;
            }

            setBusy(true, "Updating schedule...");
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return new ScheduleDAO().updateSchedule(selectedId, bus.id, route.id, dep, arr);
                }

                @Override
                protected void done() {
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            loadAllDataAsync(false);
                        } else {
                            setBusy(false, "Update failed");
                            JOptionPane.showMessageDialog(ManageSchedulesPanel.this, "Update failed");
                        }
                    } catch (Exception ex) {
                        setBusy(false, "Update failed");
                        JOptionPane.showMessageDialog(ManageSchedulesPanel.this, "Update failed");
                    }
                }
            };
            worker.execute();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid schedule data");
        }
    }

    private void delete() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Select row");
            return;
        }

        int c = JOptionPane.showConfirmDialog(this, "Delete schedule?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        setBusy(true, "Deleting schedule...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return new ScheduleDAO().deleteSchedule(selectedId);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        loadAllDataAsync(false);
                    } else {
                        setBusy(false, "Delete failed");
                        JOptionPane.showMessageDialog(ManageSchedulesPanel.this, "Delete failed");
                    }
                } catch (Exception ex) {
                    setBusy(false, "Delete failed");
                    JOptionPane.showMessageDialog(ManageSchedulesPanel.this, "Delete failed");
                }
            }
        };
        worker.execute();
    }

    private Timestamp getTimestamp(JDateChooser dc, JSpinner sp) {
        Date d = dc.getDate();
        Date t = (Date) sp.getValue();
        long millis = d.getTime() + (t.getTime() % (24L * 60L * 60L * 1000L));
        return new Timestamp(millis);
    }

    private int selectedItemId(JComboBox<Item> box) {
        Object selected = box.getSelectedItem();
        return selected instanceof Item ? ((Item) selected).id : -1;
    }

    private void selectById(JComboBox<Item> box, int id) {
        if (id < 0) return;
        for (int i = 0; i < box.getItemCount(); i++) {
            Item item = box.getItemAt(i);
            if (item.id == id) {
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    @Override
    public void refreshData() {
        loadAllDataAsync(false);
    }

    private static class Item {
        int id;
        String label;

        Item(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Item)) return false;
            Item other = (Item) obj;
            return id == other.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    private static class ScheduleData {
        List<String[]> buses;
        List<String[]> routes;
        List<String[]> schedules;
    }
}
