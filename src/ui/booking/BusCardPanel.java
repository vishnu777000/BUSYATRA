package ui.booking;

import config.UIConfig;

import javax.swing.*;
import java.awt.*;

public class BusCardPanel extends JPanel {

public BusCardPanel(
        int scheduleId,
        String operator,
        String busType,
        String route,
        String departure,
        String arrival,
        int seats,
        double fare,
        Runnable onSelect
){

    setLayout(new BorderLayout(15,15));
    setBackground(Color.WHITE);
    setBorder(BorderFactory.createEmptyBorder(15,15,15,15));

    UIConfig.styleCard(this);

    /* ================= LEFT ================= */

    JPanel left = new JPanel();
    left.setOpaque(false);
    left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));

    JLabel op = new JLabel(operator + " (" + busType + ")");
    op.setFont(new Font("Segoe UI",Font.BOLD,16));

    JLabel routeLbl = new JLabel(route);
    routeLbl.setFont(UIConfig.FONT_SMALL);
    routeLbl.setForeground(UIConfig.TEXT_LIGHT);

    left.add(op);
    left.add(Box.createVerticalStrut(5));
    left.add(routeLbl);

    /* ================= CENTER ================= */

    JPanel center = new JPanel(new GridLayout(2,1));
    center.setOpaque(false);

    JLabel time = new JLabel(departure + " → " + arrival);
    time.setFont(UIConfig.FONT_SUBTITLE);

    JLabel seatsLbl = new JLabel("Seats Available: " + seats);
    seatsLbl.setFont(UIConfig.FONT_SMALL);
    seatsLbl.setForeground(UIConfig.TEXT_LIGHT);

    center.add(time);
    center.add(seatsLbl);

    /* ================= RIGHT ================= */

    JPanel right = new JPanel();
    right.setOpaque(false);
    right.setLayout(new BoxLayout(right,BoxLayout.Y_AXIS));

    JLabel price = new JLabel("₹ " + fare);
    price.setFont(new Font("Segoe UI",Font.BOLD,18));
    price.setForeground(UIConfig.SUCCESS);
    price.setAlignmentX(Component.CENTER_ALIGNMENT);

    JButton selectBtn = new JButton("Select");
    selectBtn.setPreferredSize(new Dimension(100,35));
    UIConfig.primaryBtn(selectBtn);

    selectBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

    selectBtn.addActionListener(e -> {
        if(onSelect != null) onSelect.run();
    });

    right.add(price);
    right.add(Box.createVerticalStrut(10));
    right.add(selectBtn);

    /* ================= ADD ================= */

    add(left,BorderLayout.WEST);
    add(center,BorderLayout.CENTER);
    add(right,BorderLayout.EAST);
}

}
