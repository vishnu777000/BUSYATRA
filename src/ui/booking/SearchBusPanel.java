package ui.booking;

import com.toedter.calendar.JDateChooser;
import config.UIConfig;
import dao.CityDAO;
import dao.RouteDAO;
import dao.SearchDAO;
import ui.common.MainFrame;
import util.BookingContext;
import util.FareCalculator;
import util.IconUtil;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
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
    private final RouteDAO routeDAO = new RouteDAO();
    private final Map<String, List<String>> cityCache = new HashMap<>();
    private final Map<String, Double> fareCache = new HashMap<>();
    private final Map<Integer, RoutePreviewData> routePreviewCache = new ConcurrentHashMap<>();
    private final Set<Integer> routePreviewLoading = ConcurrentHashMap.newKeySet();
    private volatile long suggestionToken = 0L;
    private volatile long searchToken = 0L;

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

        JLabel sub = new JLabel("Search by boarding and drop stops with live route-stop matching");
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

        sourceField = createInput("Boarding Stop / City");
        destinationField = createInput("Drop Stop / City");
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
        dateChooser.setDate(todayStart());
        dateChooser.setDateFormatString("yyyy-MM-dd");
        dateChooser.setPreferredSize(new Dimension(160, 40));
        dateChooser.setMinSelectableDate(todayStart());

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
            long token = ++suggestionToken;

            List<String> cached = cityCache.get(cacheKey);
            if (cached != null) {
                showSuggestions(field, popup, cached, cacheKey, token);
                return;
            }

            SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<String> doInBackground() {
                    return cityDAO.searchCities(text);
                }

                @Override
                protected void done() {
                    try {
                        List<String> cities = get();
                        cityCache.put(cacheKey, cities == null ? List.of() : cities);
                        if (cityCache.size() > 80) {
                            cityCache.clear();
                        }
                        showSuggestions(field, popup, cities, cacheKey, token);
                    } catch (Exception ignored) {
                        popup.setVisible(false);
                    }
                }
            };
            worker.execute();
        });
    }

    private void showSuggestions(JTextField field, JPopupMenu popup, List<String> cities, String cacheKey, long token) {
        if (token != suggestionToken || !field.isShowing()) {
            return;
        }

        String current = field.getText() == null ? "" : field.getText().trim().toUpperCase();
        if (!cacheKey.equals(current)) {
            return;
        }

        popup.setVisible(false);
        popup.removeAll();

        if (cities == null || cities.isEmpty()) {
            return;
        }

        for (String city : cities) {
            JMenuItem item = new JMenuItem(city);
            item.addActionListener(a -> {
                field.setText(city);
                popup.setVisible(false);
            });
            popup.add(item);
        }

        popup.show(field, 0, field.getHeight());
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
        String rawSrc = sourceField.getText() == null ? "" : sourceField.getText().trim();
        String rawDst = destinationField.getText() == null ? "" : destinationField.getText().trim();
        Date dateObj = dateChooser.getDate();

        if (rawSrc.isBlank() || rawDst.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter source and destination.");
            return;
        }

        String src = cityDAO.resolveCityName(rawSrc);
        String dst = cityDAO.resolveCityName(rawDst);
        if (!src.equals(rawSrc)) {
            sourceField.setText(src);
        }
        if (!dst.equals(rawDst)) {
            destinationField.setText(dst);
        }

        if (src.equalsIgnoreCase(dst)) {
            JOptionPane.showMessageDialog(this, "Source and destination cannot be same.");
            return;
        }
        if (dateObj == null) {
            JOptionPane.showMessageDialog(this, "Please select a journey date.");
            return;
        }
        if (dateObj.before(todayStart())) {
            dateChooser.setDate(todayStart());
            JOptionPane.showMessageDialog(this, "Past journey dates are not allowed.");
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd").format(dateObj);
        lastSource = src;
        lastDestination = dst;
        lastDate = date;
        fareCache.clear();
        long token = ++searchToken;

        setLoading(true, "Finding buses for the selected stops...");
        resultsPanel.removeAll();
        resultsPanel.add(loadingSkeleton());
        resultsPanel.revalidate();
        resultsPanel.repaint();

        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                List<String[]> results = searchDAO.searchSchedules(src, dst, date);
                preloadFares(results, src, dst);
                return results;
            }

            @Override
            protected void done() {
                if (token != searchToken) {
                    return;
                }
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

    private void preloadFares(List<String[]> rows, String src, String dst) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        for (String[] row : rows) {
            if (row == null || row.length <= 4) {
                continue;
            }

            int routeId = parseInt(row[1]);
            String key = fareKey(routeId, src, dst);
            double fare = parseDouble(row[4]);

            if (fare <= 0) {
                Double cached = fareCache.get(key);
                if (cached != null) {
                    fare = cached;
                } else if (routeId > 0) {
                    fare = fallbackFare(routeId, safe(row[3]), src, dst);
                }
            }

            fareCache.put(key, fare);
            row[4] = String.valueOf(fare);
        }
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

        JLabel routeMeta = new JLabel("Checking route coverage...");
        routeMeta.setForeground(new Color(30, 64, 175));
        routeMeta.setFont(UIConfig.FONT_SMALL);

        JLabel routeTrail = new JLabel(" ");
        routeTrail.setForeground(UIConfig.TEXT_LIGHT);
        routeTrail.setFont(UIConfig.FONT_SMALL);

        JLabel timing = new JLabel(dep + "  ->  " + arr);
        timing.setFont(UIConfig.FONT_NORMAL);

        JLabel seatInfo = new JLabel(seats + " seats left");
        seatInfo.setForeground(new Color(22, 163, 74));
        seatInfo.setFont(UIConfig.FONT_SMALL);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(route);
        left.add(Box.createVerticalStrut(4));
        left.add(routeMeta);
        left.add(Box.createVerticalStrut(3));
        left.add(routeTrail);
        left.add(Box.createVerticalStrut(4));
        left.add(timing);
        left.add(Box.createVerticalStrut(4));
        left.add(seatInfo);

        populateRoutePreview(routeId, src, dst, routeMeta, routeTrail);

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

        fare = fareCache.getOrDefault(fareKey(routeId, src, dst), 0.0);
        if (fare > 0) {
            return fare;
        }

        return fallbackFare(routeId, row != null && row.length > 3 ? safe(row[3]) : "", src, dst);
    }

    private String fareKey(int routeId, String src, String dst) {
        return routeId + "|" + (src == null ? "" : src.trim().toUpperCase()) + "|" +
                (dst == null ? "" : dst.trim().toUpperCase());
    }

    private double fallbackFare(int routeId, String busType, String src, String dst) {
        if (routeId <= 0) {
            return 0;
        }

        int fromDistance = routeDAO.getDistance(routeId, src);
        int toDistance = routeDAO.getDistance(routeId, dst);
        if (fromDistance < 0 || toDistance < 0) {
            return 0;
        }
        double rate = routeDAO.getRatePerKm(routeId);
        int segmentDistance = Math.abs(toDistance - fromDistance);
        return FareCalculator.calculateSegmentFare(segmentDistance, rate, busType, 0);
    }

    private void populateRoutePreview(int routeId, String src, String dst, JLabel metaLabel, JLabel trailLabel) {
        if (routeId <= 0) {
            metaLabel.setText("Route details unavailable");
            trailLabel.setText(" ");
            return;
        }

        RoutePreviewData cached = routePreviewCache.get(routeId);
        if (cached != null) {
            applyRoutePreview(cached, src, dst, metaLabel, trailLabel);
            return;
        }

        metaLabel.setText("Checking route coverage...");
        trailLabel.setText("Open Route & Stops for the full route line.");

        if (!routePreviewLoading.add(routeId)) {
            return;
        }

        SwingWorker<RoutePreviewData, Void> worker = new SwingWorker<>() {
            @Override
            protected RoutePreviewData doInBackground() {
                RoutePreviewData data = new RoutePreviewData();
                List<String[]> stopRows = routeDAO.getStopsByRoute(routeId);
                if (stopRows == null) {
                    return data;
                }

                Set<String> seen = new HashSet<>();
                for (String[] row : stopRows) {
                    String stop = row != null && row.length > 0 && row[0] != null ? row[0].trim() : "";
                    if (stop.isBlank()) {
                        continue;
                    }
                    String key = stop.toUpperCase(Locale.ENGLISH);
                    if (seen.add(key)) {
                        data.stops.add(stop);
                    }
                }
                return data;
            }

            @Override
            protected void done() {
                routePreviewLoading.remove(routeId);
                try {
                    RoutePreviewData data = get();
                    routePreviewCache.put(routeId, data);
                    applyRoutePreview(data, src, dst, metaLabel, trailLabel);
                } catch (Exception ignored) {
                    metaLabel.setText("Open Route & Stops to verify the route");
                    trailLabel.setText(" ");
                }
            }
        };
        worker.execute();
    }

    private void applyRoutePreview(RoutePreviewData data, String src, String dst, JLabel metaLabel, JLabel trailLabel) {
        if (data == null || data.stops.isEmpty()) {
            metaLabel.setText("Open Route & Stops to verify the route");
            trailLabel.setText("Live stop list is unavailable for this bus.");
            return;
        }

        int fromIndex = stopIndex(data.stops, src);
        int toIndex = stopIndex(data.stops, dst);
        String fullLine = summarizeStops(data.stops, 6);

        if (fromIndex >= 0 && toIndex >= 0 && fromIndex < toIndex) {
            List<String> segmentStops = new ArrayList<>(data.stops.subList(fromIndex, toIndex + 1));
            int intermediateStops = Math.max(0, segmentStops.size() - 2);
            metaLabel.setText(
                    intermediateStops == 0
                            ? "This bus directly covers your selected segment"
                            : "This bus passes through your selected segment via " + intermediateStops + " stop(s)"
            );
            trailLabel.setText(
                    "<html><body style='width:430px'>Bus line: " + escapeHtml(fullLine) +
                            "<br/>Your segment: " + escapeHtml(summarizeStops(segmentStops, 5)) + "</body></html>"
            );
            return;
        }

        metaLabel.setText("Full route line available for this bus");
        trailLabel.setText("<html><body style='width:430px'>Bus line: " + escapeHtml(fullLine) + "</body></html>");
    }

    private int stopIndex(List<String> stops, String target) {
        if (stops == null || stops.isEmpty() || target == null || target.isBlank()) {
            return -1;
        }
        for (int i = 0; i < stops.size(); i++) {
            String stop = stops.get(i);
            if (stop != null && stop.trim().equalsIgnoreCase(target.trim())) {
                return i;
            }
        }
        return -1;
    }

    private String summarizeStops(List<String> stops, int maxShown) {
        if (stops == null || stops.isEmpty()) {
            return "";
        }

        List<String> clean = stops.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
        if (clean.isEmpty()) {
            return "";
        }
        if (clean.size() <= Math.max(2, maxShown)) {
            return String.join(" -> ", clean);
        }

        List<String> visible = new ArrayList<>();
        visible.add(clean.get(0));

        int middleSlots = Math.max(1, maxShown - 2);
        int remaining = clean.size() - 2;
        if (remaining <= middleSlots) {
            visible.addAll(clean.subList(1, clean.size() - 1));
        } else {
            for (int i = 1; i <= middleSlots; i++) {
                int idx = (int) Math.round((i * remaining) / (double) (middleSlots + 1));
                idx = Math.max(1, Math.min(clean.size() - 2, idx));
                String stop = clean.get(idx);
                if (!visible.contains(stop)) {
                    visible.add(stop);
                }
            }
        }

        visible.add(clean.get(clean.size() - 1));
        return String.join(" -> ", visible);
    }

    private String escapeHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private Date todayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private static class RoutePreviewData {
        List<String> stops = new ArrayList<>();
    }
}
