package ui.booking;

import config.UIConfig;
import dao.BookingDAO;
import dao.SeatDAO;
import dao.SeatLockDAO;
import dao.WalletDAO;
import ui.common.MainFrame;
import util.IconUtil;
import util.BookingContext;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PaymentPanel extends JPanel implements Refreshable {

    private final MainFrame frame;

    private JLabel tripLabel;
    private JLabel seatsLabel;
    private JLabel amountLabel;
    private JLabel walletLabel;
    private JLabel statusLabel;
    private JLabel loadingGifLabel;
    private JProgressBar progressBar;

    private JButton payBtn;
    private JButton backBtn;

    public PaymentPanel(MainFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);
    }

    private JComponent header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        backBtn = new JButton("Back");
        UIConfig.secondaryBtn(backBtn);
        backBtn.setPreferredSize(new Dimension(120, 35));
        backBtn.addActionListener(e -> frame.goBack());

        JLabel title = new JLabel("Payment", SwingConstants.CENTER);
        title.setFont(UIConfig.FONT_TITLE);

        panel.add(backBtn, BorderLayout.WEST);
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }

    private JComponent center() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 300));
        UIConfig.styleCard(card);

        tripLabel = centerLabel(UIConfig.FONT_SUBTITLE);
        seatsLabel = centerLabel(UIConfig.FONT_SMALL);

        amountLabel = centerLabel(new Font("Segoe UI", Font.BOLD, 28));
        amountLabel.setForeground(UIConfig.SUCCESS);

        walletLabel = centerLabel(UIConfig.FONT_SMALL);
        walletLabel.setForeground(UIConfig.TEXT_LIGHT);

        statusLabel = centerLabel(UIConfig.FONT_SMALL);
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        URL loadingUrl = getClass().getResource("/resources/icons/actions/loading.gif");
        ImageIcon loadingIcon = loadingUrl != null
                ? new ImageIcon(loadingUrl)
                : IconUtil.load("loading.gif", 72, 72);
        loadingGifLabel = new JLabel(loadingIcon, SwingConstants.CENTER);
        loadingGifLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadingGifLabel.setPreferredSize(new Dimension(90, 72));
        if (loadingIcon.getIconWidth() <= 0) {
            loadingGifLabel.setText("Processing...");
        }
        loadingGifLabel.setVisible(false);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(220, 8));

        card.add(tripLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(seatsLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(amountLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(walletLabel);
        card.add(Box.createVerticalStrut(15));
        card.add(loadingGifLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(progressBar);
        card.add(Box.createVerticalStrut(8));
        card.add(statusLabel);

        wrapper.add(card);
        return wrapper;
    }

    private JLabel centerLabel(Font font) {
        JLabel l = new JLabel("", SwingConstants.CENTER);
        l.setFont(font);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JComponent actions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        panel.setOpaque(false);

        payBtn = new JButton("Pay using Wallet");
        payBtn.setPreferredSize(new Dimension(220, 45));
        UIConfig.primaryBtn(payBtn);
        payBtn.addActionListener(e -> doPayment());

        panel.add(payBtn);
        return panel;
    }

    private void setBusy(boolean busy, String text) {
        statusLabel.setText(text == null ? "" : text);
        loadingGifLabel.setVisible(busy);
        progressBar.setVisible(busy);
        payBtn.setEnabled(!busy);
        backBtn.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void doPayment() {
        if (!BookingContext.isReadyForPayment()) {
            JOptionPane.showMessageDialog(this, "Booking data incomplete");
            return;
        }

        final double amount = BookingContext.getFinalAmount();
        final Set<String> seats = BookingContext.copySelectedSeats();

        setBusy(true, "Processing payment...");

        SwingWorker<PaymentResult, Void> worker = new SwingWorker<>() {
            @Override
            protected PaymentResult doInBackground() {
                WalletDAO walletDAO = new WalletDAO();
                BookingDAO bookingDAO = new BookingDAO();
                SeatLockDAO seatLockDAO = new SeatLockDAO();
                SeatDAO seatDAO = new SeatDAO();
                List<Integer> createdBookingIds = new ArrayList<>();

                SwingUtilities.invokeLater(() -> statusLabel.setText("Checking wallet balance..."));

                if (walletDAO.getBalance(Session.userId) < amount) {
                    return PaymentResult.fail("Insufficient wallet balance");
                }

                SwingUtilities.invokeLater(() -> statusLabel.setText("Validating seat availability..."));
                Set<String> unavailable = seatDAO.getUnavailableSeats(
                        BookingContext.scheduleId,
                        BookingContext.fromOrder,
                        BookingContext.toOrder
                );
                for (String seat : seats) {
                    if (unavailable.contains(seat)) {
                        return PaymentResult.fail("Seat " + seat + " already booked");
                    }
                }

                SwingUtilities.invokeLater(() -> statusLabel.setText("Debiting wallet..."));
                if (!walletDAO.deductMoney(Session.userId, amount)) {
                    return PaymentResult.fail("Payment failed");
                }

                SwingUtilities.invokeLater(() -> statusLabel.setText("Confirming ticket(s)..."));
                int generatedTicketId = -1;
                for (String seat : seats) {
                    if (!bookingDAO.isSeatAvailable(
                            BookingContext.scheduleId,
                            seat,
                            BookingContext.fromOrder,
                            BookingContext.toOrder
                    )) {
                        for (Integer createdId : createdBookingIds) {
                            bookingDAO.cancelBooking(createdId);
                        }
                        walletDAO.addMoney(Session.userId, amount);
                        seatLockDAO.releaseSeatLocks(Session.userId);
                        return PaymentResult.fail("Seat " + seat + " became unavailable. Please reselect seats.");
                    }

                    int ticketId = bookingDAO.insertBooking(
                            Session.userId,
                            BookingContext.routeId,
                            BookingContext.scheduleId,
                            BookingContext.fromStop,
                            BookingContext.toStop,
                            BookingContext.fromOrder,
                            BookingContext.toOrder,
                            seat,
                            BookingContext.farePerSeat,
                            BookingContext.journeyDate,
                            BookingContext.passengerName,
                            BookingContext.passengerPhone,
                            BookingContext.passengerEmail
                    );
                    if (ticketId <= 0) {
                        // Best-effort rollback for already-created bookings before refund.
                        for (Integer createdId : createdBookingIds) {
                            bookingDAO.cancelBooking(createdId);
                        }
                        walletDAO.addMoney(Session.userId, amount);
                        seatLockDAO.releaseSeatLocks(Session.userId);
                        String daoError = bookingDAO.getLastErrorMessage();
                        if (daoError == null || daoError.isBlank()) {
                            return PaymentResult.fail("Booking failed");
                        }
                        return PaymentResult.fail("Booking failed: " + daoError);
                    }
                    createdBookingIds.add(ticketId);
                    generatedTicketId = ticketId;
                }

                seatLockDAO.releaseSeatLocks(Session.userId);
                return PaymentResult.success(generatedTicketId);
            }

            @Override
            protected void done() {
                try {
                    PaymentResult result = get();
                    if (result.ok) {
                        BookingContext.ticketId = result.ticketId;
                        setBusy(false, "Payment successful");
                        JOptionPane.showMessageDialog(PaymentPanel.this, "Booking successful");
                        frame.showScreen(MainFrame.SCREEN_SUMMARY);
                    } else {
                        setBusy(false, "");
                        JOptionPane.showMessageDialog(PaymentPanel.this, result.message);
                    }
                } catch (Exception ex) {
                    setBusy(false, "");
                    JOptionPane.showMessageDialog(PaymentPanel.this, "Unexpected error during payment");
                }
            }
        };
        worker.execute();
    }

    @Override
    public void refreshData() {
        tripLabel.setText(BookingContext.fromStop + " -> " + BookingContext.toStop);
        seatsLabel.setText("Seats: " + BookingContext.copySelectedSeats());
        amountLabel.setText("INR " + BookingContext.getFinalAmount());
        setBusy(true, "Loading wallet...");

        SwingWorker<Double, Void> worker = new SwingWorker<>() {
            @Override
            protected Double doInBackground() {
                return new WalletDAO().getBalance(Session.userId);
            }

            @Override
            protected void done() {
                try {
                    walletLabel.setText("Wallet Balance: INR " + get());
                    setBusy(false, "");
                } catch (Exception ex) {
                    walletLabel.setText("Wallet Balance: unavailable");
                    setBusy(false, "");
                }
            }
        };
        worker.execute();
    }

    private static class PaymentResult {
        private final boolean ok;
        private final String message;
        private final int ticketId;

        private PaymentResult(boolean ok, String message, int ticketId) {
            this.ok = ok;
            this.message = message;
            this.ticketId = ticketId;
        }

        private static PaymentResult success(int ticketId) {
            return new PaymentResult(true, "", ticketId);
        }

        private static PaymentResult fail(String message) {
            return new PaymentResult(false, message, -1);
        }
    }
}
