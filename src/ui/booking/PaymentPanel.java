package ui.booking;

import config.UIConfig;
import dao.BookingDAO;
import dao.SeatLockDAO;
import dao.WalletDAO;
import ui.common.MainFrame;
import util.BookingContext;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class PaymentPanel extends JPanel implements Refreshable {

private final MainFrame frame;

private JLabel tripLabel;
private JLabel seatsLabel;
private JLabel amountLabel;
private JLabel walletLabel;
private JLabel statusLabel;

private JButton payBtn;

public PaymentPanel(MainFrame frame) {

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

    JLabel title = new JLabel("Payment",SwingConstants.CENTER);
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
    card.setPreferredSize(new Dimension(420,300));

    UIConfig.styleCard(card);

    tripLabel = centerLabel(UIConfig.FONT_SUBTITLE);
    seatsLabel = centerLabel(UIConfig.FONT_SMALL);

    amountLabel = centerLabel(new Font("Segoe UI",Font.BOLD,28));
    amountLabel.setForeground(UIConfig.SUCCESS);

    walletLabel = centerLabel(UIConfig.FONT_SMALL);
    walletLabel.setForeground(UIConfig.TEXT_LIGHT);

    statusLabel = centerLabel(UIConfig.FONT_SMALL);

    card.add(tripLabel);
    card.add(Box.createVerticalStrut(10));

    card.add(seatsLabel);
    card.add(Box.createVerticalStrut(20));

    card.add(amountLabel);
    card.add(Box.createVerticalStrut(10));

    card.add(walletLabel);
    card.add(Box.createVerticalStrut(15));

    card.add(statusLabel);

    wrapper.add(card);

    return wrapper;
}

private JLabel centerLabel(Font font){

    JLabel l = new JLabel("",SwingConstants.CENTER);
    l.setFont(font);
    l.setAlignmentX(Component.CENTER_ALIGNMENT);
    return l;
}

/* ================= ACTIONS ================= */

private JComponent actions(){

    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,10));
    panel.setOpaque(false);

    payBtn = new JButton("Pay using Wallet");
    payBtn.setPreferredSize(new Dimension(220,45));

    UIConfig.primaryBtn(payBtn);

    payBtn.addActionListener(e -> doPayment());

    panel.add(payBtn);

    return panel;
}

/* ================= PAYMENT ================= */

private void doPayment(){

    if(!BookingContext.isReadyForPayment()){

        JOptionPane.showMessageDialog(this,"Booking data incomplete");
        return;
    }

    payBtn.setEnabled(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    statusLabel.setText("Processing payment...");

    double amount = BookingContext.getFinalAmount();

    WalletDAO walletDAO = new WalletDAO();
    BookingDAO bookingDAO = new BookingDAO();
    SeatLockDAO seatLockDAO = new SeatLockDAO();

    if(walletDAO.getBalance(Session.userId) < amount){

        fail("Insufficient wallet balance");
        return;
    }

    Set<String> seats = BookingContext.copySelectedSeats();

    Timer t = new Timer(1000,e -> {

        try{

            int generatedTicketId = -1;

            /* CHECK SEATS */

            for(String seat : seats){

                boolean available =
                        bookingDAO.isSeatAvailable(
                                BookingContext.scheduleId,
                                seat,
                                BookingContext.fromOrder,
                                BookingContext.toOrder
                        );

                if(!available){
                    fail("Seat "+seat+" already booked");
                    return;
                }
            }

            /* WALLET PAYMENT */

            if(!walletDAO.deductMoney(Session.userId,amount)){
                fail("Payment failed");
                return;
            }

            /* INSERT BOOKINGS */

            for(String seat : seats){

                int ticketId =
                        bookingDAO.insertBooking(
                                Session.userId,
                                BookingContext.routeId,
                                BookingContext.scheduleId,
                                BookingContext.fromStop,
                                BookingContext.toStop,
                                BookingContext.fromOrder,
                                BookingContext.toOrder,
                                seat,
                                BookingContext.farePerSeat,
                                BookingContext.journeyDate
                        );

                if(ticketId <= 0){

                    walletDAO.addMoney(Session.userId,amount);
                    fail("Booking failed");
                    return;
                }

                generatedTicketId = ticketId;
            }

            BookingContext.ticketId = generatedTicketId;

            seatLockDAO.releaseSeatLocks(Session.userId);

            success();

        }catch(Exception ex){
            ex.printStackTrace();
            fail("Unexpected error");
        }

    });

    t.setRepeats(false);
    t.start();
}

/* ================= SUCCESS ================= */

private void success(){

    statusLabel.setText("Payment successful ✅");
    setCursor(Cursor.getDefaultCursor());

    JOptionPane.showMessageDialog(this,"Booking successful ✅");

    frame.showScreen(MainFrame.SCREEN_SUMMARY);
}

/* ================= FAIL ================= */

private void fail(String msg){

    statusLabel.setText("");
    setCursor(Cursor.getDefaultCursor());
    payBtn.setEnabled(true);

    JOptionPane.showMessageDialog(this,msg);
}

/* ================= REFRESH ================= */

@Override
public void refreshData(){

    tripLabel.setText(
            BookingContext.fromStop + " → " + BookingContext.toStop
    );

    seatsLabel.setText(
            "Seats : " + BookingContext.copySelectedSeats()
    );

    amountLabel.setText(
            "₹ " + BookingContext.getFinalAmount()
    );

    walletLabel.setText(
            "Wallet Balance : ₹ " +
            new WalletDAO().getBalance(Session.userId)
    );

    statusLabel.setText("");
    payBtn.setEnabled(true);
}

}
