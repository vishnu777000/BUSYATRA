package util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class GeneratedRouteMapUtil {

    private static final Color BG_TOP = new Color(246, 249, 255);
    private static final Color BG_BOTTOM = new Color(255, 255, 255);
    private static final Color TRACK = new Color(44, 123, 229);
    private static final Color TRACK_SOFT = new Color(196, 214, 243);
    private static final Color NORMAL_STOP = new Color(64, 99, 138);
    private static final Color BOARDING_STOP = new Color(22, 163, 74);
    private static final Color DROP_STOP = new Color(220, 53, 69);
    private static final Color TEXT = new Color(36, 43, 52);
    private static final Color SUBTEXT = new Color(106, 118, 132);

    private GeneratedRouteMapUtil() {
    }

    public static ImageIcon buildFromRows(List<String[]> stopRows, String fromStop, String toStop, int width, int height) {
        List<String> stops = new ArrayList<>();
        if (stopRows != null) {
            for (String[] row : stopRows) {
                if (row != null && row.length > 0 && row[0] != null && !row[0].isBlank()) {
                    stops.add(row[0].trim());
                }
            }
        }
        return build(stops, fromStop, toStop, width, height);
    }

    public static ImageIcon build(List<String> stops, String fromStop, String toStop, int width, int height) {
        int w = Math.max(220, width);
        int h = Math.max(120, height);

        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        try {
            enableQuality(g2);
            paintBackground(g2, w, h);

            if (stops == null || stops.isEmpty()) {
                drawEmptyState(g2, w, h);
                return new ImageIcon(canvas);
            }

            int count = stops.size();
            int leftPad = 28;
            int rightPad = 28;
            int baseline = h / 2;
            int upperY = Math.max(38, baseline - 26);
            int lowerY = Math.min(h - 36, baseline + 26);
            int labelTop = 22;
            int labelBottom = h - 18;

            int[] xs = new int[count];
            int[] ys = new int[count];
            for (int i = 0; i < count; i++) {
                xs[i] = count == 1
                        ? w / 2
                        : leftPad + (int) Math.round((double) (w - leftPad - rightPad) * i / (count - 1));
                ys[i] = (i % 2 == 0) ? upperY : lowerY;
            }

            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(TRACK_SOFT);
            Path2D path = new Path2D.Double();
            path.moveTo(xs[0], ys[0]);
            for (int i = 1; i < count; i++) {
                int controlX = (xs[i - 1] + xs[i]) / 2;
                path.curveTo(controlX, ys[i - 1], controlX, ys[i], xs[i], ys[i]);
            }
            g2.draw(path);

            g2.setFont(new Font("Segoe UI", Font.BOLD, w >= 360 ? 12 : 11));
            FontMetrics fm = g2.getFontMetrics();

            for (int i = 0; i < count; i++) {
                String stop = stops.get(i);
                boolean isBoarding = equalsStop(stop, fromStop);
                boolean isDrop = equalsStop(stop, toStop);
                Color stopColor = isBoarding ? BOARDING_STOP : (isDrop ? DROP_STOP : NORMAL_STOP);

                g2.setColor(stopColor);
                g2.fillOval(xs[i] - 8, ys[i] - 8, 16, 16);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(xs[i] - 8, ys[i] - 8, 16, 16);

                g2.setColor(TEXT);
                String label = trimLabel(stop, w >= 420 ? 18 : 12);
                int textWidth = fm.stringWidth(label);
                int textX = Math.max(8, Math.min(w - textWidth - 8, xs[i] - (textWidth / 2)));
                int textY = (i % 2 == 0) ? labelTop : labelBottom;
                g2.drawString(label, textX, textY);

                if (isBoarding || isDrop) {
                    String tag = isBoarding ? "Boarding" : "Drop";
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    FontMetrics tagFm = g2.getFontMetrics();
                    int tagWidth = tagFm.stringWidth(tag) + 10;
                    int tagX = Math.max(6, Math.min(w - tagWidth - 6, xs[i] - (tagWidth / 2)));
                    int tagY = (i % 2 == 0) ? textY + 8 : textY - 22;

                    g2.setColor(new Color(255, 255, 255, 220));
                    g2.fillRoundRect(tagX, tagY, tagWidth, 16, 10, 10);
                    g2.setColor(stopColor);
                    g2.drawRoundRect(tagX, tagY, tagWidth, 16, 10, 10);
                    g2.drawString(tag, tagX + 5, tagY + 11);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, w >= 360 ? 12 : 11));
                    fm = g2.getFontMetrics();
                }
            }

            g2.setColor(SUBTEXT);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.drawString("Auto-generated route sketch from route stops", 14, h - 10);
        } finally {
            g2.dispose();
        }

        return new ImageIcon(canvas);
    }

    private static void enableQuality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static void paintBackground(Graphics2D g2, int w, int h) {
        GradientPaint paint = new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM);
        g2.setPaint(paint);
        g2.fillRoundRect(0, 0, w, h, 24, 24);

        g2.setColor(new Color(219, 229, 241));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 24, 24);
    }

    private static void drawEmptyState(Graphics2D g2, int w, int h) {
        g2.setColor(SUBTEXT);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        String text = "No route stops available";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (w - fm.stringWidth(text)) / 2, h / 2);
    }

    private static boolean equalsStop(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private static String trimLabel(String label, int maxLen) {
        if (label == null) {
            return "";
        }
        String clean = label.trim();
        if (clean.length() <= maxLen) {
            return clean;
        }
        return clean.substring(0, Math.max(0, maxLen - 1)) + ".";
    }
}
