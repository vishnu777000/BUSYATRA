package ui.booking;

import config.UIConfig;
import ui.common.MainFrame;
import util.BookingContext;
import util.Refreshable;

import javax.swing.*;
import java.awt.*;

public class PassengerDetailsPanel extends JPanel implements Refreshable {

private final MainFrame frame;

private JTextField nameField;
private JTextField phoneField;
private JTextField emailField;

private JLabel seatsLabel;
private JLabel amountLabel;

public PassengerDetailsPanel(MainFrame frame){

    this.frame = frame;

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(),BorderLayout.NORTH);
    add(center(),BorderLayout.CENTER);
    add(actions(),BorderLayout.SOUTH);
}



private JComponent header(){

    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JButton back = new JButton("← Back");
    UIConfig.secondaryBtn(back);
    back.setPreferredSize(new Dimension(120,35));

    back.addActionListener(e -> frame.goBack());

    JLabel title = new JLabel("Passenger Details",SwingConstants.CENTER);
    title.setFont(UIConfig.FONT_TITLE);

    panel.add(back,BorderLayout.WEST);
    panel.add(title,BorderLayout.CENTER);

    return panel;
}



private JComponent center(){

    JPanel wrapper = new JPanel(new GridBagLayout());
    wrapper.setOpaque(false);

    JPanel card = new JPanel();
    card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
    card.setPreferredSize(new Dimension(420,320));

    UIConfig.styleCard(card);

    nameField = field("Passenger Name");
    phoneField = field("Mobile Number");
    emailField = field("Email Address");

    seatsLabel = new JLabel();
    seatsLabel.setFont(UIConfig.FONT_SMALL);
    seatsLabel.setForeground(UIConfig.TEXT_LIGHT);

    amountLabel = new JLabel();
    amountLabel.setFont(new Font("Segoe UI",Font.BOLD,22));
    amountLabel.setForeground(UIConfig.SUCCESS);

    card.add(nameField);
    card.add(Box.createVerticalStrut(12));

    card.add(phoneField);
    card.add(Box.createVerticalStrut(12));

    card.add(emailField);
    card.add(Box.createVerticalStrut(20));

    card.add(new JSeparator());
    card.add(Box.createVerticalStrut(12));

    card.add(seatsLabel);
    card.add(Box.createVerticalStrut(8));

    card.add(amountLabel);

    wrapper.add(card);

    return wrapper;
}

private JTextField field(String title){

    JTextField f = new JTextField();
    UIConfig.styleField(f);
    f.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));
    f.setBorder(BorderFactory.createTitledBorder(title));
    return f;
}



private JComponent actions(){

    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,10));
    panel.setOpaque(false);

    JButton proceed = new JButton("Proceed to Payment");
    proceed.setPreferredSize(new Dimension(200,40));

    UIConfig.primaryBtn(proceed);

    proceed.addActionListener(e -> proceed());

    panel.add(proceed);

    return panel;
}



private void proceed(){

    String name = nameField.getText().trim();
    String phone = phoneField.getText().trim();
    String email = emailField.getText().trim();

    if(name.isEmpty()){
        JOptionPane.showMessageDialog(this,"Enter passenger name");
        return;
    }

    if(!phone.matches("\\d{10}")){
        JOptionPane.showMessageDialog(this,"Enter valid 10 digit mobile number");
        return;
    }

    if(!email.contains("@")){
        JOptionPane.showMessageDialog(this,"Enter valid email address");
        return;
    }

    BookingContext.passengerName = name;
    BookingContext.passengerPhone = phone;
    BookingContext.passengerEmail = email;

    frame.showScreen(MainFrame.SCREEN_PAYMENT);
}



@Override
public void refreshData(){

    seatsLabel.setText(
            "Seats : " + BookingContext.copySelectedSeats()
    );

    amountLabel.setText(
            "Total Amount : ₹ " + BookingContext.getFinalAmount()
    );
}

}
