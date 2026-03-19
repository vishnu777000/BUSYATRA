package ui.tickets;

import config.UIConfig;
import dao.BookingDAO;
import dao.WalletDAO;
import util.Session;

import javax.swing.*;
import java.awt.*;

public class CancelTicketPanel extends JPanel {

    private int currentBookingId = -1;

    private final BookingDAO bookingDAO = new BookingDAO();
    private final WalletDAO walletDAO = new WalletDAO();

    private JButton cancelBtn;

    public void setBookingId(int bookingId) {
        this.currentBookingId = bookingId;
    }

    public CancelTicketPanel() {

        setLayout(new GridBagLayout());
        setBackground(UIConfig.BACKGROUND);

        JPanel card = new JPanel();
        UIConfig.styleCard(card);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 300));

        JLabel title =
                new JLabel("Cancel Ticket",
                        SwingConstants.CENTER);

        title.setFont(UIConfig.FONT_TITLE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel info =
                new JLabel("Confirm cancellation of selected booking",
                        SwingConstants.CENTER);

        info.setFont(UIConfig.FONT_SMALL);
        info.setForeground(UIConfig.TEXT_LIGHT);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);

        cancelBtn = new JButton("Confirm Cancellation");

        UIConfig.dangerBtn(cancelBtn);
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        cancelBtn.addActionListener(e -> cancelBooking());

        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(info);
        card.add(Box.createVerticalStrut(25));
        card.add(cancelBtn);

        add(card);
    }

    /* ================= CANCEL FLOW ================= */

    private void cancelBooking() {

        if (currentBookingId <= 0) {
            error("Invalid booking selected");
            return;
        }

        cancelBtn.setEnabled(false);

        int bookingId = currentBookingId;

        double refund =
                bookingDAO.getBookingAmount(bookingId);

        if (refund <= 0) {
            cancelBtn.setEnabled(true);
            error("Booking not found or already cancelled");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Cancel this booking?\nRefund: ₹ " + refund,
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            cancelBtn.setEnabled(true);
            return;
        }

        /* 1️⃣ Update booking status */

        if (!bookingDAO.cancelBooking(bookingId)) {

            cancelBtn.setEnabled(true);
            error("Cancellation failed");
            return;
        }

        /* 2️⃣ Refund wallet */

        boolean refunded =
                walletDAO.addMoney(Session.userId, refund);

        if (!refunded) {

            cancelBtn.setEnabled(true);
            error("Cancellation done but refund failed. Contact support.");
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "Booking cancelled successfully ✅\n₹ " +
                        refund + " refunded to wallet"
        );

        cancelBtn.setEnabled(true);
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