package ui.booking;

import config.UIConfig;
import dao.RouteDAO;
import util.BookingContext;
import util.GeneratedRouteMapUtil;
import util.RouteMapImageUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class RouteMapPanel extends JPanel {

    private final int routeId;
    private final int scheduleId;
    private final String fromStop;
    private final String toStop;
    private final JLabel mapLabel = new JLabel("Loading route map...", SwingConstants.CENTER);
    private final JPanel timelineHolder = new JPanel(new BorderLayout());
    private final Timer resizeDebounce;
    private ImageIcon currentMapIcon;
    private int lastRenderedWidth = -1;
    private int lastRenderedHeight = -1;

    public RouteMapPanel(int routeId) {
        this(routeId, BookingContext.scheduleId, BookingContext.fromStop, BookingContext.toStop);
    }

    public RouteMapPanel(int routeId, int scheduleId, String fromStop, String toStop) {
        this.routeId = routeId;
        this.scheduleId = scheduleId;
        this.fromStop = fromStop == null ? "" : fromStop.trim();
        this.toStop = toStop == null ? "" : toStop.trim();
        this.resizeDebounce = new Timer(70, e -> renderScaledMap());
        this.resizeDebounce.setRepeats(false);

        setLayout(new BorderLayout(14, 14));
        setBackground(UIConfig.BACKGROUND);

        add(mapSection(), BorderLayout.CENTER);
        add(stopsSection(), BorderLayout.SOUTH);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                resizeDebounce.restart();
            }
        });

        loadMapAndStops();
    }

    private JComponent mapSection() {
        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);

        JLabel title = new JLabel("Route Map");
        title.setFont(UIConfig.FONT_SUBTITLE);

        mapLabel.setFont(UIConfig.FONT_SMALL);
        mapLabel.setForeground(UIConfig.TEXT_LIGHT);
        mapLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        mapLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        mapLabel.setPreferredSize(new Dimension(500, 240));
        mapLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        card.add(title, BorderLayout.NORTH);
        card.add(mapLabel, BorderLayout.CENTER);
        return card;
    }

    private JComponent stopsSection() {
        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);

        JLabel title = new JLabel("Route Line & Timings");
        title.setFont(UIConfig.FONT_SUBTITLE);

        timelineHolder.setOpaque(false);
        timelineHolder.add(
                new RouteTimelinePanel(routeId, scheduleId, fromStop, toStop),
                BorderLayout.CENTER
        );

        JScrollPane scroll = new JScrollPane(timelineHolder);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(0, 180));

        card.add(title, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void loadMapAndStops() {
        SwingWorker<RouteData, Void> worker = new SwingWorker<>() {
            @Override
            protected RouteData doInBackground() {
                RouteDAO mapDao = new RouteDAO();
                RouteDAO stopDao = new RouteDAO();
                RouteData data = new RouteData();
                data.mapPath = mapDao.getRouteMap(routeId);
                data.stopRows = stopDao.getStopsByRoute(routeId);
                data.errorMessage = stopDao.hasLastError() ? stopDao.getLastError() : mapDao.getLastError();
                data.hasError = stopDao.hasLastError() || mapDao.hasLastError();
                return data;
            }

            @Override
            protected void done() {
                try {
                    RouteData data = get();
                    renderMap(data.mapPath, data.stopRows, data.hasError ? data.errorMessage : null);
                } catch (ExecutionException e) {
                    showMapStatus(e.getCause() != null && e.getCause().getMessage() != null
                            ? e.getCause().getMessage().trim()
                            : "Unable to load map");
                } catch (Exception e) {
                    showMapStatus("Unable to load map");
                }
            }
        };
        worker.execute();
    }

    private void renderMap(String mapPath, List<String[]> stopRows, String errorMessage) {
        currentMapIcon = RouteMapImageUtil.resolve(mapPath);
        lastRenderedWidth = -1;
        lastRenderedHeight = -1;
        if (currentMapIcon == null || currentMapIcon.getIconWidth() <= 0) {
            currentMapIcon = GeneratedRouteMapUtil.buildFromRows(stopRows, fromStop, toStop, 640, 240);
        }
        if (currentMapIcon == null || currentMapIcon.getIconWidth() <= 0) {
            showMapStatus((errorMessage == null || errorMessage.isBlank()) ? "No route map uploaded" : errorMessage);
            return;
        }
        renderScaledMap();
    }

    private void renderScaledMap() {
        if (currentMapIcon == null || currentMapIcon.getIconWidth() <= 0) return;

        int w = Math.max(320, Math.min(700, mapLabel.getWidth() > 0 ? mapLabel.getWidth() - 16 : 500));
        int h = Math.max(180, Math.min(280, mapLabel.getHeight() > 0 ? mapLabel.getHeight() - 16 : 240));
        if (w == lastRenderedWidth && h == lastRenderedHeight) {
            return;
        }

        lastRenderedWidth = w;
        lastRenderedHeight = h;
        mapLabel.setIcon(RouteMapImageUtil.scaleToFit(currentMapIcon, w, h));
        mapLabel.setText("");
    }

    private void showMapStatus(String message) {
        currentMapIcon = null;
        mapLabel.setIcon(null);
        mapLabel.setText(message == null || message.isBlank() ? "No route map uploaded" : message);
    }

    private static class RouteData {
        String mapPath;
        String errorMessage;
        boolean hasError;
        List<String[]> stopRows;
    }
}
