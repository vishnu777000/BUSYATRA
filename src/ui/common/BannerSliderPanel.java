package ui.common;

import dao.BannerDAO;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BannerSliderPanel extends JPanel {

    private List<String> images;
    private JLabel imageLabel;
    private int index = 0;

    public BannerSliderPanel() {

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(800, 180));

        imageLabel = new JLabel();
        add(imageLabel, BorderLayout.CENTER);

        loadImages();
        startSlider();
    }

    private void loadImages() {
        images = new BannerDAO().getHomepageBannerImages();
        if (!images.isEmpty()) {
            setImage(images.get(0));
        }
    }

    private void startSlider() {

        Timer timer = new Timer(3000, e -> {
            if (images == null || images.isEmpty()) return;

            index = (index + 1) % images.size();
            setImage(images.get(index));
        });

        timer.start();
    }

    private void setImage(String path) {
        try {
            ImageIcon icon = new ImageIcon(path);
            Image img = icon.getImage().getScaledInstance(
                    getWidth() > 0 ? getWidth() : 800,
                    180,
                    Image.SCALE_SMOOTH
            );
            imageLabel.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            imageLabel.setText("Banner not available");
        }
    }
}