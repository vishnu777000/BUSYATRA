package ui.booking;

import config.UIConfig;
import ui.common.MainFrame;
import util.BookingContext;
import util.Refreshable;

import javax.swing.*;
import java.awt.*;
import java.util.stream.Collectors;

public class BookingSummaryPanel extends JPanel implements Refreshable {

private final MainFrame frame;

private JLabel tripLabel;
private JLabel passengerLabel;
private JLabel seatsLabel;
private JLabel amountLabel;

public BookingSummaryPanel(MainFrame frame){

    this.frame = frame;

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(),BorderLayout.NORTH);
    add(center(),BorderLayout.CENTER);
    add(actions(),BorderLayout.SOUTH);
}

/* ================= HEADER ================= */

private JComponent header(){

    JPanel p = new JPanel();
    p.setOpaque(false);
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));

    JLabel success = new JLabel("Booking Confirmed ✅");
    success.setFont(new Font("Segoe UI",Font.BOLD,24));
    success.setForeground(UIConfig.SUCCESS);
    success.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel sub = new JLabel("Your ticket is ready");
    sub.setFont(UIConfig.FONT_SMALL);
    sub.setForeground(UIConfig.TEXT_LIGHT);
    sub.setAlignmentX(Component.CENTER_ALIGNMENT);

    p.add(success);
    p.add(Box.createVerticalStrut(5));
    p.add(sub);

    return p;
}

/* ================= CENTER ================= */

private JComponent center(){

    JPanel wrapper = new JPanel(new GridBagLayout());
    wrapper.setOpaque(false);

    JPanel card = new JPanel();
    card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
    card.setPreferredSize(new Dimension(420,300));

    UIConfig.styleCard(card);

    tripLabel = label(UIConfig.FONT_SUBTITLE);

    passengerLabel = label(UIConfig.FONT_SMALL);
    seatsLabel = label(UIConfig.FONT_SMALL);

    amountLabel = new JLabel();
    amountLabel.setFont(new Font("Segoe UI",Font.BOLD,26));
    amountLabel.setForeground(UIConfig.SUCCESS);

    card.add(tripLabel);
    card.add(Box.createVerticalStrut(12));

    card.add(passengerLabel);
    card.add(Box.createVerticalStrut(10));

    card.add(seatsLabel);
    card.add(Box.createVerticalStrut(12));

    card.add(new JSeparator());
    card.add(Box.createVerticalStrut(12));

    card.add(amountLabel);
    card.add(Box.createVerticalStrut(15));

    card.add(new RouteTimelinePanel(BookingContext.routeId));

    wrapper.add(card);

    return wrapper;
}

private JLabel label(Font f){

    JLabel l = new JLabel();
    l.setFont(f);
    return l;
}

/* ================= ACTIONS ================= */

private JComponent actions(){

    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,10));
    panel.setOpaque(false);

    JButton viewBtn = new JButton("View Ticket");
    viewBtn.setPreferredSize(new Dimension(200,42));

    UIConfig.primaryBtn(viewBtn);

    viewBtn.addActionListener(e -> {

        BookingContext.clearAfterPreview();
        frame.showScreen(MainFrame.SCREEN_TICKET_PREVIEW);
    });

    panel.add(viewBtn);

    return panel;
}

/* ================= REFRESH ================= */

@Override
public void refreshData(){

    tripLabel.setText(
            BookingContext.fromStop + " → " + BookingContext.toStop
    );

    passengerLabel.setText(
            "Passenger : " +
            BookingContext.passengerName +
            " (" + BookingContext.passengerPhone + ")"
    );

    seatsLabel.setText(
            "Seats : " +
            BookingContext.copySelectedSeats()
                    .stream()
                    .sorted()
                    .collect(Collectors.joining(", "))
    );

    amountLabel.setText(
            "Total Paid : ₹ " + BookingContext.getFinalAmount()
    );
}

}
