package ui.clerk;

import config.UIConfig;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClerkStatusBar extends JPanel {

    private final JLabel timeLabel;
    private final JLabel statusLabel;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy | HH:mm:ss");

    public ClerkStatusBar() {

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 32));
        setBackground(UIConfig.CARD);
        setBorder(
                BorderFactory.createMatteBorder(
                        1, 0, 0, 0, UIConfig.BORDER)
        );

        /* ================= LEFT STATUS ================= */
        statusLabel = new JLabel(" DB: Connected  |  Mode: ONLINE ");
        statusLabel.setFont(UIConfig.FONT_SMALL);
        statusLabel.setForeground(UIConfig.TEXT);

        /* ================= RIGHT CLOCK ================= */
        timeLabel = new JLabel();
        timeLabel.setFont(UIConfig.FONT_SMALL);
        timeLabel.setForeground(UIConfig.TEXT);

        updateTime();

        // ⏱ live clock (Swing-safe)
        Timer timer = new Timer(1000, e -> updateTime());
        timer.setRepeats(true);
        timer.start();

        add(statusLabel, BorderLayout.WEST);
        add(timeLabel, BorderLayout.EAST);
    }

    /* ================= TIME ================= */
    private void updateTime() {
        timeLabel.setText(
                LocalDateTime.now().format(TIME_FMT) + "  "
        );
    }

    /* ================= STATUS CONTROL (READY) ================= */

    // call this if DB disconnects
    public void setDbDisconnected() {
        statusLabel.setText(" DB: Disconnected  |  Mode: OFFLINE ");
        statusLabel.setForeground(UIConfig.DANGER);
    }

    // call this after reconnect
    public void setDbConnected() {
        statusLabel.setText(" DB: Connected  |  Mode: ONLINE ");
        statusLabel.setForeground(UIConfig.SUCCESS);
    }
}
