package config;

import java.awt.*;

public class UITheme {

    /* ================= COLORS ================= */

    public static final Color BG = new Color(245, 247, 250);
    public static final Color CARD = Color.WHITE;

    public static final Color PRIMARY = new Color(220, 53, 69);
    public static final Color PRIMARY_HOVER = new Color(200, 40, 60);

    public static final Color SUCCESS = new Color(40, 167, 69);
    public static final Color WARNING = new Color(255, 193, 7);
    public static final Color ERROR = new Color(220, 53, 69);
    public static final Color INFO = new Color(23, 162, 184);

    public static final Color TEXT = new Color(40, 40, 40);
    public static final Color TEXT_SECONDARY = new Color(120, 120, 120);

    public static final Color BORDER = new Color(230, 230, 230);

    /* ================= SEAT COLORS ================= */

    public static final Color SEAT_AVAILABLE = new Color(40, 167, 69);
    public static final Color SEAT_BOOKED = new Color(220, 53, 69);
    public static final Color SEAT_LOCKED = new Color(255, 193, 7);

    /* ================= FONTS ================= */

    public static final Font FONT_TITLE =
            new Font("Segoe UI", Font.BOLD, 18);

    public static final Font FONT_SUBTITLE =
            new Font("Segoe UI", Font.BOLD, 15);

    public static final Font FONT_BOLD =
            new Font("Segoe UI", Font.BOLD, 13);

    public static final Font FONT_PLAIN =
            new Font("Segoe UI", Font.PLAIN, 13);

    public static final Font FONT_SMALL =
            new Font("Segoe UI", Font.PLAIN, 12);

    /* ================= SPACING ================= */

    public static final int PADDING = 15;
    public static final int GAP = 15;

    /* ================= DIMENSIONS ================= */

    public static final int SIDEBAR_WIDTH = 220;
    public static final int HEADER_HEIGHT = 60;

}
