package ui.booking;

import config.UIConfig;
import ui.common.MainFrame;
import util.BookingContext;

import javax.swing.*;
import java.awt.*;

public class BookingConfirmationPanel extends JPanel {

private final MainFrame frame;

public BookingConfirmationPanel(MainFrame frame){
    this.frame = frame;

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(),BorderLayout.NORTH);
    add(centerCard(),BorderLayout.CENTER);
    add(actions(),BorderLayout.SOUTH);
}



private JComponent header(){

    JPanel p = new JPanel();
    p.setOpaque(false);
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));

    JLabel title = new JLabel("Booking Summary");
    title.setFont(UIConfig.FONT_TITLE);
    title.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel sub = new JLabel("Review your booking details");
    sub.setFont(UIConfig.FONT_SMALL);
    sub.setForeground(UIConfig.TEXT_LIGHT);
    sub.setAlignmentX(Component.CENTER_ALIGNMENT);

    p.add(title);
    p.add(Box.createVerticalStrut(5));
    p.add(sub);

    return p;
}



private JComponent centerCard(){

    JPanel wrapper = new JPanel(new GridBagLayout());
    wrapper.setOpaque(false);

    JPanel card = new JPanel();
    card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
    card.setPreferredSize(new Dimension(420,300));

    UIConfig.styleCard(card);

    card.add(row("Schedule ID",
            String.valueOf(BookingContext.scheduleId)));

    card.add(row("Seats",
            String.join(", ", BookingContext.getSelectedSeats())));

    card.add(row("Fare / Seat",
            "₹ " + BookingContext.farePerSeat));

    card.add(row("Total Seats",
            String.valueOf(BookingContext.seatCount())));

    card.add(Box.createVerticalStrut(10));
    card.add(new JSeparator());
    card.add(Box.createVerticalStrut(10));

    card.add(totalRow());

    wrapper.add(card);

    return wrapper;
}



private JPanel row(String key,String value){

    JPanel r = new JPanel(new BorderLayout());
    r.setOpaque(false);
    r.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));

    JLabel k = new JLabel(key);
    k.setForeground(UIConfig.TEXT_LIGHT);

    JLabel v = new JLabel(value);
    v.setFont(UIConfig.FONT_NORMAL);

    r.add(k,BorderLayout.WEST);
    r.add(v,BorderLayout.EAST);

    return r;
}



private JPanel totalRow(){

    JPanel r = new JPanel(new BorderLayout());
    r.setOpaque(false);

    JLabel k = new JLabel("Total Fare");
    k.setFont(UIConfig.FONT_SUBTITLE);

    JLabel v = new JLabel("₹ " + BookingContext.getFinalAmount());
    v.setFont(new Font("Segoe UI",Font.BOLD,24));
    v.setForeground(UIConfig.SUCCESS);

    r.add(k,BorderLayout.WEST);
    r.add(v,BorderLayout.EAST);

    return r;
}



private JComponent actions(){

    JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,10));
    p.setOpaque(false);

    JButton back = btn("← Back");
    UIConfig.secondaryBtn(back);

    JButton proceed = btn("Proceed to Payment");
    UIConfig.primaryBtn(proceed);

    back.addActionListener(e -> frame.goBack());
    proceed.addActionListener(e -> frame.showScreen("PAYMENT"));

    p.add(back);
    p.add(proceed);

    return p;
}

private JButton btn(String text){

    JButton b = new JButton(text);
    b.setPreferredSize(new Dimension(180,40)); 
    return b;
}

}
