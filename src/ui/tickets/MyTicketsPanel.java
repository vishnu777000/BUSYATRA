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
    private final List<TicketRowMeta> ticketRows = new ArrayList<>();

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
                new Object[]{"Ticket Ref","From","To","Seat(s)","Fare (INR)","Journey Date","Status"},0){
            public boolean isCellEditable(int r,int c){
                return false;
            }
        };

        table = new JTable(model);
        UIConfig.styleTable(table);

        JScrollPane sp = new JScrollPane(table);
        UIConfig.styleScroll(sp);
        sp.setColumnHeaderView(table.getTableHeader());

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
        ticketRows.clear();
        if (statusLabel != null) statusLabel.setText("Loading tickets...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<List<TicketRowMeta>,Void> worker = new SwingWorker<>() {
            @Override
            protected List<TicketRowMeta> doInBackground() {

                List<TicketRowMeta> data = new ArrayList<>();

                try {
                    List<String[]> list = new BookingDAO().getBookingsByUser(Session.userId);

                    for(String[] r : list){
                        TicketRowMeta row = TicketRowMeta.from(r);
                        if (row != null) {
                            data.add(row);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return data;
            }

            @Override
            protected void done() {

                try {
                    List<TicketRowMeta> data = get();
                    ticketRows.clear();
                    ticketRows.addAll(data);
                    for(TicketRowMeta row : data){
                        model.addRow(row.toTableRow());
                    }
                    if (statusLabel != null) {
                        statusLabel.setText("Loaded " + data.size() + " bookings");
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

    private TicketRowMeta selectedRowMeta() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= ticketRows.size()) {
            return null;
        }
        return ticketRows.get(row);
    }

    private void openPreview() {

        TicketRowMeta selected = selectedRowMeta();
        if(selected == null){
            JOptionPane.showMessageDialog(this,"Select booking");
            return;
        }

        BookingContext.setRecentTicketIds(selected.bookingIds);

        frame.showScreen(MainFrame.SCREEN_TICKET_PREVIEW);
    }

    private void openCancelScreen() {

        TicketRowMeta selected = selectedRowMeta();
        if(selected == null){
            JOptionPane.showMessageDialog(this,"Select booking");
            return;
        }

        if(!"CONFIRMED".equalsIgnoreCase(selected.status)){
            JOptionPane.showMessageDialog(this,"Only confirmed can cancel");
            return;
        }

        CancelTicketPanel panel =
                (CancelTicketPanel) frame.getScreen(MainFrame.SCREEN_CANCEL);

        panel.setBookingSelection(selected.bookingIds, selected.displayRef);

        frame.showScreen(MainFrame.SCREEN_CANCEL);
    }

    public void refreshData() {
        loadBookings();
    }

    @Override
    public boolean refreshOnFirstShow() {
        return false;
    }

    private void deleteHistory() {
        TicketRowMeta selected = selectedRowMeta();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select booking");
            return;
        }

        if ("CONFIRMED".equalsIgnoreCase(selected.status)) {
            JOptionPane.showMessageDialog(this, "Cancel confirmed booking first, then delete history.");
            return;
        }

        int c = JOptionPane.showConfirmDialog(this,
                "Delete selected history record?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        if (statusLabel != null) statusLabel.setText("Deleting history...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return new BookingDAO().deleteBookingHistoryForUser(selected.bookingIds, Session.userId);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    if (Boolean.TRUE.equals(get())) {
                        loadBookings();
                        JOptionPane.showMessageDialog(MyTicketsPanel.this, "History deleted");
                    } else {
                        if (statusLabel != null) statusLabel.setText("Delete failed");
                        JOptionPane.showMessageDialog(MyTicketsPanel.this, "Unable to delete history");
                    }
                } catch (Exception e) {
                    if (statusLabel != null) statusLabel.setText("Delete failed");
                    JOptionPane.showMessageDialog(MyTicketsPanel.this, "Unable to delete history");
                }
            }
        };

        worker.execute();
    }

    private static class TicketRowMeta {
        private String displayRef;
        private String from;
        private String to;
        private String seats;
        private String amount;
        private String journeyDate;
        private String status;
        private final List<Integer> bookingIds = new ArrayList<>();

        private static TicketRowMeta from(String[] raw) {
            if (raw == null || raw.length < 7) return null;

            TicketRowMeta row = new TicketRowMeta();
            row.displayRef = raw[0];
            row.from = raw[1];
            row.to = raw[2];
            row.seats = raw[3];
            row.amount = raw[4];
            row.journeyDate = raw[5];
            row.status = raw[6];

            if (raw.length >= 8 && raw[7] != null) {
                for (String token : raw[7].split(",")) {
                    try {
                        int id = Integer.parseInt(token.trim());
                        if (id > 0) {
                            row.bookingIds.add(id);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            if (row.bookingIds.isEmpty()) {
                try {
                    int id = Integer.parseInt(raw[0].trim());
                    if (id > 0) {
                        row.bookingIds.add(id);
                    }
                } catch (Exception ignored) {
                }
            }

            return row.bookingIds.isEmpty() ? null : row;
        }

        private Object[] toTableRow() {
            return new Object[]{displayRef, from, to, seats, amount, journeyDate, status};
        }
    }
}
