package ui.common;

import config.UITheme;

import javax.swing.*;
import java.awt.*;

public class StatsCard extends JPanel {

    private JLabel valueLabel;

    public StatsCard(String title, String value) {

        setLayout(new BorderLayout());
        setBackground(UITheme.CARD);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);

        valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
    }

    public void setValue(String val) {
        valueLabel.setText(val);
    }
}