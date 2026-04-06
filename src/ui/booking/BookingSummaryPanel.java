package ui.booking;

import config.UIConfig;
import ui.common.MainFrame;
import util.BookingContext;
import util.Refreshable;

import javax.swing.*;
import java.awt.*;

public class BookingSummaryPanel extends JPanel implements Refreshable {

    private final MainFrame frame;

    private JLabel tripLabel;
    private JLabel passengerLabel;
    private JLabel seatsLabel;
    private JLabel ticketIdsLabel;
    private JLabel amountLabel;

    public BookingSummaryPanel(MainFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);
    }

    private JComponent header() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel success = new JLabel("Booking Confirmed");
        success.setFont(new Font("Segoe UI", Font.BOLD, 24));
        success.setForeground(UIConfig.SUCCESS);
        success.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Your ticket is ready");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(UIConfig.TEXT_LIGHT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(success);
        panel.add(Box.createVerticalStrut(5));
        panel.add(sub);

        return panel;
    }

    private JComponent center() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 320));
        UIConfig.styleCard(card);

        tripLabel = label(UIConfig.FONT_SUBTITLE);
        passengerLabel = label(UIConfig.FONT_SMALL);
        seatsLabel = label(UIConfig.FONT_SMALL);
        ticketIdsLabel = label(UIConfig.FONT_SMALL);

        amountLabel = new JLabel();
        amountLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        amountLabel.setForeground(UIConfig.SUCCESS);

        card.add(tripLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(passengerLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(seatsLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(ticketIdsLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(new JSeparator());
        card.add(Box.createVerticalStrut(12));
        card.add(amountLabel);
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

    private JComponent actions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        panel.setOpaque(false);

        JButton viewBtn = new JButton("View Ticket");
        viewBtn.setPreferredSize(new Dimension(200, 42));
        UIConfig.primaryBtn(viewBtn);
        viewBtn.addActionListener(e -> frame.showScreen(MainFrame.SCREEN_TICKET_PREVIEW));

        panel.add(viewBtn);
        return panel;
    }

    @Override
    public void refreshData() {
        tripLabel.setText(BookingContext.fromStop + " -> " + BookingContext.toStop);
        passengerLabel.setText(
                "Passenger : " +
                BookingContext.passengerName +
                " (" + BookingContext.passengerPhone + ")"
        );
        seatsLabel.setText("Seats : " + BookingContext.getSeatListString());

        int ticketCount = BookingContext.getRecentTicketCount();
        if (ticketCount > 1) {
            ticketIdsLabel.setText("Ticket IDs : " + BookingContext.getTicketIdListString());
        } else if (BookingContext.getPrimaryTicketId() > 0) {
            ticketIdsLabel.setText("Ticket ID : " + BookingContext.getPrimaryTicketId());
        } else {
            ticketIdsLabel.setText("Ticket ID : Pending");
        }

        amountLabel.setText("Total Paid : INR " + String.format("%.2f", BookingContext.getFinalAmount()));
    }
}
