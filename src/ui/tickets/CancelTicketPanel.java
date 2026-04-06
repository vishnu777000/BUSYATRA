package ui.tickets;

import config.UIConfig;
import dao.BookingDAO;
import dao.WalletDAO;
import util.BookingContext;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CancelTicketPanel extends JPanel {

    private final List<Integer> currentBookingIds = new ArrayList<>();
    private String currentBookingRef = "";

    private final BookingDAO bookingDAO = new BookingDAO();
    private final WalletDAO walletDAO = new WalletDAO();

    private JButton cancelBtn;
    private JLabel statusLabel;

    public void setBookingSelection(List<Integer> bookingIds, String bookingRef) {
        currentBookingIds.clear();
        if (bookingIds != null) {
            for (Integer bookingId : bookingIds) {
                if (bookingId != null && bookingId > 0 && !currentBookingIds.contains(bookingId)) {
                    currentBookingIds.add(bookingId);
                }
            }
        }
        this.currentBookingRef = bookingRef == null ? "" : bookingRef.trim();
    }

    public CancelTicketPanel() {

        setLayout(new GridBagLayout());
        setBackground(UIConfig.BACKGROUND);

        JPanel card = new JPanel();
        UIConfig.styleCard(card);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 300));

        JLabel title = new JLabel("Cancel Ticket", SwingConstants.CENTER);
        title.setFont(UIConfig.FONT_TITLE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel info = new JLabel("Confirm cancellation of selected booking", SwingConstants.CENTER);
        info.setFont(UIConfig.FONT_SMALL);
        info.setForeground(UIConfig.TEXT_LIGHT);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);

        cancelBtn = new JButton("Confirm Cancellation");
        UIConfig.dangerBtn(cancelBtn);
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelBtn.addActionListener(e -> cancelBooking());

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(info);
        card.add(Box.createVerticalStrut(20));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(cancelBtn);

        add(card);
    }

    private void cancelBooking() {

        List<Integer> bookingIds = resolveBookingIds();
        if (bookingIds.isEmpty()) {
            error("Invalid booking selected");
            return;
        }

        setBusy(true, "Checking refund...");

        SwingWorker<Double, Void> worker = new SwingWorker<>() {
            @Override
            protected Double doInBackground() {
                return bookingDAO.getBookingAmount(bookingIds);
            }

            @Override
            protected void done() {
                try {
                    double refund = get();
                    if (refund <= 0) {
                        setBusy(false, "Booking not found");
                        error("Booking not found or already cancelled");
                        return;
                    }

                    setBusy(false, " ");
                    int confirm = JOptionPane.showConfirmDialog(
                            CancelTicketPanel.this,
                            "Cancel this booking" +
                                    (currentBookingRef.isBlank() ? "" : " (" + currentBookingRef + ")") +
                                    "?\nRefund: INR " + String.format("%.2f", refund),
                            "Confirm Cancellation",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );

                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }

                    performCancellation(bookingIds, refund);
                } catch (Exception e) {
                    setBusy(false, "Unable to check refund");
                    error("Unable to check refund");
                }
            }
        };

        worker.execute();
    }

    private List<Integer> resolveBookingIds() {
        List<Integer> bookingIds = new ArrayList<>(currentBookingIds);
        if (!bookingIds.isEmpty()) {
            return bookingIds;
        }

        bookingIds.addAll(BookingContext.getRecentTicketIds());
        if (bookingIds.isEmpty() && BookingContext.getPrimaryTicketId() > 0) {
            bookingIds.add(BookingContext.getPrimaryTicketId());
        }
        if (!bookingIds.isEmpty() && currentBookingRef.isBlank()) {
            currentBookingRef = String.valueOf(bookingIds.get(bookingIds.size() - 1));
        }
        return bookingIds;
    }

    private void performCancellation(List<Integer> bookingIds, double refund) {
        setBusy(true, "Cancelling booking...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                if (!bookingDAO.cancelBookings(bookingIds)) {
                    return false;
                }
                return walletDAO.addMoney(Session.userId, refund);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = Boolean.TRUE.equals(get());
                    if (!ok) {
                        setBusy(false, "Cancellation failed");
                        error("Cancellation failed or refund could not be added");
                        return;
                    }

                    setBusy(false, "Cancellation completed");
                    setBookingSelection(null, "");
                    BookingContext.clearAfterPreview();
                    JOptionPane.showMessageDialog(
                            CancelTicketPanel.this,
                            "Booking cancelled successfully\nINR " + refund + " refunded to wallet"
                    );
                } catch (Exception e) {
                    setBusy(false, "Cancellation failed");
                    error("Cancellation failed");
                }
            }
        };

        worker.execute();
    }

    private void setBusy(boolean busy, String message) {
        if (cancelBtn != null) {
            cancelBtn.setEnabled(!busy);
        }
        if (statusLabel != null) {
            statusLabel.setText(message == null ? " " : message);
        }
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void error(String msg) {

        JOptionPane.showMessageDialog(
                this,
                msg,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
