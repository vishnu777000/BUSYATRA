package ui.tickets;

import config.UIConfig;
import dao.BookingDAO;
import ui.common.MainFrame;
import util.BookingContext;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MyTicketsPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JLabel statusLabel;

    public MyTicketsPanel(MainFrame frame) {

        this.frame = frame;

        setLayout(new BorderLayout(16,16));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        add(topBar(), BorderLayout.NORTH);
        add(tableCard(), BorderLayout.CENTER);

        loadBookings();
    }

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
        JButton deleteBtn = new JButton("Delete History");
        UIConfig.secondaryBtn(deleteBtn);
        deleteBtn.addActionListener(e -> deleteHistory());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        actions.add(refreshBtn);
        actions.add(previewBtn);
        actions.add(cancelBtn);
        actions.add(deleteBtn);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        bar.add(title,BorderLayout.WEST);
        bar.add(actions,BorderLayout.EAST);

        return bar;
    }

    private JPanel tableCard() {

        model = new DefaultTableModel(
                new Object[]{"Ticket ID","From","To","Seat(s)","Fare (INR)","Journey Date","Status"},0){
            public boolean isCellEditable(int r,int c){
                return false;
            }
        };

        table = new JTable(model);
        UIConfig.styleTable(table);

        JScrollPane sp = new JScrollPane(table);
        UIConfig.styleScroll(sp);

        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UIConfig.FONT_SMALL);
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        card.add(sp, BorderLayout.CENTER);
        card.add(statusLabel, BorderLayout.SOUTH);

        return card;
    }

    private void loadBookings() {

        model.setRowCount(0);
        if (statusLabel != null) statusLabel.setText("Loading tickets...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<List<Object[]>,Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object[]> doInBackground() {

                List<Object[]> data = new ArrayList<>();

                try {
                    List<String[]> list = new BookingDAO().getBookingsByUser(Session.userId);

                    for(String[] r : list){
                        if (r == null || r.length < 7) continue;
                        data.add(new Object[]{
                                Integer.parseInt(r[0]),
                                r[1],
                                r[2],
                                r[3],
                                r[4],
                                r[5],
                                r[6]
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return data;
            }

            @Override
            protected void done() {

                try {
                    List<Object[]> data = get();
                    for(Object[] row : data){
                        model.addRow(row);
                    }
                    if (statusLabel != null) {
                        statusLabel.setText("Loaded " + data.size() + " tickets");
                    }
                } catch (Exception e) {
                    if (statusLabel != null) statusLabel.setText("Failed to load bookings");
                    JOptionPane.showMessageDialog(MyTicketsPanel.this, "Failed to load bookings");
                }

                setCursor(Cursor.getDefaultCursor());
            }
        };

        worker.execute();
    }

    private void openPreview() {

        int row = table.getSelectedRow();

        if(row==-1){
            JOptionPane.showMessageDialog(this,"Select booking");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row,0).toString());

        BookingContext.ticketId = id;

        frame.showScreen(MainFrame.SCREEN_TICKET_PREVIEW);
    }

    private void openCancelScreen() {

        int row = table.getSelectedRow();

        if(row==-1){
            JOptionPane.showMessageDialog(this,"Select booking");
            return;
        }

        String status = model.getValueAt(row,6).toString();

        if(!"CONFIRMED".equalsIgnoreCase(status)){
            JOptionPane.showMessageDialog(this,"Only confirmed can cancel");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row,0).toString());

        CancelTicketPanel panel =
                (CancelTicketPanel) frame.getScreen(MainFrame.SCREEN_CANCEL);

        panel.setBookingId(id);

        frame.showScreen(MainFrame.SCREEN_CANCEL);
    }

    public void refreshData() {
        loadBookings();
    }

    private void deleteHistory() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select booking");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        String status = model.getValueAt(row, 6).toString();
        if ("CONFIRMED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Cancel confirmed booking first, then delete history.");
            return;
        }

        int c = JOptionPane.showConfirmDialog(this,
                "Delete selected history record?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        boolean ok = new BookingDAO().deleteBookingHistoryForUser(id, Session.userId);
        if (ok) {
            loadBookings();
            JOptionPane.showMessageDialog(this, "History deleted");
        } else {
            JOptionPane.showMessageDialog(this, "Unable to delete history");
        }
    }
}
