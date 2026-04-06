package ui.admin;

import com.toedter.calendar.JDateChooser;
import config.UIConfig;
import dao.BusDAO;
import dao.RouteDAO;
import dao.ScheduleDAO;
import util.DBConnectionUtil;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
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
    private JButton cancelBtn;
    private JButton deleteBtn;

    private int selectedId = -1;
    private String selectedStatus = "ACTIVE";
    private boolean busyState = false;

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
        model = new DefaultTableModel(new Object[]{"ID", "Bus", "Route", "Departure", "Arrival", "Status"}, 0) {
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
        cancelBtn = btn("Cancel");
        deleteBtn = btn("Delete");

        UIConfig.primaryBtn(addBtn);
        UIConfig.successBtn(updateBtn);
        UIConfig.secondaryBtn(cancelBtn);
        UIConfig.dangerBtn(deleteBtn);

        addBtn.addActionListener(e -> add());
        updateBtn.addActionListener(e -> update());
        cancelBtn.addActionListener(e -> toggleCancel());
        deleteBtn.addActionListener(e -> delete());

        JPanel btns = new JPanel(new GridLayout(4, 1, 10, 10));
        btns.setOpaque(false);
        btns.add(addBtn);
        btns.add(updateBtn);
        btns.add(cancelBtn);
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
        dc.setDate(todayStart());
        dc.setMinSelectableDate(todayStart());
        return dc;
    }

    private JSpinner timeSpinner() {
        JSpinner sp = new JSpinner(new SpinnerDateModel());
        sp.setEditor(new JSpinner.DateEditor(sp, "HH:mm:ss"));
        return sp;
    }

    private void setBusy(boolean busy, String message) {
        busyState = busy;
        loadingLabel.setText(message == null ? " " : message);
        table.setEnabled(!busy);
        searchBtn.setEnabled(!busy);
        addBtn.setEnabled(!busy);
        busBox.setEnabled(!busy);
        routeBox.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        refreshActionButtons();
    }

    private void loadAllDataAsync(boolean keepSearchFilter) {
        String searchText = keepSearchFilter ? searchField.getText().trim() : "";
        setBusy(true, "Loading schedules...");

        SwingWorker<ScheduleData, Void> worker = new SwingWorker<>() {
            @Override
            protected ScheduleData doInBackground() {
                try (Connection ignored = DBConnectionUtil.getConnection()) {
                    
                } catch (Exception e) {
                    throw new RuntimeException(DBConnectionUtil.userMessage(e), e);
                }

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
                    String message = DBConnectionUtil.userMessage(ex);
                    loadingLabel.setText(message);
                    if (!DBConnectionUtil.isConnectionUnavailable(ex)) {
                        JOptionPane.showMessageDialog(ManageSchedulesPanel.this, message);
                    }
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
        selectedId = -1;
        selectedStatus = "ACTIVE";
        refreshActionButtons();
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
        if (row == -1) {
            selectedId = -1;
            selectedStatus = "ACTIVE";
            refreshActionButtons();
            return;
        }
        selectedId = Integer.parseInt(model.getValueAt(row, 0).toString());
        selectedStatus = model.getColumnCount() > 5 && model.getValueAt(row, 5) != null
                ? model.getValueAt(row, 5).toString()
                : "ACTIVE";
        String[] details = new ScheduleDAO().getScheduleAdminEditData(selectedId);
        if (details != null && details.length >= 6) {
            trySelectById(busBox, parseInt(details[1]));
            trySelectById(routeBox, parseInt(details[2]));
            applyDateTime(departDateChooser, departTimeSpinner, details[3]);
            applyDateTime(arriveDateChooser, arriveTimeSpinner, details[4]);
            selectedStatus = details[5] == null || details[5].isBlank() ? selectedStatus : details[5];
        }
        refreshActionButtons();
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
            if (!arr.after(dep)) {
                JOptionPane.showMessageDialog(this, "Arrival must be after departure");
                return;
            }

            setBusy(true, "Creating schedule...");
            ScheduleDAO scheduleDAO = new ScheduleDAO();
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return scheduleDAO.addSchedule(bus.id, route.id, dep, arr);
                }

                @Override
                protected void done() {
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            loadAllDataAsync(false);
                        } else {
                            setBusy(false, "Create failed");
                            String message = scheduleDAO.getLastErrorMessage();
                            JOptionPane.showMessageDialog(ManageSchedulesPanel.this,
                                    message == null || message.isBlank() ? "Create failed" : message);
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
            if (!arr.after(dep)) {
                JOptionPane.showMessageDialog(this, "Arrival must be after departure");
                return;
            }

            setBusy(true, "Updating schedule...");
            ScheduleDAO scheduleDAO = new ScheduleDAO();
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return scheduleDAO.updateSchedule(selectedId, bus.id, route.id, dep, arr);
                }

                @Override
                protected void done() {
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            loadAllDataAsync(false);
                        } else {
                            setBusy(false, "Update failed");
                            String message = scheduleDAO.getLastErrorMessage();
                            JOptionPane.showMessageDialog(ManageSchedulesPanel.this,
                                    message == null || message.isBlank() ? "Update failed" : message);
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

    private void toggleCancel() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Select row");
            return;
        }

        boolean reactivate = "CANCELLED".equalsIgnoreCase(selectedStatus);
        String targetStatus = reactivate ? "ACTIVE" : "CANCELLED";
        String action = reactivate ? "reactivate" : "cancel";

        int c = JOptionPane.showConfirmDialog(
                this,
                "Do you want to " + action + " this schedule?",
                reactivate ? "Reactivate Schedule" : "Cancel Schedule",
                JOptionPane.YES_NO_OPTION
        );
        if (c != JOptionPane.YES_OPTION) {
            return;
        }

        setBusy(true, reactivate ? "Reactivating schedule..." : "Cancelling schedule...");
        ScheduleDAO scheduleDAO = new ScheduleDAO();
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return scheduleDAO.setScheduleStatus(selectedId, targetStatus);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        loadAllDataAsync(true);
                    } else {
                        setBusy(false, reactivate ? "Reactivate failed" : "Cancel failed");
                        String message = scheduleDAO.getLastErrorMessage();
                        JOptionPane.showMessageDialog(
                                ManageSchedulesPanel.this,
                                message == null || message.isBlank()
                                        ? (reactivate ? "Reactivate failed" : "Cancel failed")
                                        : message
                        );
                    }
                } catch (Exception ex) {
                    setBusy(false, reactivate ? "Reactivate failed" : "Cancel failed");
                    JOptionPane.showMessageDialog(
                            ManageSchedulesPanel.this,
                            reactivate ? "Reactivate failed" : "Cancel failed"
                    );
                }
            }
        };
        worker.execute();
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
        Date selectedDate = dc == null ? null : dc.getDate();
        Date selectedTime = sp == null ? null : (Date) sp.getValue();
        if (selectedDate == null || selectedTime == null) {
            throw new IllegalArgumentException("Date and time are required");
        }

        Calendar dateCalendar = Calendar.getInstance();
        dateCalendar.setTime(selectedDate);

        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTime(selectedTime);

        dateCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        dateCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
        dateCalendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND));
        dateCalendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(dateCalendar.getTimeInMillis());
    }

    private Date todayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private void refreshActionButtons() {
        boolean hasSelection = selectedId > 0;
        if (cancelBtn != null) {
            cancelBtn.setText("CANCELLED".equalsIgnoreCase(selectedStatus) ? "Reactivate" : "Cancel");
            cancelBtn.setEnabled(!busyState && hasSelection);
        }
        if (deleteBtn != null) {
            deleteBtn.setEnabled(!busyState && hasSelection);
        }
        if (updateBtn != null) {
            updateBtn.setEnabled(!busyState && hasSelection);
        }
        if (addBtn != null) {
            addBtn.setEnabled(!busyState);
        }
        if (searchBtn != null) {
            searchBtn.setEnabled(!busyState);
        }
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

    private void trySelectById(JComboBox<Item> box, int id) {
        if (box == null || id <= 0) return;
        selectById(box, id);
    }

    private int parseInt(String value) {
        try {
            return value == null ? -1 : Integer.parseInt(value.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private void applyDateTime(JDateChooser chooser, JSpinner spinner, String raw) {
        Date value = parseDateTime(raw);
        if (value == null) return;
        if (chooser != null) chooser.setDate(value);
        if (spinner != null) spinner.setValue(value);
    }

    private Date parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String value = raw.trim().replace('T', ' ');
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd HH:mm:ss.S"
        };

        for (String pattern : patterns) {
            try {
                LocalDateTime dt = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
                Timestamp ts = Timestamp.valueOf(dt);
                return new Date(ts.getTime());
            } catch (Exception ignored) {

            }
        }

        try {
            return Timestamp.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void refreshData() {
        loadAllDataAsync(false);
    }

    @Override
    public boolean refreshOnFirstShow() {
        return false;
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
