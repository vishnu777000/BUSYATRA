package ui.booking;

import config.UIConfig;
import dao.SearchDAO;
import ui.common.MainFrame;
import util.BookingContext;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BusListPanel extends JPanel implements util.Refreshable {

    private final MainFrame frame;
    private JPanel resultsPanel;
    private volatile long refreshToken = 0L;

    public BusListPanel(MainFrame frame) {

        this.frame = frame;

        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(results(), BorderLayout.CENTER);
    }

    private JComponent header() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton back = new JButton("<- Back");
        UIConfig.secondaryBtn(back);
        back.setPreferredSize(new Dimension(120, 35));

        back.addActionListener(e -> frame.goBack());

        JLabel title = new JLabel("Available Buses", SwingConstants.CENTER);
        title.setFont(UIConfig.FONT_TITLE);

        panel.add(back, BorderLayout.WEST);
        panel.add(title, BorderLayout.CENTER);

        return panel;
    }

    private JComponent results() {

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    @Override
    public void refreshData() {

        long token = ++refreshToken;

        resultsPanel.removeAll();
        resultsPanel.add(message("Loading buses..."));
        resultsPanel.revalidate();
        resultsPanel.repaint();

        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                return new SearchDAO().searchSchedules(
                        BookingContext.fromStop,
                        BookingContext.toStop,
                        BookingContext.journeyDate
                );
            }

            @Override
            protected void done() {
                if (token != refreshToken) {
                    return;
                }

                resultsPanel.removeAll();

                try {
                    List<String[]> list = get();

                    if (list == null || list.isEmpty()) {
                        resultsPanel.add(message("No buses available"));
                    } else {
                        for (String[] r : list) {
                            int scheduleId = Integer.parseInt(r[0]);
                            int routeId = Integer.parseInt(r[1]);

                            String operator = r[2];
                            String busType = r[3];
                            double fare = Double.parseDouble(r[4]);
                            String dep = r[5];
                            String arr = r[6];
                            int seats = Integer.parseInt(r[9]);

                            BusCardPanel card = new BusCardPanel(
                                    scheduleId,
                                    operator,
                                    busType,
                                    BookingContext.fromStop + " -> " + BookingContext.toStop,
                                    dep,
                                    arr,
                                    seats,
                                    fare,
                                    () -> {
                                        BookingContext.scheduleId = scheduleId;
                                        BookingContext.routeId = routeId;
                                        BookingContext.farePerSeat = fare;

                                        frame.showScreen(MainFrame.SCREEN_BUS_DETAILS);
                                    }
                            );

                            resultsPanel.add(card);
                            resultsPanel.add(Box.createVerticalStrut(15));
                        }
                    }
                } catch (Exception e) {
                    resultsPanel.add(message("Failed to load buses"));
                }

                resultsPanel.revalidate();
                resultsPanel.repaint();
            }
        };

        worker.execute();
    }

    private JComponent message(String text) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(text);
        label.setFont(UIConfig.FONT_SUBTITLE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalStrut(50));
        panel.add(label);
        return panel;
    }
}
