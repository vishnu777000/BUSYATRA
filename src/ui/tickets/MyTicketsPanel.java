package ui.tickets;

import config.UIConfig;
import dao.BookingDAO;
import util.Refreshable;
import util.Session;
import util.IconUtil;
import util.BookingContext;
import ui.common.MainFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MyTicketsPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private JTable table;
    private DefaultTableModel model;

    public MyTicketsPanel(MainFrame frame) {

        this.frame = frame;

        setLayout(new BorderLayout(16,16));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        add(topBar(), BorderLayout.NORTH);
        add(tableCard(), BorderLayout.CENTER);

        loadBookings();
    }

    /* ================= TOP ================= */

    private JPanel topBar() {

        JLabel title = new JLabel("My Bookings");
        title.setFont(UIConfig.FONT_TITLE);

        JButton refreshBtn = new JButton("Refresh");
        UIConfig.infoBtn(refreshBtn);
        refreshBtn.addActionListener(e -> loadBookings());

        JButton previewBtn = new JButton("View Ticket");
        UIConfig.primaryBtn(previewBtn);
        previewBtn.addActionListener(e -> openPreview());

        JButton cancelBtn = new JButton("Cancel");
        UIConfig.dangerBtn(cancelBtn);
        cancelBtn.addActionListener(e -> openCancelScreen());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        actions.add(refreshBtn);
        actions.add(previewBtn);
        actions.add(cancelBtn);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        bar.add(title,BorderLayout.WEST);
        bar.add(actions,BorderLayout.EAST);

        return bar;
    }

    /* ================= TABLE ================= */

    private JPanel tableCard() {

        model = new DefaultTableModel(
                new Object[]{
                        "ID","Route","Seat","Amount","Date","Status"
                },0){

            public boolean isCellEditable(int r,int c){
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(28);

        JScrollPane sp = new JScrollPane(table);

        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);

        card.add(sp);

        return card;
    }

    /* ================= LOAD ================= */

    private void loadBookings() {

        model.setRowCount(0);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<List<Object[]>,Void> worker = new SwingWorker<>() {
          @Override
            protected List<Object[]> doInBackground() {

                List<Object[]> data = new ArrayList<>();

                try {

    List<String[]> list =
            new BookingDAO().getBookingsByUser(Session.userId);

    for(String[] r : list){

        model.addRow(new Object[]{
                r[0], // id
                r[1] + " → " + r[2], // route
                r[3], // seat
                "₹ " + r[4],
                r[5], // date
                r[6]  // status
        });
    }

} catch (Exception e) {
    e.printStackTrace();
}

                return data;
            }

            protected void done() {

                try {

                    List<Object[]> data = get();

                    for(Object[] row : data){
                        model.addRow(row);
                    }

                } catch (Exception e) {

                    JOptionPane.showMessageDialog(
                            MyTicketsPanel.this,
                            "Failed to load bookings"
                    );
                }

                setCursor(Cursor.getDefaultCursor());
            }
        };

        worker.execute();
    }

    /* ================= PREVIEW ================= */

    private void openPreview() {

        int row = table.getSelectedRow();

        if(row==-1){
            JOptionPane.showMessageDialog(this,"Select booking");
            return;
        }

        int id = (int) model.getValueAt(row,0);

        BookingContext.ticketId = id;

        frame.showScreen(MainFrame.SCREEN_TICKET_PREVIEW);
    }

    /* ================= CANCEL ================= */

    private void openCancelScreen() {

        int row = table.getSelectedRow();

        if(row==-1){
            JOptionPane.showMessageDialog(this,"Select booking");
            return;
        }

        String status = model.getValueAt(row,5).toString();

        if(!"CONFIRMED".equalsIgnoreCase(status)){
            JOptionPane.showMessageDialog(this,"Only confirmed can cancel");
            return;
        }

        int id = (int) model.getValueAt(row,0);

        CancelTicketPanel panel =
                (CancelTicketPanel) frame.getScreen(MainFrame.SCREEN_CANCEL);

        panel.setBookingId(id);

        frame.showScreen(MainFrame.SCREEN_CANCEL);
    }

    /* ================= REFRESH ================= */

    public void refreshData() {
        loadBookings();
    }
}