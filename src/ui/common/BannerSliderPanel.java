package ui.common;

import dao.BannerDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BannerSliderPanel extends JPanel {

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 180;
    private static final int MAX_RENDER_WIDTH = 980;
    private static final long RELOAD_INTERVAL_MS = 45_000L;
    private static final int CARD_PADDING = 8;

    private List<String> images = new ArrayList<>();
    private final JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
    private final Map<String, ImageIcon> sourceCache = new ConcurrentHashMap<>();
    private final Map<String, ImageIcon> scaledCache = new ConcurrentHashMap<>();
    private int index = 0;
    private final Timer sliderTimer;
    private long lastReloadMs = 0L;
    private boolean remoteLoadAttempted = false;
    private volatile boolean loadingRemote = false;

    public BannerSliderPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setMinimumSize(new Dimension(420, DEFAULT_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, DEFAULT_HEIGHT));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(233, 236, 241)),
                BorderFactory.createEmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING)
        ));

        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setForeground(new Color(100, 116, 139));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(new Color(248, 250, 252));
        imageLabel.setText("Loading banners...");
        card.add(imageLabel, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        sliderTimer = new Timer(3500, e -> advanceSlide());
        sliderTimer.start();

        SwingUtilities.invokeLater(this::reloadBanners);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (!images.isEmpty()) {
                    scaledCache.clear();
                    int safeIndex = Math.max(0, Math.min(index, images.size() - 1));
                    showImageAt(safeIndex);
                }
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!sliderTimer.isRunning()) {
            sliderTimer.start();
        }
    }

    @Override
    public void removeNotify() {
        if (sliderTimer.isRunning()) {
            sliderTimer.stop();
        }
        super.removeNotify();
    }

    public void reloadBanners() {
        long now = System.currentTimeMillis();
        if (loadingRemote) return;
        if (remoteLoadAttempted && !images.isEmpty() && now - lastReloadMs < RELOAD_INTERVAL_MS) return;

        loadingRemote = true;
        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                List<String> loaded = new BannerDAO().getHomepageBannerImages();
                if (loaded == null) {
                    loaded = new ArrayList<>();
                }
                loaded.removeIf(path -> path == null || path.isBlank());
                List<String> finalImages = loaded.isEmpty() ? bundledImages() : loaded;
                finalImages = retainResolvableImages(finalImages);
                if (finalImages.isEmpty() && !images.isEmpty()) {
                    finalImages = new ArrayList<>(images);
                }
                preloadImages(finalImages);
                return finalImages;
            }

            @Override
            protected void done() {
                try {
                    applyImages(get(), "No active banners");
                } catch (Exception ignored) {
                    if (images.isEmpty()) {
                        applyImages(bundledImages(), "No active banners");
                    }
                } finally {
                    lastReloadMs = System.currentTimeMillis();
                    remoteLoadAttempted = true;
                    loadingRemote = false;
                }
            }
        };
        worker.execute();
    }

    private void advanceSlide() {
        if (System.currentTimeMillis() - lastReloadMs > RELOAD_INTERVAL_MS) {
            reloadBanners();
        }
        if (images.isEmpty()) return;

        showImageAt(index + 1);
    }

    private void applyImages(List<String> loaded, String emptyMessage) {
        Set<String> uniqueImages = new LinkedHashSet<>();
        if (loaded != null) {
            for (String image : loaded) {
                if (image != null && !image.isBlank()) {
                    uniqueImages.add(normalizePath(image));
                }
            }
        }

        images = new ArrayList<>(uniqueImages);
        lastReloadMs = System.currentTimeMillis();
        scaledCache.clear();

        if (!images.isEmpty() && showImageAt(0)) return;

        imageLabel.setText(emptyMessage);
        imageLabel.setIcon(null);
    }

    private List<String> bundledImages() {
        Set<String> defaults = new LinkedHashSet<>();

        File dir = new File("src/resources/banners");
        File[] files = dir.listFiles((parent, name) -> {
            String lower = name == null ? "" : name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        });

        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File file : files) {
                defaults.add(file.getAbsolutePath().replace("\\", "/"));
            }
        }

        for (int i = 1; i <= 10; i++) {
            String name = "banner" + i + ".png";
            URL url = getClass().getResource("/resources/banners/" + name);
            if (url != null) {
                defaults.add(name);
            }
        }

        return new ArrayList<>(defaults);
    }

    private boolean showImageAt(int requestedIndex) {
        if (images.isEmpty()) {
            imageLabel.setText("No active banners");
            imageLabel.setIcon(null);
            return false;
        }

        int size = images.size();
        int startIndex = Math.floorMod(requestedIndex, size);
        for (int offset = 0; offset < size; offset++) {
            int candidateIndex = (startIndex + offset) % size;
            if (setImage(images.get(candidateIndex))) {
                index = candidateIndex;
                return true;
            }
        }

        imageLabel.setText("No active banners");
        imageLabel.setIcon(null);
        return false;
    }

    private boolean setImage(String rawPath) {
        try {
            String path = normalizePath(rawPath);
            int width = targetRenderWidth();
            int height = DEFAULT_HEIGHT - (CARD_PADDING * 2);
            String cacheKey = cacheKey(path, width, height);

            ImageIcon icon = scaledCache.get(cacheKey);
            if (icon == null) {
                ImageIcon original = resolveImage(path);
                if (original == null || original.getIconWidth() <= 0) {
                    imageLabel.setText("Banner not available");
                    imageLabel.setIcon(null);
                    return false;
                }

                icon = scaleToFit(original, width, height);
                if (icon != null) {
                    scaledCache.put(cacheKey, icon);
                }
            }

            if (icon == null || icon.getIconWidth() <= 0) {
                imageLabel.setText("Banner not available");
                imageLabel.setIcon(null);
                return false;
            }

            imageLabel.setIcon(icon);
            imageLabel.setText("");
            return true;
        } catch (Exception e) {
            imageLabel.setText("Banner not available");
            imageLabel.setIcon(null);
            return false;
        }
    }

    private ImageIcon resolveImage(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return null;

        String path = normalizePath(rawPath);
        ImageIcon cached = sourceCache.get(path);
        if (cached != null) {
            return cached;
        }

        ImageIcon loaded = loadImage(path);
        if (loaded != null && loaded.getIconWidth() > 0) {
            sourceCache.put(path, loaded);
        }
        return loaded;
    }

    private ImageIcon loadImage(String path) {
        String fileName = new File(path).getName();

        if (path.startsWith("file:")) {
            try {
                File file = new File(URI.create(path));
                if (file.exists()) return new ImageIcon(file.getAbsolutePath());
            } catch (Exception ignored) {
                
            }
        }

        if (path.startsWith("/")) {
            URL url = getClass().getResource(path);
            if (url != null) return new ImageIcon(url);
        }

        if (path.startsWith("resources/")) {
            URL url = getClass().getResource("/" + path);
            if (url != null) return new ImageIcon(url);
        }

        if (path.startsWith("src/")) {
            File sourceFile = new File(path);
            if (sourceFile.exists()) {
                return new ImageIcon(sourceFile.getAbsolutePath());
            }
        }

        if (!path.startsWith("/") && !path.startsWith("http")) {
            URL url = getClass().getResource("/resources/banners/" + path);
            if (url != null) return new ImageIcon(url);
        }

        URL fromName = getClass().getResource("/resources/banners/" + fileName);
        if (fromName != null) return new ImageIcon(fromName);

        File direct = new File(path);
        if (direct.exists()) {
            return new ImageIcon(direct.getAbsolutePath());
        }

        File projectRelative = new File(System.getProperty("user.dir"), path);
        if (projectRelative.exists()) {
            return new ImageIcon(projectRelative.getAbsolutePath());
        }

        if (!Objects.equals(fileName, path)) {
            File byName = new File("src/resources/banners/" + fileName);
            if (byName.exists()) return new ImageIcon(byName.getAbsolutePath());
        }

        return null;
    }

    private void preloadImages(List<String> paths) {
        if (paths == null) return;

        for (String rawPath : paths) {
            String path = normalizePath(rawPath);
            if (path.isBlank() || sourceCache.containsKey(path)) {
                continue;
            }

            ImageIcon icon = loadImage(path);
            if (icon != null && icon.getIconWidth() > 0) {
                sourceCache.put(path, icon);
            }
        }
    }

    private List<String> retainResolvableImages(List<String> paths) {
        List<String> validPaths = new ArrayList<>();
        if (paths == null) return validPaths;

        for (String rawPath : paths) {
            String path = normalizePath(rawPath);
            ImageIcon icon = resolveImage(path);
            if (icon != null && icon.getIconWidth() > 0) {
                validPaths.add(path);
            }
        }

        return validPaths;
    }

    private ImageIcon scaleToFit(ImageIcon source, int targetWidth, int targetHeight) {
        if (source == null || targetWidth <= 0 || targetHeight <= 0) return null;

        int sourceWidth = source.getIconWidth();
        int sourceHeight = source.getIconHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) return null;

        double scale = Math.min(
                targetWidth / (double) sourceWidth,
                targetHeight / (double) sourceHeight
        );
        scale = Math.min(scale, 1.0d);
        int drawWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int drawHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        int offsetX = (targetWidth - drawWidth) / 2;
        int offsetY = (targetHeight - drawHeight) / 2;

        BufferedImage canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(source.getImage(), offsetX, offsetY, drawWidth, drawHeight, null);
        g2.dispose();

        return new ImageIcon(canvas);
    }

    private int targetRenderWidth() {
        int available = getWidth() > 0 ? getWidth() - (CARD_PADDING * 2) : DEFAULT_WIDTH;
        available = Math.max(480, available);
        return Math.min(MAX_RENDER_WIDTH, available);
    }

    private String cacheKey(String path, int width, int height) {
        return path + "@" + width + "x" + height;
    }

    private String normalizePath(String rawPath) {
        String path = rawPath.trim().replace("\\", "/");
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        return path;
    }
}
