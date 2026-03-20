package ui.booking;

import com.toedter.calendar.JDateChooser;
import config.UIConfig;
import dao.CityDAO;
import dao.RouteDAO;
import dao.SearchDAO;
import ui.common.MainFrame;
import util.BookingContext;
import util.IconUtil;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SearchBusPanel extends JPanel {

    private final MainFrame frame;

    private JTextField sourceField;
    private JTextField destinationField;
    private JDateChooser dateChooser;
    private JButton searchBtn;
    private JLabel statusLabel;
    private JPanel resultsPanel;
    private JComboBox<String> busTypeFilter;
    private JComboBox<String> departureFilter;
    private JComboBox<String> sortFilter;

    private final CityDAO cityDAO = new CityDAO();
    private final SearchDAO searchDAO = new SearchDAO();
    private final Map<String, List<String>> cityCache = new HashMap<>();
    private final Map<String, Double> fareCache = new HashMap<>();

    private List<String[]> lastSearchRaw = new ArrayList<>();
    private String lastSource = "";
    private String lastDestination = "";
    private String lastDate = "";

    public SearchBusPanel(MainFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout(18, 18));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildTop(), BorderLayout.NORTH);
        add(buildResults(), BorderLayout.CENTER);
    }

    private JComponent buildTop() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel title = new JLabel("Search Buses");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JLabel sub = new JLabel("Fast route search with live fare and seat visibility");
        sub.setForeground(UIConfig.TEXT_LIGHT);
        sub.setFont(UIConfig.FONT_SMALL);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        titleRow.add(left, BorderLayout.WEST);

        JPanel card = new JPanel(new BorderLayout(10, 10));
        UIConfig.styleCard(card);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        sourceField = createInput("Source City");
        destinationField = createInput("Destination City");
        addAutoSuggest(sourceField);
        addAutoSuggest(destinationField);

        JButton swapBtn = new JButton(IconUtil.loadTinted("swap.png", 18, 18, Color.WHITE));
        UIConfig.primaryBtn(swapBtn);
        swapBtn.setPreferredSize(new Dimension(46, 40));
        swapBtn.setBackground(new Color(220, 53, 69));
        swapBtn.setToolTipText("Swap source and destination");
        swapBtn.addActionListener(e -> {
            String temp = sourceField.getText();
            sourceField.setText(destinationField.getText());
            destinationField.setText(temp);
        });

        dateChooser = new JDateChooser();
        dateChooser.setDate(new Date());
        dateChooser.setDateFormatString("yyyy-MM-dd");
        dateChooser.setPreferredSize(new Dimension(160, 40));

        searchBtn = new JButton("Search Buses");
        UIConfig.primaryBtn(searchBtn);
        searchBtn.setPreferredSize(new Dimension(150, 40));
        searchBtn.addActionListener(e -> search());

        gbc.gridx = 0;
        form.add(sourceField, gbc);
        gbc.gridx = 1;
        form.add(swapBtn, gbc);
        gbc.gridx = 2;
        form.add(destinationField, gbc);
        gbc.gridx = 3;
        gbc.weightx = 0;
        form.add(dateChooser, gbc);
        gbc.gridx = 4;
        form.add(searchBtn, gbc);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setOpaque(false);

        busTypeFilter = new JComboBox<>(new String[]{"All Bus Types", "AC", "NON-AC", "SLEEPER", "SEATER"});
        departureFilter = new JComboBox<>(new String[]{"Any Time", "Morning (05-12)", "Afternoon (12-17)", "Evening (17-22)", "Night (22-05)"});
        sortFilter = new JComboBox<>(new String[]{"Sort: Departure", "Sort: Fare Low->High", "Sort: Fare High->Low", "Sort: Seats High->Low"});

        UIConfig.styleCombo(busTypeFilter);
        UIConfig.styleCombo(departureFilter);
        UIConfig.styleCombo(sortFilter);
        busTypeFilter.setPreferredSize(new Dimension(150, 34));
        departureFilter.setPreferredSize(new Dimension(170, 34));
        sortFilter.setPreferredSize(new Dimension(180, 34));

        filters.add(busTypeFilter);
        filters.add(departureFilter);
        filters.add(sortFilter);

        busTypeFilter.addActionListener(e -> refreshFilteredView());
        departureFilter.addActionListener(e -> refreshFilteredView());
        sortFilter.addActionListener(e -> refreshFilteredView());

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);
        statusLabel.setFont(UIConfig.FONT_SMALL);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(form, BorderLayout.NORTH);
        center.add(filters, BorderLayout.CENTER);

        card.add(center, BorderLayout.CENTER);
        card.add(statusLabel, BorderLayout.SOUTH);

        wrapper.add(titleRow, BorderLayout.NORTH);
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private JTextField createInput(String title) {
        JTextField field = new JTextField();
        UIConfig.styleField(field);
        field.setPreferredSize(new Dimension(180, 40));
        field.setBorder(BorderFactory.createTitledBorder(title));
        field.addActionListener(e -> search());
        return field;
    }

    private void addAutoSuggest(JTextField field) {
        JPopupMenu popup = new JPopupMenu();

        Timer debounce = new Timer(260, null);
        debounce.setRepeats(false);

        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                debounce.restart();
            }
        });

        debounce.addActionListener(ev -> {
            String text = field.getText() == null ? "" : field.getText().trim();
            popup.setVisible(false);
            popup.removeAll();

            if (text.length() < 1) return;

            String cacheKey = text.toUpperCase();
            List<String> cities = cityCache.containsKey(cacheKey)
                    ? cityCache.get(cacheKey)
                    : cityDAO.searchCities(text);

            cityCache.put(cacheKey, cities);
            if (cityCache.size() > 80) {
                cityCache.clear();
            }

            for (String city : cities) {
                JMenuItem item = new JMenuItem(city);
                item.addActionListener(a -> {
                    field.setText(city);
                    popup.setVisible(false);
                });
                popup.add(item);
            }

            if (popup.getComponentCount() > 0) {
                popup.show(field, 0, field.getHeight());
            }
        });
    }

    private JComponent buildResults() {
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private void setLoading(boolean loading, String text) {
        searchBtn.setEnabled(!loading);
        searchBtn.setText(loading ? "Searching..." : "Search Buses");
        statusLabel.setText(text == null ? " " : text);
        setCursor(Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void search() {
        String src = sourceField.getText() == null ? "" : sourceField.getText().trim();
        String dst = destinationField.getText() == null ? "" : destinationField.getText().trim();
        Date dateObj = dateChooser.getDate();

        if (src.isBlank() || dst.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter source and destination.");
            return;
        }
        if (src.equalsIgnoreCase(dst)) {
            JOptionPane.showMessageDialog(this, "Source and destination cannot be same.");
            return;
        }
        if (dateObj == null) {
            JOptionPane.showMessageDialog(this, "Please select a journey date.");
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd").format(dateObj);
        lastSource = src;
        lastDestination = dst;
        lastDate = date;
        fareCache.clear();

        setLoading(true, "Finding best buses...");
        resultsPanel.removeAll();
        resultsPanel.add(loadingSkeleton());
        resultsPanel.revalidate();
        resultsPanel.repaint();

        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                return searchDAO.searchSchedules(src, dst, date);
            }

            @Override
            protected void done() {
                try {
                    List<String[]> list = get();
                    lastSearchRaw = list == null ? new ArrayList<>() : new ArrayList<>(list);
                    renderResults(lastSearchRaw, src, dst, date);
                    setLoading(false, "Found " + lastSearchRaw.size() + " bus options");
                } catch (Exception e) {
                    resultsPanel.removeAll();
                    resultsPanel.add(emptyState("Search failed. Please try again."));
                    resultsPanel.revalidate();
                    resultsPanel.repaint();
                    setLoading(false, "Search failed");
                }
            }
        };
        worker.execute();
    }

    private void renderResults(List<String[]> list, String src, String dst, String date) {
        resultsPanel.removeAll();

        list = applyFiltersAndSort(list);

        if (list == null || list.isEmpty()) {
            resultsPanel.add(emptyState("No buses found for selected route/date."));
            resultsPanel.revalidate();
            resultsPanel.repaint();
            return;
        }

        for (String[] r : list) {
            resultsPanel.add(createBusCard(r, src, dst, date));
            resultsPanel.add(Box.createVerticalStrut(12));
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private void refreshFilteredView() {
        if (lastSearchRaw == null || lastSearchRaw.isEmpty()) return;
        renderResults(lastSearchRaw, lastSource, lastDestination, lastDate);
    }

    private JComponent loadingSkeleton() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        JLabel lbl = new JLabel("Searching schedules...", SwingConstants.CENTER);
        lbl.setFont(UIConfig.FONT_SUBTITLE);
        lbl.setForeground(UIConfig.TEXT_LIGHT);
        panel.add(lbl, BorderLayout.CENTER);
        return panel;
    }

    private JComponent emptyState(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
        JLabel lbl = new JLabel(message, SwingConstants.CENTER);
        lbl.setFont(UIConfig.FONT_SUBTITLE);
        lbl.setForeground(UIConfig.TEXT_LIGHT);
        panel.add(lbl, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBusCard(String[] r, String src, String dst, String date) {
        int scheduleId = parseInt(r[0]);
        int routeId = parseInt(r[1]);
        String operator = safe(r[2]);
        String busType = safe(r[3]);
        double fare = effectiveFare(routeId, r, src, dst);
        String dep = safe(r[5]);
        String arr = safe(r[6]);
        int fromOrder = parseInt(r[7]);
        int toOrder = parseInt(r[8]);
        String seats = safe(r[9]);

        JPanel card = new JPanel(new BorderLayout(14, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel(operator + "  |  " + busType);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel route = new JLabel(src + "  ->  " + dst);
        route.setForeground(UIConfig.TEXT_LIGHT);
        route.setFont(UIConfig.FONT_SMALL);

        JLabel timing = new JLabel(dep + "  ->  " + arr);
        timing.setFont(UIConfig.FONT_NORMAL);

        JLabel seatInfo = new JLabel(seats + " seats left");
        seatInfo.setForeground(new Color(22, 163, 74));
        seatInfo.setFont(UIConfig.FONT_SMALL);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(route);
        left.add(Box.createVerticalStrut(4));
        left.add(timing);
        left.add(Box.createVerticalStrut(4));
        left.add(seatInfo);

        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setOpaque(false);

        JLabel price = new JLabel("INR " + String.format("%.2f", fare), SwingConstants.RIGHT);
        price.setFont(new Font("Segoe UI", Font.BOLD, 18));
        price.setForeground(UIConfig.PRIMARY);

        JPanel actions = new JPanel(new GridLayout(2, 1, 0, 6));
        actions.setOpaque(false);

        JButton routeBtn = new JButton("Route & Stops");
        UIConfig.secondaryBtn(routeBtn);
        routeBtn.setPreferredSize(new Dimension(130, 34));
        routeBtn.addActionListener(e ->
                openRouteDialog(routeId, scheduleId, src, dst)
        );

        JButton viewBtn = new JButton("Select Seats");
        UIConfig.primaryBtn(viewBtn);
        viewBtn.setPreferredSize(new Dimension(130, 34));
        viewBtn.addActionListener(e -> {
            BookingContext.clear();
            BookingContext.scheduleId = scheduleId;
            BookingContext.routeId = routeId;
            BookingContext.fromOrder = fromOrder;
            BookingContext.toOrder = toOrder;
            BookingContext.fromStop = src;
            BookingContext.toStop = dst;
            BookingContext.farePerSeat = fare;
            BookingContext.journeyDate = date;
            frame.showScreen(MainFrame.SCREEN_BUS_DETAILS);
        });

        actions.add(routeBtn);
        actions.add(viewBtn);

        right.add(price, BorderLayout.NORTH);
        right.add(actions, BorderLayout.SOUTH);

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private void openRouteDialog(int routeId, int scheduleId, String fromStop, String toStop) {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Route Map & Station Timings",
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setLayout(new BorderLayout());
        dialog.setSize(700, 560);
        dialog.setLocationRelativeTo(this);
        dialog.add(new RouteMapPanel(routeId, scheduleId, fromStop, toStop), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<String[]> applyFiltersAndSort(List<String[]> input) {
        if (input == null) return List.of();

        String busType = busTypeFilter == null ? "All Bus Types" : (String) busTypeFilter.getSelectedItem();
        String timeBand = departureFilter == null ? "Any Time" : (String) departureFilter.getSelectedItem();
        String sortBy = sortFilter == null ? "Sort: Departure" : (String) sortFilter.getSelectedItem();

        List<String[]> filtered = input.stream().filter(r -> {
            if (r == null || r.length < 10) return false;
            if (busType != null && !"All Bus Types".equalsIgnoreCase(busType)) {
                String type = safe(r[3]).toUpperCase();
                String wanted = busType.toUpperCase();
                if ("NON-AC".equals(wanted)) {
                    if (type.contains("AC") && !type.contains("NON")) return false;
                } else if (!type.contains(wanted.replace("-", "")) && !type.contains(wanted)) {
                    return false;
                }
            }

            if (timeBand != null && !"Any Time".equalsIgnoreCase(timeBand)) {
                int hh = parseHour(safe(r[5]));
                if (hh < 0) return false;
                return switch (timeBand) {
                    case "Morning (05-12)" -> hh >= 5 && hh < 12;
                    case "Afternoon (12-17)" -> hh >= 12 && hh < 17;
                    case "Evening (17-22)" -> hh >= 17 && hh < 22;
                    case "Night (22-05)" -> hh >= 22 || hh < 5;
                    default -> true;
                };
            }
            return true;
        }).collect(Collectors.toList());

        filtered.sort((a, b) -> {
            switch (sortBy == null ? "" : sortBy) {
                case "Sort: Fare Low->High":
                    return Double.compare(
                            effectiveFare(parseInt(a[1]), a, lastSource, lastDestination),
                            effectiveFare(parseInt(b[1]), b, lastSource, lastDestination)
                    );
                case "Sort: Fare High->Low":
                    return Double.compare(
                            effectiveFare(parseInt(b[1]), b, lastSource, lastDestination),
                            effectiveFare(parseInt(a[1]), a, lastSource, lastDestination)
                    );
                case "Sort: Seats High->Low":
                    return Integer.compare(parseInt(b[9]), parseInt(a[9]));
                default:
                    return safe(a[5]).compareToIgnoreCase(safe(b[5]));
            }
        });

        return filtered;
    }

    private int parseHour(String hhmm) {
        try {
            String time = hhmm == null ? "" : hhmm.trim().toUpperCase();
            if (time.contains("AM") || time.contains("PM")) {
                String core = time.replace("AM", "").replace("PM", "").trim();
                String[] p = core.split(":");
                int h = Integer.parseInt(p[0]);
                boolean pm = time.contains("PM");
                if (pm && h < 12) h += 12;
                if (!pm && h == 12) h = 0;
                return h;
            }
            String[] p = time.split(":");
            return Integer.parseInt(p[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    private double effectiveFare(int routeId, String[] row, String src, String dst) {
        double fare = row != null && row.length > 4 ? parseDouble(row[4]) : 0;
        if (fare > 0) return fare;

        String key = routeId + "|" + (src == null ? "" : src.trim().toUpperCase()) + "|" +
                (dst == null ? "" : dst.trim().toUpperCase());
        if (fareCache.containsKey(key)) return fareCache.get(key);

        double computed = new RouteDAO().calculateFare(routeId, src, dst);
        fareCache.put(key, computed);
        return computed;
    }
}
