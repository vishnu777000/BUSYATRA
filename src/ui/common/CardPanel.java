package ui.common;

import config.UIConfig;

import javax.swing.*;
import java.awt.*;

public class CardPanel extends JPanel {

    public CardPanel() {
        init();
    }

    public CardPanel(LayoutManager layout) {
        super(layout);
        init();
    }

    private void init() {

        setLayout(new BorderLayout());
        setOpaque(true);

        UIConfig.styleCard(this);
    }

    

    public void addPadding(int top, int left, int bottom, int right) {
        setBorder(BorderFactory.createCompoundBorder(
                getBorder(),
                BorderFactory.createEmptyBorder(top, left, bottom, right)
        ));
    }

    

    public JLabel setTitle(String text) {

        JLabel title = new JLabel(text);
        title.setFont(UIConfig.FONT_SUBTITLE);

        add(title, BorderLayout.NORTH);

        return title;
    }

    

    public void setFooter(JComponent comp) {
        add(comp, BorderLayout.SOUTH);
    }
}