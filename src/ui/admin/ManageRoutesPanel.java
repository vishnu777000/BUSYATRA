package ui.admin;

import config.UIConfig;
import dao.RouteDAO;
import util.GeneratedRouteMapUtil;
import util.Refreshable;
import util.RouteMapImageUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.List;

public class ManageRoutesPanel extends JPanel implements Refreshable {

    private JTable table;
    private DefaultTableModel model;

    private JTextField routeNameField;
    private JTextField distanceField;
    private JTextField rateField;
    private JTextField filterField;
    private JLabel mapPreview;
    private JLabel loadingLabel;

    private JButton addBtn;
    private JButton updateBtn;
    private JButton mapBtn;
    private JButton routeLineBtn;
    private JButton deleteBtn;
    private JButton refreshBtn;

    private int selectedRouteId = -1;
    private int previewRequestVersion = 0;
    private final RouteDAO dao = new RouteDAO();

    public ManageRoutesPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);

        loadRoutes();
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel title = new JLabel("Route Management");
        title.setFont(UIConfig.FONT_TITLE);

        JLabel sub = new JLabel("Manage routes, fares and map image");
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

        searchBtn.addActionListener(e -> searchRoutes());
        refreshBtn.addActionListener(e -> loadRoutes());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(filterField);
        right.add(searchBtn);
        right.add(refreshBtn);

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel center() {
        JPanel p = new JPanel(new GridLayout(1, 2, 20, 20));
        p.setOpaque(false);
        p.add(tableCard());
        p.add(formCard());
        return p;
    }

    private JPanel tableCard() {
        model = new DefaultTableModel(new Object[]{"ID", "Route", "KM", "Rate", "Map"}, 0) {
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

        JLabel h = new JLabel("Route Details");
        h.setFont(UIConfig.FONT_SUBTITLE);

        routeNameField = field("Route Name");
        distanceField = field("Distance KM");
        rateField = field("Rate / KM");

        mapPreview = new JLabel("No Map", SwingConstants.CENTER);
        mapPreview.setForeground(UIConfig.TEXT_LIGHT);
        mapPreview.setPreferredSize(new Dimension(220, 130));
        mapPreview.setBorder(BorderFactory.createLineBorder(UIConfig.BORDER));

        JPanel fields = new JPanel(new GridLayout(4, 1, 10, 10));
        fields.setOpaque(false);
        fields.add(routeNameField);
        fields.add(distanceField);
        fields.add(rateField);
        fields.add(mapPreview);

        addBtn = btn("Add");
        updateBtn = btn("Update");
        mapBtn = btn("Upload Map");
        routeLineBtn = btn("Route Line");
        deleteBtn = btn("Delete");

        UIConfig.primaryBtn(addBtn);
        UIConfig.successBtn(updateBtn);
        UIConfig.infoBtn(mapBtn);
        UIConfig.dangerBtn(deleteBtn);

        addBtn.addActionListener(e -> addRoute());
        updateBtn.addActionListener(e -> updateRoute());
        mapBtn.addActionListener(e -> uploadMap());
        routeLineBtn.addActionListener(e -> openRouteLineDialog());
        deleteBtn.addActionListener(e -> deleteRoute());

        JPanel btns = new JPanel(new GridLayout(5, 1, 10, 10));
        btns.setOpaque(false);
        btns.add(addBtn);
        btns.add(updateBtn);
        btns.add(mapBtn);
        btns.add(routeLineBtn);
        btns.add(deleteBtn);

        card.add(h, BorderLayout.NORTH);
        card.add(fields, BorderLayout.CENTER);
        card.add(btns, BorderLayout.SOUTH);
        return card;
    }

    private JTextField field(String t) {
        JTextField f = new JTextField();
        UIConfig.styleField(f);
        f.setBorder(BorderFactory.createTitledBorder(t));
        return f;
    }

    private JButton btn(String t) {
        JButton b = new JButton(t);
        b.setPreferredSize(new Dimension(140, 36));
        return b;
    }

    private void setBusy(boolean busy, String message) {
        loadingLabel.setText(message == null ? " " : message);
        table.setEnabled(!busy);
        if (refreshBtn != null) refreshBtn.setEnabled(!busy);
        addBtn.setEnabled(!busy);
        updateBtn.setEnabled(!busy);
        mapBtn.setEnabled(!busy);
        routeLineBtn.setEnabled(!busy);
        deleteBtn.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void loadRoutes() {
        setBusy(true, "Loading routes...");
        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            private String errorMessage;

            @Override
            protected List<String[]> doInBackground() {
                List<String[]> routes = dao.getAllRoutes();
                errorMessage = dao.getLastError();
                return routes;
            }

            @Override
            protected void done() {
                try {
                    List<String[]> routes = get();
                    model.setRowCount(0);
                    for (String[] r : routes) {
                        model.addRow(r);
                    }
                    if ((routes == null || routes.isEmpty()) && dao.hasLastError()) {
                        loadingLabel.setText(errorMessage);
                    } else {
                        loadingLabel.setText("Loaded " + model.getRowCount() + " routes");
                    }
                } catch (Exception ex) {
                    String error = (errorMessage == null || errorMessage.isBlank()) ? "Failed to load routes" : errorMessage;
                    loadingLabel.setText(error);
                    JOptionPane.showMessageDialog(ManageRoutesPanel.this, error);
                } finally {
                    setBusy(false, loadingLabel.getText());
                }
            }
        };
        worker.execute();
    }

    private void searchRoutes() {
        String q = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            loadRoutes();
            return;
        }
        setBusy(true, "Searching...");
        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            private String errorMessage;

            @Override
            protected List<String[]> doInBackground() {
                List<String[]> routes = dao.getAllRoutes();
                errorMessage = dao.getLastError();
                return routes;
            }

            @Override
            protected void done() {
                try {
                    List<String[]> routes = get();
                    model.setRowCount(0);
                    for (String[] r : routes) {
                        String text = (r[1] + " " + r[2] + " " + r[3]).toLowerCase();
                        if (text.contains(q)) {
                            model.addRow(r);
                        }
                    }
                    if ((routes == null || routes.isEmpty()) && dao.hasLastError()) {
                        loadingLabel.setText(errorMessage);
                    } else {
                        loadingLabel.setText("Found " + model.getRowCount() + " routes");
                    }
                } catch (Exception ex) {
                    loadingLabel.setText((errorMessage == null || errorMessage.isBlank()) ? "Search failed" : errorMessage);
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

        selectedRouteId = Integer.parseInt(model.getValueAt(row, 0).toString());
        routeNameField.setText(model.getValueAt(row, 1).toString());
        distanceField.setText(model.getValueAt(row, 2).toString());
        rateField.setText(model.getValueAt(row, 3).toString());

        String map = model.getValueAt(row, 4) == null ? "" : model.getValueAt(row, 4).toString();
        mapPreview.setIcon(null);
        mapPreview.setText("No Map");

        final int requestVersion = ++previewRequestVersion;
        mapPreview.setText("Loading preview...");
        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() {
                ImageIcon icon = RouteMapImageUtil.resolve(map);
                if (icon != null) {
                    return RouteMapImageUtil.scaleToFit(icon, 220, 130);
                }
                return GeneratedRouteMapUtil.buildFromRows(dao.getStopsByRoute(selectedRouteId), "", "", 220, 130);
            }

            @Override
            protected void done() {
                if (requestVersion != previewRequestVersion) {
                    return;
                }
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        mapPreview.setIcon(icon);
                        mapPreview.setText("");
                    } else {
                        mapPreview.setIcon(null);
                        mapPreview.setText("Preview unavailable");
                    }
                } catch (Exception ex) {
                    mapPreview.setIcon(null);
                    mapPreview.setText("Preview unavailable");
                }
            }
        };
        worker.execute();
    }

    private void addRoute() {
        try {
            String name = routeNameField.getText().trim();
            int km = Integer.parseInt(distanceField.getText().trim());
            double rate = Double.parseDouble(rateField.getText().trim());
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Route name required");
                return;
            }

            setBusy(true, "Adding route...");
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                private String errorMessage;

                @Override
                protected Boolean doInBackground() {
                    boolean ok = dao.addRouteMaster(name, km, rate);
                    errorMessage = dao.getLastError();
                    return ok;
                }

                @Override
                protected void done() {
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            clear();
                            loadRoutes();
                            JOptionPane.showMessageDialog(ManageRoutesPanel.this, "Route added");
                        } else {
                            String error = (errorMessage == null || errorMessage.isBlank()) ? "Add failed" : errorMessage;
                            setBusy(false, error);
                            JOptionPane.showMessageDialog(ManageRoutesPanel.this, error);
                        }
                    } catch (Exception ex) {
                        String error = (errorMessage == null || errorMessage.isBlank()) ? "Add failed" : errorMessage;
                        setBusy(false, error);
                        JOptionPane.showMessageDialog(ManageRoutesPanel.this, error);
                    }
                }
            };
            worker.execute();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid input");
        }
    }

    private void updateRoute() {
        if (selectedRouteId == -1) {
            JOptionPane.showMessageDialog(this, "Select route");
            return;
        }
        try {
            String name = routeNameField.getText().trim();
            int km = Integer.parseInt(distanceField.getText().trim());
            double rate = Double.parseDouble(rateField.getText().trim());

            setBusy(true, "Updating route...");
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                private String errorMessage;

                @Override
                protected Boolean doInBackground() {
                    boolean ok = dao.updateRoute(selectedRouteId, name, km, rate);
                    errorMessage = dao.getLastError();
                    return ok;
                }

                @Override
                protected void done() {
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            loadRoutes();
                        } else {
                            String error = (errorMessage == null || errorMessage.isBlank()) ? "Update failed" : errorMessage;
                            setBusy(false, error);
                            JOptionPane.showMessageDialog(ManageRoutesPanel.this, error);
                        }
                    } catch (Exception ex) {
                        String error = (errorMessage == null || errorMessage.isBlank()) ? "Update failed" : errorMessage;
                        setBusy(false, error);
                        JOptionPane.showMessageDialog(ManageRoutesPanel.this, error);
                    }
                }
            };
            worker.execute();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid input");
        }
    }

    private void uploadMap() {
        if (selectedRouteId == -1) {
            JOptionPane.showMessageDialog(this, "Select route");
            return;
        }

        JFileChooser ch = new JFileChooser();
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = ch.getSelectedFile();
        String path = f.getAbsolutePath().replace("\\", "/");

        setBusy(true, "Uploading map...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                boolean ok = dao.updateRouteMap(selectedRouteId, path);
                errorMessage = dao.getLastError();
                return ok;
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        loadRoutes();
                    } else {
                        String error = errorMessage;
                        setBusy(false, error);
                        JOptionPane.showMessageDialog(ManageRoutesPanel.this, error);
                    }
                } catch (ExecutionException ex) {
                    String error = errorMessage;
                    Throwable cause = ex.getCause();
                    if ((error == null || error.isBlank() || "Route operation failed.".equals(error)) && cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
                        error = cause.getMessage().trim();
                    }
                    if (error == null || error.isBlank()) {
                        error = "Route operation failed.";
                    }
                    setBusy(false, error);
                    JOptionPane.showMessageDialog(ManageRoutesPanel.this, error);
                } catch (Exception ex) {
                    String error = errorMessage;
                    if (error == null || error.isBlank()) {
                        error = "Route operation failed.";
                    }
                    setBusy(false, error);
                    JOptionPane.showMessageDialog(ManageRoutesPanel.this, error);
                }
            }
        };
        worker.execute();
    }

    private void deleteRoute() {
        if (selectedRouteId == -1) {
            JOptionPane.showMessageDialog(this, "Select route");
            return;
        }
        int c = JOptionPane.showConfirmDialog(this, "Delete selected route?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        setBusy(true, "Deleting route...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                boolean ok = dao.deleteRoute(selectedRouteId);
                errorMessage = dao.getLastError();
                return ok;
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        clear();
                        loadRoutes();
                    } else {
                        String error = (errorMessage == null || errorMessage.isBlank()) ? "Delete failed" : errorMessage;
                        setBusy(false, error);
                        JOptionPane.showMessageDialog(ManageRoutesPanel.this, error);
                    }
                } catch (Exception ex) {
                    String error = (errorMessage == null || errorMessage.isBlank()) ? "Delete failed" : errorMessage;
                    setBusy(false, error);
                    JOptionPane.showMessageDialog(ManageRoutesPanel.this, error);
                }
            }
        };
        worker.execute();
    }

    private void openRouteLineDialog() {
        if (selectedRouteId == -1) {
            JOptionPane.showMessageDialog(this, "Select route");
            return;
        }

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Route Line Management",
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setSize(560, 460);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(12, 12));

        DefaultTableModel stopsModel = new DefaultTableModel(
                new Object[]{"Stop", "Order", "Distance KM"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable stopsTable = new JTable(stopsModel);
        UIConfig.styleTable(stopsTable);
        JScrollPane stopScroll = new JScrollPane(stopsTable);
        UIConfig.styleScroll(stopScroll);
        stopScroll.setColumnHeaderView(stopsTable.getTableHeader());
        JLabel stopStatus = new JLabel(" ");
        stopStatus.setForeground(UIConfig.TEXT_LIGHT);

        JPanel addStopCard = new JPanel(new GridLayout(2, 4, 8, 8));
        UIConfig.styleCard(addStopCard);

        JTextField stopName = new JTextField();
        JTextField stopOrder = new JTextField();
        JTextField stopDistance = new JTextField();
        JButton addStopBtn = new JButton("Add Stop");
        UIConfig.primaryBtn(addStopBtn);

        stopName.setBorder(BorderFactory.createTitledBorder("Stop Name"));
        stopOrder.setBorder(BorderFactory.createTitledBorder("Order"));
        stopDistance.setBorder(BorderFactory.createTitledBorder("Distance"));

        addStopCard.add(stopName);
        addStopCard.add(stopOrder);
        addStopCard.add(stopDistance);
        addStopCard.add(addStopBtn);

        JLabel hint = new JLabel("Tip: Keep order and distance increasing for ETA accuracy.");
        hint.setFont(UIConfig.FONT_SMALL);
        hint.setForeground(UIConfig.TEXT_LIGHT);
        addStopCard.add(hint);

        addStopBtn.addActionListener(e -> {
            try {
                String stop = stopName.getText().trim();
                int order = Integer.parseInt(stopOrder.getText().trim());
                int distance = Integer.parseInt(stopDistance.getText().trim());

                if (stop.isBlank()) {
                    JOptionPane.showMessageDialog(dialog, "Enter stop name");
                    return;
                }
                if (order <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Order must be greater than zero");
                    return;
                }
                if (distance < 0) {
                    JOptionPane.showMessageDialog(dialog, "Distance must be zero or greater");
                    return;
                }

                addStopBtn.setEnabled(false);
                stopStatus.setText("Adding stop...");
                SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                    private String errorMessage;

                    @Override
                    protected Boolean doInBackground() {
                        RouteDAO routeDao = new RouteDAO();
                        boolean ok = routeDao.addStop(selectedRouteId, stop, order, distance);
                        errorMessage = routeDao.getLastError();
                        return ok;
                    }

                    @Override
                    protected void done() {
                        addStopBtn.setEnabled(true);
                        try {
                            if (Boolean.TRUE.equals(get())) {
                                stopName.setText("");
                                stopOrder.setText("");
                                stopDistance.setText("");
                                loadRouteStops(stopsModel, selectedRouteId, stopStatus, addStopBtn);
                            } else {
                                stopStatus.setText(errorMessage == null || errorMessage.isBlank() ? "Add stop failed" : errorMessage);
                                JOptionPane.showMessageDialog(dialog, stopStatus.getText());
                            }
                        } catch (Exception ex) {
                            stopStatus.setText(errorMessage == null || errorMessage.isBlank() ? "Add stop failed" : errorMessage);
                            JOptionPane.showMessageDialog(dialog, stopStatus.getText());
                        }
                    }
                };
                worker.execute();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Enter valid stop/order/distance");
            }
        });

        dialog.add(stopStatus, BorderLayout.NORTH);
        dialog.add(stopScroll, BorderLayout.CENTER);
        dialog.add(addStopCard, BorderLayout.SOUTH);
        loadRouteStops(stopsModel, selectedRouteId, stopStatus, addStopBtn);
        dialog.setVisible(true);
    }

    private void loadRouteStops(DefaultTableModel stopsModel, int routeId, JLabel stopStatus, JButton addStopBtn) {
        stopsModel.setRowCount(0);
        stopsModel.addRow(new Object[]{"Loading...", "-", "-"});
        stopStatus.setText("Loading stops...");
        addStopBtn.setEnabled(false);

        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            private String errorMessage;

            @Override
            protected List<String[]> doInBackground() {
                RouteDAO routeDao = new RouteDAO();
                List<String[]> stops = routeDao.getStopsByRoute(routeId);
                errorMessage = routeDao.getLastError();
                return stops;
            }

            @Override
            protected void done() {
                addStopBtn.setEnabled(true);
                try {
                    List<String[]> stops = get();
                    stopsModel.setRowCount(0);
                    for (String[] s : stops) {
                        if (s == null || s.length < 3) continue;
                        stopsModel.addRow(new Object[]{s[0], s[1], s[2]});
                    }
                    if (stopsModel.getRowCount() == 0) {
                        String message = (errorMessage == null || errorMessage.isBlank()) ? "No stops configured" : errorMessage;
                        stopsModel.addRow(new Object[]{message, "-", "-"});
                        stopStatus.setText(message);
                    } else {
                        stopStatus.setText("Loaded " + stopsModel.getRowCount() + " stops");
                    }
                } catch (Exception ex) {
                    stopsModel.setRowCount(0);
                    String message = (errorMessage == null || errorMessage.isBlank()) ? "Failed to load stops" : errorMessage;
                    stopsModel.addRow(new Object[]{message, "-", "-"});
                    stopStatus.setText(message);
                }
            }
        };
        worker.execute();
    }

    private void clear() {
        selectedRouteId = -1;
        routeNameField.setText("");
        distanceField.setText("");
        rateField.setText("");
        mapPreview.setIcon(null);
        mapPreview.setText("No Map");
    }

    @Override
    public void refreshData() {
        loadRoutes();
    }

    @Override
    public boolean refreshOnFirstShow() {
        return false;
    }
}
