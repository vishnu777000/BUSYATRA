package ui.booking;

import config.UIConfig;
import dao.RouteDAO;
import dao.ScheduleDAO;
import util.BookingContext;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class RouteTimelinePanel extends JPanel {

    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30_000L;

    private final int routeId;
    private final int scheduleId;
    private final String fromStop;
    private final String toStop;
    private final JPanel rowsPanel = new JPanel();
    private final JLabel header = new JLabel("Route Line & Timings");

    public RouteTimelinePanel(int routeId) {
        this(routeId, BookingContext.scheduleId, BookingContext.fromStop, BookingContext.toStop);
    }

    public RouteTimelinePanel(int routeId, String fromStop, String toStop) {
        this(routeId, BookingContext.scheduleId, fromStop, toStop);
    }

    public RouteTimelinePanel(int routeId, int scheduleId, String fromStop, String toStop) {
        this.routeId = routeId;
        this.scheduleId = scheduleId;
        this.fromStop = fromStop == null ? "" : fromStop.trim();
        this.toStop = toStop == null ? "" : toStop.trim();

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);

        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setForeground(UIConfig.TEXT_LIGHT);
        add(header, BorderLayout.NORTH);

        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setOpaque(false);
        add(rowsPanel, BorderLayout.CENTER);

        loadTimeline();
    }

    private void loadTimeline() {
        String cacheKey = cacheKey();
        CacheEntry cached = CACHE.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.loadedAtMs) <= CACHE_TTL_MS) {
            render(cached.data);
            return;
        }

        rowsPanel.removeAll();
        rowsPanel.add(statusLabel("Loading route timeline..."));

        SwingWorker<TimelineData, Void> worker = new SwingWorker<>() {
            @Override
            protected TimelineData doInBackground() {
                TimelineData data = new TimelineData();

                RouteDAO routeDAO = new RouteDAO();
                List<String[]> stopRows = routeDAO.getStopsByRoute(routeId);
                data.errorMessage = routeDAO.getLastError();
                data.hasError = routeDAO.hasLastError();
                for (String[] row : stopRows) {
                    if (row == null || row.length == 0) continue;
                    String name = safe(row, 0);
                    if (name.isBlank()) continue;

                    StopPoint p = new StopPoint();
                    p.name = name;
                    p.order = parseInt(safe(row, 1), -1);
                    p.distance = parseDouble(safe(row, 2), Double.NaN);
                    data.stops.add(p);
                }

                if (scheduleId > 0) {
                    String[] schedule = new ScheduleDAO().getScheduleDetails(scheduleId);
                    if (schedule != null && schedule.length >= 6) {
                        data.departure = parseDateTime(schedule[4]);
                        data.arrival = parseDateTime(schedule[5]);
                    }
                }
                return data;
            }

            @Override
            protected void done() {
                try {
                    TimelineData data = get();
                    if (data != null && !data.stops.isEmpty()) {
                        CacheEntry entry = new CacheEntry();
                        entry.loadedAtMs = System.currentTimeMillis();
                        entry.data = data;
                        CACHE.put(cacheKey, entry);
                    }
                    render(data);
                } catch (ExecutionException e) {
                    rowsPanel.removeAll();
                    Throwable cause = e.getCause();
                    CacheEntry fallback = CACHE.get(cacheKey);
                    if (fallback != null && fallback.data != null && !fallback.data.stops.isEmpty()) {
                        render(fallback.data);
                        return;
                    }
                    rowsPanel.add(statusLabel(cause != null && cause.getMessage() != null
                            ? cause.getMessage().trim()
                            : "Unable to load route timeline"));
                    rowsPanel.revalidate();
                    rowsPanel.repaint();
                } catch (Exception ignored) {
                    rowsPanel.removeAll();
                    CacheEntry fallback = CACHE.get(cacheKey);
                    if (fallback != null && fallback.data != null && !fallback.data.stops.isEmpty()) {
                        render(fallback.data);
                        return;
                    }
                    rowsPanel.add(statusLabel("Unable to load route timeline"));
                    rowsPanel.revalidate();
                    rowsPanel.repaint();
                }
            }
        };
        worker.execute();
    }

    private void render(TimelineData data) {
        rowsPanel.removeAll();

        if (data == null || data.stops.isEmpty()) {
            rowsPanel.add(statusLabel(data != null && data.hasError
                    ? data.errorMessage
                    : "No route stops available"));
            rowsPanel.revalidate();
            rowsPanel.repaint();
            return;
        }

        boolean hasWindow = data.departure != null && data.arrival != null
                && data.arrival.isAfter(data.departure);
        if (hasWindow) {
            header.setText("Route Line & Station ETA");
        } else {
            header.setText("Route Line");
        }

        int fromIdx = indexOfStop(data.stops, fromStop);
        int toIdx = indexOfStop(data.stops, toStop);
        int minIdx = (fromIdx >= 0 && toIdx >= 0) ? Math.min(fromIdx, toIdx) : -1;
        int maxIdx = (fromIdx >= 0 && toIdx >= 0) ? Math.max(fromIdx, toIdx) : -1;

        double minDist = Double.POSITIVE_INFINITY;
        double maxDist = Double.NEGATIVE_INFINITY;
        boolean hasDistance = true;
        for (StopPoint s : data.stops) {
            if (Double.isNaN(s.distance)) {
                hasDistance = false;
                break;
            }
            minDist = Math.min(minDist, s.distance);
            maxDist = Math.max(maxDist, s.distance);
        }
        if (!hasDistance || minDist == Double.POSITIVE_INFINITY || maxDist == Double.NEGATIVE_INFINITY) {
            minDist = 0;
            maxDist = Math.max(1, data.stops.size() - 1);
        }
        double totalUnits = Math.max(1.0, maxDist - minDist);
        long totalMinutes = hasWindow ? Math.max(1L, Duration.between(data.departure, data.arrival).toMinutes()) : -1L;

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

        for (int i = 0; i < data.stops.size(); i++) {
            StopPoint s = data.stops.get(i);

            double positionUnits = hasDistance
                    ? (s.distance - minDist)
                    : i;
            long offsetMins = (long) Math.round((positionUnits / totalUnits) * Math.max(0, totalMinutes));

            String rightText = "";
            if (hasWindow) {
                LocalDateTime eta = data.departure.plusMinutes(offsetMins);
                rightText = eta.format(timeFmt) + " (" + offsetMins + " min)";
            } else if (!Double.isNaN(s.distance)) {
                rightText = ((int) Math.round(s.distance)) + " km";
            }

            String tag = "";
            if (equalsStop(s.name, fromStop)) tag = "Boarding";
            if (equalsStop(s.name, toStop)) tag = "Drop";

            boolean inLeg = minIdx >= 0 && maxIdx >= 0 && i >= minIdx && i <= maxIdx;
            int orderNo = s.order > 0 ? s.order : (i + 1);
            rowsPanel.add(stopRow(orderNo, s.name, rightText, tag, inLeg, i == data.stops.size() - 1));

            if (i < data.stops.size() - 1) {
                rowsPanel.add(Box.createVerticalStrut(4));
            }
        }

        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    private JComponent stopRow(int orderNo, String stopName, String rightText, String tag, boolean highlight, boolean isLast) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JPanel timeline = new JPanel(new BorderLayout());
        timeline.setOpaque(false);
        timeline.setPreferredSize(new Dimension(28, 34));

        JLabel marker = new JLabel(String.format("%02d", orderNo), SwingConstants.CENTER);
        marker.setOpaque(true);
        marker.setFont(new Font("Segoe UI", Font.BOLD, 10));
        marker.setForeground(Color.WHITE);
        marker.setBackground(highlight ? UIConfig.PRIMARY : new Color(155, 166, 180));
        marker.setPreferredSize(new Dimension(24, 18));

        timeline.add(marker, BorderLayout.NORTH);
        if (!isLast) {
            JSeparator line = new JSeparator(SwingConstants.VERTICAL);
            line.setForeground(highlight ? UIConfig.PRIMARY : new Color(206, 214, 226));
            line.setPreferredSize(new Dimension(2, 14));
            timeline.add(line, BorderLayout.CENTER);
        }

        JPanel textWrap = new JPanel(new BorderLayout(6, 0));
        textWrap.setOpaque(false);

        JLabel name = new JLabel(stopName);
        name.setFont(UIConfig.FONT_NORMAL);
        name.setForeground(highlight ? UIConfig.PRIMARY.darker() : UIConfig.TEXT);

        JLabel meta = new JLabel(rightText == null ? "" : rightText);
        meta.setFont(UIConfig.FONT_SMALL);
        meta.setForeground(UIConfig.TEXT_LIGHT);

        textWrap.add(name, BorderLayout.WEST);
        textWrap.add(meta, BorderLayout.EAST);

        row.add(timeline, BorderLayout.WEST);
        row.add(textWrap, BorderLayout.CENTER);

        if (tag != null && !tag.isBlank()) {
            JLabel chip = new JLabel(tag);
            chip.setFont(UIConfig.FONT_SMALL);
            chip.setOpaque(true);
            chip.setBackground(highlight ? new Color(255, 234, 238) : new Color(240, 240, 240));
            chip.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            chip.setForeground(UIConfig.PRIMARY);
            row.add(chip, BorderLayout.EAST);
        }

        return row;
    }

    private JLabel statusLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConfig.FONT_SMALL);
        lbl.setForeground(UIConfig.TEXT_LIGHT);
        return lbl;
    }

    private String cacheKey() {
        return routeId + "|" + scheduleId + "|" + fromStop.toUpperCase(Locale.ENGLISH) + "|" + toStop.toUpperCase(Locale.ENGLISH);
    }

    private int indexOfStop(List<StopPoint> stops, String name) {
        if (name == null || name.isBlank()) return -1;
        for (int i = 0; i < stops.size(); i++) {
            if (equalsStop(stops.get(i).name, name)) return i;
        }
        return -1;
    }

    private boolean equalsStop(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private String safe(String[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length || arr[idx] == null) return "";
        return arr[idx].trim();
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String t = raw.trim();

        List<DateTimeFormatter> dateTimeFormats = new ArrayList<>();
        dateTimeFormats.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        dateTimeFormats.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        dateTimeFormats.add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        dateTimeFormats.add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        dateTimeFormats.add(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.ENGLISH));

        for (DateTimeFormatter f : dateTimeFormats) {
            try {
                return LocalDateTime.parse(t, f);
            } catch (DateTimeParseException ignored) {
                
            }
        }

        List<DateTimeFormatter> timeOnly = new ArrayList<>();
        timeOnly.add(DateTimeFormatter.ofPattern("HH:mm:ss"));
        timeOnly.add(DateTimeFormatter.ofPattern("HH:mm"));
        timeOnly.add(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));

        for (DateTimeFormatter f : timeOnly) {
            try {
                LocalTime lt = LocalTime.parse(t, f);
                return LocalDateTime.of(LocalDate.now(), lt);
            } catch (DateTimeParseException ignored) {
                
            }
        }

        return null;
    }

    private static class StopPoint {
        String name;
        int order;
        double distance;
    }

    private static class TimelineData {
        List<StopPoint> stops = new ArrayList<>();
        LocalDateTime departure;
        LocalDateTime arrival;
        String errorMessage;
        boolean hasError;
    }

    private static class CacheEntry {
        long loadedAtMs;
        TimelineData data;
    }
}
