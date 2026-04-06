package util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

public final class RouteMapImageUtil {

    private RouteMapImageUtil() {
    }

    public static ImageIcon resolve(String mapPath) {
        if (mapPath == null || mapPath.isBlank()) {
            return null;
        }

        String path = mapPath.trim().replace("\\", "/");

        ImageIcon icon = fromResourcePath(path);
        if (isValid(icon)) return icon;

        icon = fromRoutesFolder(path);
        if (isValid(icon)) return icon;

        icon = fromFilePath(path);
        if (isValid(icon)) return icon;

        return null;
    }

    public static ImageIcon scaleToFit(ImageIcon source, int maxWidth, int maxHeight) {
        if (!isValid(source) || maxWidth <= 0 || maxHeight <= 0) {
            return source;
        }

        int srcW = source.getIconWidth();
        int srcH = source.getIconHeight();
        double scale = Math.min((double) maxWidth / srcW, (double) maxHeight / srcH);
        if (scale <= 0) {
            return source;
        }

        int targetW = Math.max(1, (int) Math.round(srcW * scale));
        int targetH = Math.max(1, (int) Math.round(srcH * scale));

        BufferedImage canvas = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(source.getImage(), 0, 0, targetW, targetH, null);
        g2.dispose();
        return new ImageIcon(canvas);
    }

    private static ImageIcon fromResourcePath(String path) {
        if (path.startsWith("/")) {
            URL url = RouteMapImageUtil.class.getResource(path);
            if (url != null) return new ImageIcon(url);
        }

        if (path.startsWith("resources/")) {
            URL url = RouteMapImageUtil.class.getResource("/" + path);
            if (url != null) return new ImageIcon(url);
        }

        return null;
    }

    private static ImageIcon fromRoutesFolder(String path) {
        String fileName = new File(path).getName();

        URL classpathUrl = RouteMapImageUtil.class.getResource("/resources/routes/" + fileName);
        if (classpathUrl != null) return new ImageIcon(classpathUrl);

        File srcRoutesFile = new File("src/resources/routes/" + fileName);
        if (srcRoutesFile.exists()) return new ImageIcon(srcRoutesFile.getAbsolutePath());

        File directSrcFile = new File("src/" + path);
        if (directSrcFile.exists()) return new ImageIcon(directSrcFile.getAbsolutePath());

        return null;
    }

    private static ImageIcon fromFilePath(String path) {
        File file = new File(path);
        if (file.exists()) {
            return new ImageIcon(file.getAbsolutePath());
        }
        return null;
    }

    private static boolean isValid(ImageIcon icon) {
        return icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0;
    }
}
