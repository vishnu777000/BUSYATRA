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

public BusDetailsPanel(MainFrame frame){

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

    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JButton back = new JButton("← Back");
    UIConfig.secondaryBtn(back);
    back.setPreferredSize(new Dimension(120,35));

    back.addActionListener(e -> frame.goBack());

    JLabel title = new JLabel("Bus Details",SwingConstants.CENTER);
    title.setFont(UIConfig.FONT_TITLE);

    panel.add(back,BorderLayout.WEST);
    panel.add(title,BorderLayout.CENTER);

    return panel;
}

/* ================= CENTER ================= */

private JComponent center(){

    JPanel wrapper = new JPanel(new GridBagLayout());
    wrapper.setOpaque(false);

    JPanel card = new JPanel();
    card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
    card.setPreferredSize(new Dimension(420,320));

    UIConfig.styleCard(card);

    tripLabel = centerLabel(UIConfig.FONT_SUBTITLE);

    operatorLabel = label(UIConfig.FONT_NORMAL);
    busTypeLabel = label(UIConfig.FONT_SMALL);
    timeLabel = label(UIConfig.FONT_NORMAL);

    fareLabel = new JLabel();
    fareLabel.setFont(new Font("Segoe UI",Font.BOLD,22));
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

    card.add(new RouteTimelinePanel(BookingContext.routeId));

    wrapper.add(card);

    return wrapper;
}

private JLabel label(Font f){
    JLabel l = new JLabel();
    l.setFont(f);
    return l;
}

private JLabel centerLabel(Font f){
    JLabel l = new JLabel();
    l.setFont(f);
    l.setAlignmentX(Component.CENTER_ALIGNMENT);
    l.setHorizontalAlignment(SwingConstants.CENTER);
    return l;
}

/* ================= ACTIONS ================= */

private JComponent actions(){

    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,10));
    panel.setOpaque(false);

    JButton select = new JButton("Select Seats");
    select.setPreferredSize(new Dimension(180,40));

    UIConfig.primaryBtn(select);

    select.addActionListener(e ->
            frame.showScreen(MainFrame.SCREEN_SEATS)
    );

    panel.add(select);

    return panel;
}

/* ================= REFRESH ================= */

@Override
public void refreshData(){

    try{

        String[] data =
                new ScheduleDAO().getScheduleDetails(
                        BookingContext.scheduleId
                );

        if(data == null) return;

        String operator = data[1];
        String busType = data[2];
        String driverPhone = data[3];
        String departure = data[4];
        String arrival = data[5];

        tripLabel.setText(
                BookingContext.fromStop +
                " → " +
                BookingContext.toStop
        );

        operatorLabel.setText("Operator : " + operator);
        busTypeLabel.setText("Bus Type : " + busType);

        timeLabel.setText(
                "Departure : " + departure +
                " → Arrival : " + arrival
        );

        fareLabel.setText(
                "Fare : ₹ " + BookingContext.farePerSeat
        );

        driverPhoneLabel.setText(
                "Driver Phone : " + driverPhone
        );

    }catch(Exception e){
        e.printStackTrace();
    }
}

}
