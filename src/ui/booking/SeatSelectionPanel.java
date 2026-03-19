package ui.booking;

import config.UIConfig;
import dao.SeatDAO;
import dao.SeatLockDAO;
import ui.common.MainFrame;
import util.BookingContext;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class SeatSelectionPanel extends JPanel implements Refreshable {

    private final MainFrame frame;

    private final SeatDAO seatDAO = new SeatDAO();
    private final SeatLockDAO seatLockDAO = new SeatLockDAO();

    private Set<String> unavailableSeats = new HashSet<>();
    private final Set<String> selectedSeats = new HashSet<>();

    private static final int SEAT_SIZE = 36;
    private static final int MAX_SEATS = 6;

    private JLabel payableLabel;
    private JLabel selectedLabel;

    private final Color AVAILABLE = new Color(232,245,233);
    private final Color SELECTED = new Color(0,150,136);
    private final Color BOOKED = new Color(210,210,210);

    public SeatSelectionPanel(MainFrame frame) {

        this.frame = frame;

        setLayout(new BorderLayout(15,15));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        add(header(),BorderLayout.NORTH);
        add(centerPanel(),BorderLayout.CENTER);
        add(bottomBar(),BorderLayout.SOUTH);
    }

    /* ================= HEADER ================= */

    private JComponent header(){

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton back = new JButton("← Back");
        UIConfig.secondaryBtn(back);

        back.addActionListener(e -> frame.goBack());

        JLabel title = new JLabel(
                BookingContext.fromStop + " → " + BookingContext.toStop,
                SwingConstants.CENTER
        );

        title.setFont(UIConfig.FONT_SUBTITLE);

        panel.add(back,BorderLayout.WEST);
        panel.add(title,BorderLayout.CENTER);
        panel.add(legend(),BorderLayout.EAST);

        return panel;
    }

    /* ================= LEGEND ================= */

    private JPanel legend(){

        JPanel legend = new JPanel(new FlowLayout());
        legend.setOpaque(false);

        legend.add(colorBox(AVAILABLE,"Available"));
        legend.add(colorBox(SELECTED,"Selected"));
        legend.add(colorBox(BOOKED,"Booked"));

        return legend;
    }

    private JPanel colorBox(Color color,String text){

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
        p.setOpaque(false);

        JLabel box = new JLabel();
        box.setOpaque(true);
        box.setBackground(color);
        box.setPreferredSize(new Dimension(14,14));

        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConfig.FONT_SMALL);

        p.add(box);
        p.add(lbl);

        return p;
    }

    /* ================= CENTER ================= */

    private JComponent centerPanel(){

        JPanel panel = new JPanel(new BorderLayout(10,10));
        panel.setOpaque(false);

        panel.add(
                new RouteTimelinePanel(BookingContext.routeId),
                BorderLayout.NORTH
        );

        panel.add(scrollSeats(),BorderLayout.CENTER);

        return panel;
    }

    private JComponent scrollSeats(){

        JScrollPane sp = new JScrollPane(seatLayout());

        sp.setBorder(null);
        sp.getViewport().setBackground(UIConfig.BACKGROUND);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        return sp;
    }

    /* ================= SEAT LAYOUT ================= */

    private JPanel seatLayout(){

        JPanel busBody = new JPanel();
        busBody.setLayout(new BoxLayout(busBody,BoxLayout.Y_AXIS));
        busBody.setBackground(Color.WHITE);

        busBody.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,200,200),2),
                BorderFactory.createEmptyBorder(25,40,25,40)
        ));

        JLabel driver = new JLabel("DRIVER",SwingConstants.CENTER);
        driver.setOpaque(true);
        driver.setBackground(new Color(220,220,220));
        driver.setFont(new Font("Segoe UI",Font.BOLD,12));
        driver.setBorder(BorderFactory.createEmptyBorder(6,20,6,20));

        busBody.add(driver);
        busBody.add(Box.createVerticalStrut(15));

        int seatNo = 1;
        int total = BookingContext.totalSeats;

        while(seatNo <= total-5){
            busBody.add(seatRow(seatNo));
            busBody.add(Box.createVerticalStrut(10));
            seatNo += 4;
        }

        busBody.add(lastRow(total));

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapper.setOpaque(false);
        wrapper.add(busBody);

        return wrapper;
    }

    /* ================= ROW ================= */

    private JComponent seatRow(int start){

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3,6,3,6);

        c.gridx=0; row.add(seatBtn("S"+start),c);
        c.gridx=1; row.add(seatBtn("S"+(start+1)),c);

        c.gridx=2;
        row.add(Box.createHorizontalStrut(60),c);

        c.gridx=3; row.add(seatBtn("S"+(start+2)),c);
        c.gridx=4; row.add(seatBtn("S"+(start+3)),c);

        return row;
    }

    private JComponent lastRow(int total){

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER,8,10));
        row.setOpaque(false);

        for(int i=total-4;i<=total;i++){
            row.add(seatBtn("S"+i));
        }

        return row;
    }

    /* ================= SEAT BUTTON ================= */

    private JButton seatBtn(String seatNo){

        JButton b = new JButton(seatNo);

        b.setPreferredSize(new Dimension(SEAT_SIZE,SEAT_SIZE));
        b.setFont(new Font("Segoe UI",Font.BOLD,11));
        b.setFocusPainted(false);

        b.setBorder(BorderFactory.createLineBorder(new Color(210,210,210)));
        b.setBackground(AVAILABLE);

        if(unavailableSeats.contains(seatNo)){

            b.setBackground(BOOKED);
            b.setForeground(Color.WHITE);
            b.setEnabled(false);

        }else{

            b.addActionListener(e -> toggleSeat(b,seatNo));

            b.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if(!selectedSeats.contains(seatNo))
                        b.setBackground(new Color(200,230,201));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if(!selectedSeats.contains(seatNo))
                        b.setBackground(AVAILABLE);
                }
            });
        }

        return b;
    }

    /* ================= TOGGLE ================= */

    private void toggleSeat(JButton b,String seatNo){

        if(selectedSeats.contains(seatNo)){

            selectedSeats.remove(seatNo);
            BookingContext.removeSeat(seatNo);

            seatLockDAO.releaseSeatLock(
                    BookingContext.scheduleId,
                    seatNo,
                    Session.userId
            );

            b.setBackground(AVAILABLE);
            b.setForeground(Color.BLACK);

        }else{

            if(selectedSeats.size() >= MAX_SEATS){
                JOptionPane.showMessageDialog(this,"Max 6 seats allowed");
                return;
            }

            if(seatLockDAO.isSeatLocked(
                    BookingContext.scheduleId,seatNo)){
                JOptionPane.showMessageDialog(this,"Seat already locked");
                return;
            }

            boolean locked = seatLockDAO.lockSeat(
                    BookingContext.scheduleId,
                    seatNo,
                    Session.userId
            );

            if(!locked){
                JOptionPane.showMessageDialog(this,"Lock failed");
                return;
            }

            selectedSeats.add(seatNo);
            BookingContext.addSeat(seatNo);

            b.setBackground(SELECTED);
            b.setForeground(Color.WHITE);
        }

        updatePayable();
    }

    /* ================= PAYABLE ================= */

    private void updatePayable(){

        selectedLabel.setText(
                "Seats : " + BookingContext.getSeatListString()
        );

        payableLabel.setText(
                "Total : ₹ " + BookingContext.getFinalAmount()
        );
    }

    /* ================= BOTTOM ================= */

    private JComponent bottomBar(){

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel left = new JPanel(new GridLayout(2,1));
        left.setOpaque(false);

        selectedLabel = new JLabel("Seats : []");
        selectedLabel.setFont(UIConfig.FONT_SMALL);

        payableLabel = new JLabel("Total : ₹ 0");
        payableLabel.setFont(UIConfig.FONT_SUBTITLE);

        left.add(selectedLabel);
        left.add(payableLabel);

        JButton proceed = new JButton("Proceed to Payment");
        UIConfig.primaryBtn(proceed);

        proceed.addActionListener(e -> {

            if(selectedSeats.isEmpty()){
                JOptionPane.showMessageDialog(this,"Select seats");
                return;
            }

            frame.showScreen(MainFrame.SCREEN_PASSENGER);
        });

        panel.add(left,BorderLayout.WEST);
        panel.add(proceed,BorderLayout.EAST);

        return panel;
    }

    /* ================= REFRESH ================= */

    @Override
    public void refreshData(){

        BookingContext.clearSeats();
        selectedSeats.clear();

        seatLockDAO.clearExpiredLocks();

        unavailableSeats =
                seatDAO.getUnavailableSeats(
                        BookingContext.scheduleId,
                        BookingContext.fromOrder,
                        BookingContext.toOrder
                );

        BookingContext.totalSeats =
                seatDAO.getTotalSeatsBySchedule(
                        BookingContext.scheduleId
                );

        removeAll();

        add(header(),BorderLayout.NORTH);
        add(centerPanel(),BorderLayout.CENTER);
        add(bottomBar(),BorderLayout.SOUTH);

        revalidate();
        repaint();
    }
}