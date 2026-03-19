package util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * IconUtil
 * Fast icon loader with caching
 */
public class IconUtil {

    private static final Map<String, ImageIcon> CACHE = new HashMap<>();

    private IconUtil(){}

    /* ================= ICONS ================= */

    public static ImageIcon load(String iconName,int width,int height){

        return loadFrom("/resources/icons/",iconName,width,height);
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

                System.err.println("[IconUtil] Missing resource: " + path);

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

}