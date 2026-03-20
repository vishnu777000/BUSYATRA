package ui.common;

import dao.BannerDAO;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BannerSliderPanel extends JPanel {

    private List<String> images = new ArrayList<>();
    private final JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
    private int index = 0;
    private final Timer sliderTimer;
    private long lastReloadMs = 0L;

    public BannerSliderPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(800, 180));
        setOpaque(false);
        add(imageLabel, BorderLayout.CENTER);

        loadImages();

        sliderTimer = new Timer(3500, e -> {
            if (System.currentTimeMillis() - lastReloadMs > 30_000L) {
                loadImages();
            }
            if (images == null || images.isEmpty()) return;
            index = (index + 1) % images.size();
            setImage(images.get(index));
        });
        sliderTimer.start();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (images != null && !images.isEmpty()) {
                    int safeIndex = Math.max(0, Math.min(index, images.size() - 1));
                    setImage(images.get(safeIndex));
                }
            }
        });
    }

    private void loadImages() {
        images = new BannerDAO().getHomepageBannerImages();
        lastReloadMs = System.currentTimeMillis();
        images.removeIf(path -> path == null || path.isBlank());
        if (images != null && !images.isEmpty()) {
            setImage(images.get(0));
        } else {
            imageLabel.setText("No active banners");
            imageLabel.setIcon(null);
        }
    }

    private void setImage(String rawPath) {
        try {
            ImageIcon icon = resolveImage(rawPath);
            if (icon == null || icon.getIconWidth() <= 0) {
                imageLabel.setText("Banner not available");
                imageLabel.setIcon(null);
                return;
            }

            int w = Math.max(450, getWidth() > 0 ? getWidth() : 800);
            Image img = icon.getImage().getScaledInstance(w, 180, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(img));
            imageLabel.setText("");
        } catch (Exception e) {
            imageLabel.setText("Banner not available");
            imageLabel.setIcon(null);
        }
    }

    private ImageIcon resolveImage(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return null;

        String path = rawPath.trim().replace("\\", "/");
        String fileName = new File(path).getName();

        if (path.startsWith("/")) {
            URL url = getClass().getResource(path);
            if (url != null) return new ImageIcon(url);
        }

        if (path.startsWith("resources/")) {
            URL url = getClass().getResource("/" + path);
            if (url != null) return new ImageIcon(url);
        }

        if (!path.startsWith("/") && !path.startsWith("http")) {
            URL url = getClass().getResource("/resources/banners/" + path);
            if (url != null) return new ImageIcon(url);
        }

        URL fromName = getClass().getResource("/resources/banners/" + fileName);
        if (fromName != null) return new ImageIcon(fromName);

        File f = new File(path);
        if (f.exists()) {
            return new ImageIcon(f.getAbsolutePath());
        }

        if (!Objects.equals(fileName, path)) {
            File byName = new File("src/resources/banners/" + fileName);
            if (byName.exists()) return new ImageIcon(byName.getAbsolutePath());
        }
        return null;
    }
}
