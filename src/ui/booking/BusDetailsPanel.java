package ui.booking;

import config.UIConfig;
import dao.ScheduleDAO;
import ui.common.MainFrame;
import util.BookingContext;

import javax.swing.*;
import java.awt.*;

public class BusDetailsPanel extends JPanel implements util.Refreshable {

    private final MainFrame frame;

    private JLabel tripLabel;
    private JLabel operatorLabel;
    private JLabel busTypeLabel;
    private JLabel timeLabel;
    private JLabel fareLabel;
    private JLabel driverPhoneLabel;

    public BusDetailsPanel(MainFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);
    }

    private JComponent header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton back = new JButton("< Back");
        UIConfig.secondaryBtn(back);
        back.setPreferredSize(new Dimension(120, 35));
        back.addActionListener(e -> frame.goBack());

        JLabel title = new JLabel("Bus Details", SwingConstants.CENTER);
        title.setFont(UIConfig.FONT_TITLE);

        panel.add(back, BorderLayout.WEST);
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }

    private JComponent center() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 320));
        UIConfig.styleCard(card);

        tripLabel = centerLabel(UIConfig.FONT_SUBTITLE);
        operatorLabel = label(UIConfig.FONT_NORMAL);
        busTypeLabel = label(UIConfig.FONT_SMALL);
        timeLabel = label(UIConfig.FONT_NORMAL);

        fareLabel = new JLabel();
        fareLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        fareLabel.setForeground(UIConfig.SUCCESS);

        driverPhoneLabel = label(UIConfig.FONT_SMALL);

        card.add(tripLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(operatorLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(busTypeLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(timeLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(new JSeparator());
        card.add(Box.createVerticalStrut(10));
        card.add(fareLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(driverPhoneLabel);
        card.add(Box.createVerticalStrut(15));
        card.add(new RouteTimelinePanel(
                BookingContext.routeId,
                BookingContext.fromStop,
                BookingContext.toStop
        ));

        wrapper.add(card);
        return wrapper;
    }

    private JLabel label(Font font) {
        JLabel label = new JLabel();
        label.setFont(font);
        return label;
    }

    private JLabel centerLabel(Font font) {
        JLabel label = new JLabel();
        label.setFont(font);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private JComponent actions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        panel.setOpaque(false);

        JButton mapBtn = new JButton("View Route Map");
        mapBtn.setPreferredSize(new Dimension(170, 40));
        UIConfig.secondaryBtn(mapBtn);
        mapBtn.addActionListener(e -> openRouteMapDialog());

        JButton select = new JButton("Select Seats");
        select.setPreferredSize(new Dimension(180, 40));
        UIConfig.primaryBtn(select);
        select.addActionListener(e -> frame.showScreen(MainFrame.SCREEN_SEATS));

        panel.add(mapBtn);
        panel.add(select);
        return panel;
    }

    private void openRouteMapDialog() {
        JDialog dialog = new JDialog(frame, "Route Map", true);
        dialog.setSize(620, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new RouteMapPanel(BookingContext.routeId), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    @Override
    public void refreshData() {
        tripLabel.setText(BookingContext.fromStop + " -> " + BookingContext.toStop);
        operatorLabel.setText("Loading operator details...");
        busTypeLabel.setText(" ");
        timeLabel.setText("Loading timings...");
        fareLabel.setText("Fare : Rs. " + BookingContext.farePerSeat);
        driverPhoneLabel.setText("Loading driver contact...");

        SwingWorker<String[], Void> worker = new SwingWorker<>() {
            @Override
            protected String[] doInBackground() {
                return new ScheduleDAO().getScheduleDetails(BookingContext.scheduleId);
            }

            @Override
            protected void done() {
                try {
                    String[] data = get();
                    if (data == null) {
                        operatorLabel.setText("Operator details unavailable");
                        busTypeLabel.setText("Bus Type : -");
                        timeLabel.setText("Departure : -");
                        driverPhoneLabel.setText("Driver Phone : -");
                        return;
                    }

                    String operator = data[1];
                    String busType = data[2];
                    String driverPhone = data[3];
                    String departure = data[4];
                    String arrival = data[5];

                    operatorLabel.setText("Operator : " + operator);
                    busTypeLabel.setText("Bus Type : " + busType);
                    timeLabel.setText("Departure : " + departure + " -> Arrival : " + arrival);
                    driverPhoneLabel.setText("Driver Phone : " + driverPhone);
                } catch (Exception e) {
                    operatorLabel.setText("Operator details unavailable");
                    busTypeLabel.setText("Bus Type : -");
                    timeLabel.setText("Departure : -");
                    driverPhoneLabel.setText("Driver Phone : -");
                }
            }
        };
        worker.execute();
    }
}
