package util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IconUtil
 * Fast icon loader with caching
 */
public class IconUtil {

    private static final Map<String, ImageIcon> CACHE = new HashMap<>();
    private static final Set<String> MISSING_LOGGED = new HashSet<>();
    private static final List<String> ICON_FOLDERS = Arrays.asList(
            "/resources/icons/",
            "/resources/icons/sidebar/",
            "/resources/icons/actions/",
            "/resources/icons/extra/",
            "/resources/icons/app/",
            "/resources/icons/ticket/",
            "/resources/icons/status/"
    );
    private static final Map<String, String> ICON_ALIASES = new HashMap<>();

    static {
        ICON_ALIASES.put("notification.png", "bell.png");
        ICON_ALIASES.put("history.png", "ticket.png");
        ICON_ALIASES.put("bus.png", "buslogo.png");
        ICON_ALIASES.put("book.png", "ticket.png");
        ICON_ALIASES.put("cancel.png", "support.png");
        ICON_ALIASES.put("printing.png", "download.png");
        ICON_ALIASES.put("route.png", "map.png");
        ICON_ALIASES.put("schedule.png", "calendar.png");
    }

    private IconUtil(){}

    /* ================= ICONS ================= */

    public static ImageIcon load(String iconName,int width,int height){

        return loadIcon(iconName, width, height);
    }

    public static void preloadCommonIcons() {
        String[] icons = {
                "buslogo.png", "menu.png", "user.png", "search.png", "home.png",
                "ticket.png", "wallet.png", "settings.png", "support.png",
                "calendar.png", "clock.png", "seat.png", "payment.png",
                "map.png", "download.png", "loading.gif"
        };
        for (String icon : icons) {
            load(icon, 20, 20);
            load(icon, 28, 28);
        }
    }

    public static ImageIcon loadTinted(String iconName, int width, int height, Color tint) {
        if (tint == null) {
            return load(iconName, width, height);
        }

        String key = "tinted|" + iconName + "|" + width + "x" + height + "|" + tint.getRGB();
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }

        ImageIcon base = load(iconName, width, height);
        Image src = base.getImage();

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(src, 0, 0, width, height, null);
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.setColor(tint);
        g2.fillRect(0, 0, width, height);
        g2.dispose();

        ImageIcon tinted = new ImageIcon(out);
        CACHE.put(key, tinted);
        return tinted;
    }

    /* ================= BANNERS ================= */

    public static ImageIcon loadBanner(String banner,int width,int height){

        return loadFrom("/resources/banners/",banner,width,height);
    }

    /* ================= CORE LOADER ================= */

    private static ImageIcon loadFrom(String folder,String name,int width,int height){

        String key = folder + name + "_" + width + "x" + height;

        if(CACHE.containsKey(key)){
            return CACHE.get(key);
        }

        try{

            String path = folder + name;
            URL url = IconUtil.class.getResource(path);

            if(url == null){
                logMissing(path);

                ImageIcon icon = placeholder(width,height);
                CACHE.put(key,icon);

                return icon;
            }

            Image img = new ImageIcon(url)
                    .getImage()
                    .getScaledInstance(
                            width,
                            height,
                            Image.SCALE_SMOOTH
                    );

            ImageIcon icon = new ImageIcon(img);

            CACHE.put(key,icon);

            return icon;

        }
        catch(Exception e){

            e.printStackTrace();

            ImageIcon icon = placeholder(width,height);
            CACHE.put(key,icon);

            return icon;
        }
    }

    private static ImageIcon loadIcon(String iconName, int width, int height) {

        String normalized = iconName == null ? "" : iconName.trim();
        String aliased = ICON_ALIASES.getOrDefault(normalized, normalized);

        ImageIcon icon = tryFolders(normalized, width, height);
        if (icon != null) return icon;

        if (!aliased.equals(normalized)) {
            icon = tryFolders(aliased, width, height);
            if (icon != null) return icon;
        }

        String key = "/resources/icons/" + normalized + "_" + width + "x" + height;
        if (!CACHE.containsKey(key)) {
            logMissing("/resources/icons/" + normalized);
            CACHE.put(key, placeholder(width, height));
        }

        return CACHE.get(key);
    }

    private static ImageIcon tryFolders(String name, int width, int height) {
        for (String folder : ICON_FOLDERS) {
            String path = folder + name;
            URL url = IconUtil.class.getResource(path);
            if (url != null) {
                return loadFrom(folder, name, width, height);
            }
        }
        return null;
    }

    /* ================= FALLBACK ================= */

    private static ImageIcon placeholder(int w,int h){

        BufferedImage img =
                new BufferedImage(
                        w,
                        h,
                        BufferedImage.TYPE_INT_ARGB
                );

        return new ImageIcon(img);
    }

    private static void logMissing(String path) {
        synchronized (MISSING_LOGGED) {
            if (MISSING_LOGGED.add(path)) {
                System.err.println("[IconUtil] Missing resource: " + path);
            }
        }
    }

}
