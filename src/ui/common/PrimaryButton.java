package ui.common;

import config.UITheme;
import util.HoverEffect;

import javax.swing.*;
import java.awt.*;

public class PrimaryButton extends JButton {

    public PrimaryButton(String text) {
        super(text);

        setFocusPainted(false);
        setForeground(Color.WHITE);
        setBackground(UITheme.PRIMARY);
        setFont(UITheme.FONT_BOLD);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        HoverEffect.applyButtonHover(this, UITheme.PRIMARY, UITheme.PRIMARY_HOVER);
    }
}