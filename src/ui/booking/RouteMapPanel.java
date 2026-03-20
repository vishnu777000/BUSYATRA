package ui.booking;

import config.UIConfig;
import dao.RouteDAO;
import util.BookingContext;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;

public class RouteMapPanel extends JPanel {

    private final int routeId;
    private final int scheduleId;
    private final String fromStop;
    private final String toStop;
    private final JLabel mapLabel = new JLabel("Loading route map...", SwingConstants.CENTER);
    private final JPanel timelineHolder = new JPanel(new BorderLayout());
    private ImageIcon currentMapIcon;

    public RouteMapPanel(int routeId) {
        this(routeId, BookingContext.scheduleId, BookingContext.fromStop, BookingContext.toStop);
    }

    public RouteMapPanel(int routeId, int scheduleId, String fromStop, String toStop) {
        this.routeId = routeId;
        this.scheduleId = scheduleId;
        this.fromStop = fromStop == null ? "" : fromStop.trim();
        this.toStop = toStop == null ? "" : toStop.trim();

        setLayout(new BorderLayout(14, 14));
        setBackground(UIConfig.BACKGROUND);

        add(mapSection(), BorderLayout.CENTER);
        add(stopsSection(), BorderLayout.SOUTH);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                renderScaledMap();
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
        mapLabel.setPreferredSize(new Dimension(500, 220));

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
                RouteData data = new RouteData();
                data.mapPath = new RouteDAO().getRouteMap(routeId);
                return data;
            }

            @Override
            protected void done() {
                try {
                    RouteData data = get();
                    renderMap(data.mapPath);
                } catch (Exception e) {
                    mapLabel.setText("Unable to load map");
                }
            }
        };
        worker.execute();
    }

    private void renderMap(String mapPath) {
        currentMapIcon = resolveMapIcon(mapPath);
        if (currentMapIcon == null || currentMapIcon.getIconWidth() <= 0) {
            mapLabel.setIcon(null);
            mapLabel.setText("No route map uploaded");
            return;
        }
        renderScaledMap();
    }

    private void renderScaledMap() {
        if (currentMapIcon == null || currentMapIcon.getIconWidth() <= 0) return;

        int w = Math.max(360, Math.min(700, mapLabel.getWidth() > 0 ? mapLabel.getWidth() : 500));
        int h = 220;
        Image scaled = currentMapIcon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        mapLabel.setIcon(new ImageIcon(scaled));
        mapLabel.setText("");
    }

    private ImageIcon resolveMapIcon(String mapPath) {
        if (mapPath == null || mapPath.isBlank()) return null;

        String path = mapPath.trim().replace("\\", "/");

        if (path.startsWith("/")) {
            URL url = getClass().getResource(path);
            if (url != null) return new ImageIcon(url);
        }

        if (path.startsWith("resources/")) {
            URL url = getClass().getResource("/" + path);
            if (url != null) return new ImageIcon(url);
        }

        URL routeUrl = getClass().getResource("/resources/routes/" + path);
        if (routeUrl != null) return new ImageIcon(routeUrl);

        File file = new File(path);
        if (file.exists()) {
            return new ImageIcon(file.getAbsolutePath());
        }
        return null;
    }

    private static class RouteData {
        String mapPath;
    }
}
