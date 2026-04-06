package ui.booking;

import config.UIConfig;
import dao.BookingDAO;
import dao.CouponDAO;
import dao.SeatDAO;
import dao.SeatLockDAO;
import dao.WalletDAO;
import ui.common.MainFrame;
import util.BookingContext;
import util.IconUtil;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class PaymentPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final CouponDAO couponDAO = new CouponDAO();

    private JLabel tripLabel;
    private JLabel seatsLabel;
    private JLabel baseAmountLabel;
    private JLabel discountLabel;
    private JLabel amountLabel;
    private JLabel walletLabel;
    private JLabel statusLabel;
    private JLabel loadingGifLabel;
    private JLabel offersHintLabel;
    private JProgressBar progressBar;
    private JTextField couponField;
    private JPanel offersPanel;

    private JButton payBtn;
    private JButton backBtn;
    private JButton applyCouponBtn;
    private JButton clearCouponBtn;

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
        card.setPreferredSize(new Dimension(520, 600));
        UIConfig.styleCard(card);

        tripLabel = centerLabel(UIConfig.FONT_SUBTITLE);
        seatsLabel = centerLabel(UIConfig.FONT_SMALL);
        baseAmountLabel = centerLabel(UIConfig.FONT_SMALL);
        baseAmountLabel.setForeground(UIConfig.TEXT_LIGHT);

        discountLabel = centerLabel(UIConfig.FONT_SMALL);
        discountLabel.setForeground(new Color(22, 163, 74));

        amountLabel = centerLabel(new Font("Segoe UI", Font.BOLD, 28));
        amountLabel.setForeground(UIConfig.SUCCESS);

        walletLabel = centerLabel(UIConfig.FONT_SMALL);
        walletLabel.setForeground(UIConfig.TEXT_LIGHT);

        statusLabel = centerLabel(UIConfig.FONT_SMALL);
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        offersHintLabel = centerLabel(UIConfig.FONT_SMALL);
        offersHintLabel.setForeground(UIConfig.TEXT_LIGHT);

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
        card.add(Box.createVerticalStrut(10));
        card.add(baseAmountLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(discountLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(amountLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(walletLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(couponSection());
        card.add(Box.createVerticalStrut(12));
        card.add(offerListSection());
        card.add(Box.createVerticalStrut(14));
        card.add(loadingGifLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(progressBar);
        card.add(Box.createVerticalStrut(8));
        card.add(statusLabel);

        wrapper.add(card);
        return wrapper;
    }

    private JLabel centerLabel(Font font) {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setFont(font);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private JComponent couponSection() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 94));

        couponField = new JTextField();
        UIConfig.styleField(couponField);
        couponField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        couponField.setBorder(BorderFactory.createTitledBorder("Coupon Code"));
        couponField.addActionListener(e -> applyCoupon());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttons.setOpaque(false);

        applyCouponBtn = new JButton("Apply Coupon");
        UIConfig.secondaryBtn(applyCouponBtn);
        applyCouponBtn.addActionListener(e -> applyCoupon());

        clearCouponBtn = new JButton("Remove Coupon");
        UIConfig.secondaryBtn(clearCouponBtn);
        clearCouponBtn.addActionListener(e -> clearCoupon(true));

        buttons.add(applyCouponBtn);
        buttons.add(clearCouponBtn);

        wrapper.add(couponField);
        wrapper.add(Box.createVerticalStrut(8));
        wrapper.add(buttons);

        return wrapper;
    }

    private JComponent offerListSection() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));

        JLabel title = new JLabel("Available Offers");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(UIConfig.TEXT);

        offersPanel = new JPanel();
        offersPanel.setOpaque(false);
        offersPanel.setLayout(new BoxLayout(offersPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(offersPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 234, 240)));
        scrollPane.setPreferredSize(new Dimension(0, 160));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        wrapper.add(title, BorderLayout.NORTH);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        wrapper.add(offersHintLabel, BorderLayout.SOUTH);

        return wrapper;
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
        setCouponControlsEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void setCouponControlsEnabled(boolean enabled) {
        if (couponField != null) {
            couponField.setEnabled(enabled);
        }
        if (applyCouponBtn != null) {
            applyCouponBtn.setEnabled(enabled);
        }
        if (clearCouponBtn != null) {
            clearCouponBtn.setEnabled(enabled && BookingContext.couponCode != null && !BookingContext.couponCode.isBlank());
        }
    }

    private void updateAmountSummary() {
        double baseAmount = BookingContext.getBaseAmount();
        double effectiveDiscount = BookingContext.getEffectiveDiscount();

        baseAmountLabel.setText("Base Fare: INR " + String.format("%.2f", baseAmount));
        if (BookingContext.couponCode == null || BookingContext.couponCode.isBlank()) {
            discountLabel.setText("Coupon Discount: not applied");
        } else {
            discountLabel.setText(
                    "Coupon " + BookingContext.couponCode + ": -INR " + String.format("%.2f", effectiveDiscount)
            );
        }
        amountLabel.setText("Payable: INR " + String.format("%.2f", BookingContext.getFinalAmount()));

        if (couponField != null) {
            couponField.setText(BookingContext.couponCode == null ? "" : BookingContext.couponCode);
        }
        setCouponControlsEnabled(couponField == null || couponField.isEnabled());
    }

    private void applyCoupon() {
        String code = couponField == null || couponField.getText() == null
                ? ""
                : couponField.getText().trim().toUpperCase();

        if (code.isBlank()) {
            JOptionPane.showMessageDialog(this, "Enter a coupon code first.");
            return;
        }
        if (BookingContext.getBaseAmount() <= 0) {
            JOptionPane.showMessageDialog(this, "Select seats before applying a coupon.");
            return;
        }

        setCouponControlsEnabled(false);
        statusLabel.setText("Validating coupon...");

        SwingWorker<CouponDAO.CouponValidation, Void> worker = new SwingWorker<>() {
            @Override
            protected CouponDAO.CouponValidation doInBackground() {
                return couponDAO.validateCouponForUser(code, Session.userId, BookingContext.getBaseAmount());
            }

            @Override
            protected void done() {
                try {
                    CouponDAO.CouponValidation result = get();
                    if (result.valid && result.offer != null) {
                        BookingContext.applyCoupon(result.offer.code, result.offer.discountAmount);
                        updateAmountSummary();
                        statusLabel.setText("Coupon applied successfully.");
                        refreshOffersAsync(false);
                    } else {
                        statusLabel.setText("Coupon not applied.");
                        JOptionPane.showMessageDialog(
                                PaymentPanel.this,
                                result.message == null || result.message.isBlank()
                                        ? "Invalid or expired coupon."
                                        : result.message
                        );
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Coupon not applied.");
                    JOptionPane.showMessageDialog(PaymentPanel.this, "Unable to validate coupon right now.");
                } finally {
                    setCouponControlsEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void clearCoupon(boolean notifyUser) {
        BookingContext.clearCoupon();
        updateAmountSummary();
        if (notifyUser) {
            statusLabel.setText("Coupon removed.");
        }
    }

    private void renderOffers(List<CouponDAO.CouponOffer> offers) {
        offersPanel.removeAll();

        if (offers == null || offers.isEmpty()) {
            offersPanel.add(offerMessage("No coupon offers available right now."));
            offersHintLabel.setText("Offers will appear here when active coupons are available.");
        } else {
            int shown = 0;
            for (CouponDAO.CouponOffer offer : offers) {
                offersPanel.add(offerRow(offer));
                offersPanel.add(Box.createVerticalStrut(8));
                shown++;
                if (shown >= 5) {
                    break;
                }
            }
            offersHintLabel.setText("One-time coupons will show as used after redemption.");
        }

        offersPanel.revalidate();
        offersPanel.repaint();
    }

    private JComponent offerMessage(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIConfig.FONT_SMALL);
        label.setForeground(UIConfig.TEXT_LIGHT);
        label.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        return label;
    }

    private JComponent offerRow(CouponDAO.CouponOffer offer) {
        JPanel row = new JPanel(new BorderLayout(8, 8));
        row.setBackground(new Color(250, 250, 252));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 236, 240)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel code = new JLabel(offer.code + "  |  Save INR " + String.format("%.2f", offer.discountAmount));
        code.setFont(new Font("Segoe UI", Font.BOLD, 13));
        code.setForeground(UIConfig.TEXT);

        JLabel helper = new JLabel(offer.helperText == null ? "" : offer.helperText);
        helper.setFont(UIConfig.FONT_SMALL);
        helper.setForeground(UIConfig.TEXT_LIGHT);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badges.setOpaque(false);

        JLabel badge = badgeLabel(offer.badgeText == null ? "Offer" : offer.badgeText,
                offer.eligible ? new Color(220, 252, 231) : new Color(241, 245, 249),
                offer.eligible ? new Color(21, 128, 61) : new Color(71, 85, 105));
        badges.add(badge);

        if (offer.minBookingAmount > 0) {
            badges.add(badgeLabel(
                    "Min INR " + String.format("%.0f", offer.minBookingAmount),
                    new Color(255, 247, 237),
                    new Color(194, 65, 12)
            ));
        }

        left.add(code);
        left.add(Box.createVerticalStrut(4));
        left.add(helper);
        left.add(Box.createVerticalStrut(6));
        left.add(badges);

        JButton action = new JButton();
        action.setPreferredSize(new Dimension(120, 36));

        boolean alreadySelected = offer.code != null && offer.code.equalsIgnoreCase(BookingContext.couponCode);
        if (offer.eligible && !alreadySelected) {
            action.setText("Apply");
            UIConfig.primaryBtn(action);
            action.addActionListener(e -> {
                couponField.setText(offer.code);
                applyCoupon();
            });
        } else if (alreadySelected) {
            action.setText("Applied");
            UIConfig.secondaryBtn(action);
            action.setEnabled(false);
        } else {
            action.setText(offer.usedUpByUser ? "Used" : "Locked");
            UIConfig.secondaryBtn(action);
            action.setEnabled(false);
        }

        row.add(left, BorderLayout.CENTER);
        row.add(action, BorderLayout.EAST);
        return row;
    }

    private JLabel badgeLabel(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return label;
    }

    private void refreshOffersAsync(boolean showBusyState) {
        if (showBusyState) {
            setBusy(true, "Loading wallet and offers...");
        } else {
            offersHintLabel.setText("Refreshing offers...");
        }

        SwingWorker<PaymentRefreshData, Void> worker = new SwingWorker<>() {
            @Override
            protected PaymentRefreshData doInBackground() {
                PaymentRefreshData data = new PaymentRefreshData();
                WalletDAO walletDAO = new WalletDAO();
                data.balance = walletDAO.getBalance(Session.userId);
                data.offers = couponDAO.getPersonalizedOffers(Session.userId, BookingContext.getBaseAmount(), 6);
                return data;
            }

            @Override
            protected void done() {
                try {
                    PaymentRefreshData data = get();
                    walletLabel.setText("Wallet Balance: INR " + String.format("%.2f", data.balance));

                    if (BookingContext.couponCode != null && !BookingContext.couponCode.isBlank()) {
                        CouponDAO.CouponValidation validation =
                                couponDAO.validateCouponForUser(BookingContext.couponCode, Session.userId, BookingContext.getBaseAmount());
                        if (validation.valid && validation.offer != null) {
                            BookingContext.applyCoupon(validation.offer.code, validation.offer.discountAmount);
                        } else {
                            clearCoupon(false);
                            statusLabel.setText("Previous coupon is no longer valid for this booking.");
                        }
                    }

                    renderOffers(data.offers);
                    updateAmountSummary();
                    setBusy(false, "");
                } catch (Exception ex) {
                    walletLabel.setText("Wallet Balance: unavailable");
                    renderOffers(new ArrayList<>());
                    setBusy(false, "Unable to refresh wallet or offers.");
                }
            }
        };
        worker.execute();
    }

    private void doPayment() {
        if (!BookingContext.isReadyForPayment()) {
            JOptionPane.showMessageDialog(this, "Booking data incomplete");
            return;
        }

        BookingContext.clearRecentTicketIds();
        final double baseAmount = BookingContext.getBaseAmount();
        final double amount = BookingContext.getFinalAmount();
        final Set<String> seats = BookingContext.copySelectedSeats();
        final String selectedCoupon = BookingContext.couponCode == null ? "" : BookingContext.couponCode.trim().toUpperCase();

        setBusy(true, "Processing payment...");

        SwingWorker<PaymentResult, Void> worker = new SwingWorker<>() {
            @Override
            protected PaymentResult doInBackground() {
                WalletDAO walletDAO = new WalletDAO();
                BookingDAO bookingDAO = new BookingDAO();
                SeatLockDAO seatLockDAO = new SeatLockDAO();
                SeatDAO seatDAO = new SeatDAO();
                List<Integer> createdBookingIds = new ArrayList<>();
                List<String> orderedSeats = new ArrayList<>(seats);
                orderedSeats.sort(Comparator.comparingInt(PaymentPanel::seatSortKey).thenComparing(String::compareToIgnoreCase));
                String orderRef = "ORD-" + Session.userId + "-" + System.currentTimeMillis();
                CouponDAO.CouponValidation couponValidation = null;

                if (!selectedCoupon.isBlank()) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Validating coupon..."));
                    couponValidation = couponDAO.validateCouponForUser(selectedCoupon, Session.userId, baseAmount);
                    if (!couponValidation.valid || couponValidation.offer == null) {
                        return PaymentResult.fail(
                                couponValidation == null || couponValidation.message == null || couponValidation.message.isBlank()
                                        ? "Selected coupon is no longer valid."
                                        : couponValidation.message
                        );
                    }
                }

                SwingUtilities.invokeLater(() -> statusLabel.setText("Checking wallet balance..."));
                if (walletDAO.getBalance(Session.userId) < amount) {
                    return PaymentResult.fail("Insufficient wallet balance");
                }

                SwingUtilities.invokeLater(() -> statusLabel.setText("Validating seat availability..."));
                seatDAO.clearLastError();
                Set<String> unavailable = seatDAO.getUnavailableSeats(
                        BookingContext.scheduleId,
                        BookingContext.fromOrder,
                        BookingContext.toOrder
                );
                if (seatDAO.hasLastError()) {
                    return PaymentResult.fail(seatDAO.getLastError());
                }

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
                for (String seat : orderedSeats) {
                    if (!bookingDAO.isSeatAvailable(
                            BookingContext.scheduleId,
                            seat,
                            BookingContext.fromOrder,
                            BookingContext.toOrder
                    )) {
                        rollbackBooking(walletDAO, bookingDAO, seatLockDAO, createdBookingIds, amount);
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
                            BookingContext.passengerEmail,
                            orderRef
                    );
                    if (ticketId <= 0) {
                        rollbackBooking(walletDAO, bookingDAO, seatLockDAO, createdBookingIds, amount);
                        String daoError = bookingDAO.getLastErrorMessage();
                        return PaymentResult.fail(
                                daoError == null || daoError.isBlank()
                                        ? "Booking failed"
                                        : "Booking failed: " + daoError
                        );
                    }
                    createdBookingIds.add(ticketId);
                }

                if (createdBookingIds.isEmpty()) {
                    rollbackBooking(walletDAO, bookingDAO, seatLockDAO, createdBookingIds, amount);
                    return PaymentResult.fail("Booking failed");
                }

                if (couponValidation != null && couponValidation.offer != null) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Saving coupon usage..."));
                    boolean usageSaved = couponDAO.recordCouponUsage(
                            couponValidation.offer.id,
                            Session.userId,
                            orderRef,
                            couponValidation.offer.code,
                            couponValidation.offer.discountAmount,
                            baseAmount,
                            amount
                    );
                    if (!usageSaved) {
                        rollbackBooking(walletDAO, bookingDAO, seatLockDAO, createdBookingIds, amount);
                        String couponError = couponDAO.getLastError();
                        return PaymentResult.fail(
                                couponError == null || couponError.isBlank()
                                        ? "Coupon usage could not be saved. Payment was reversed."
                                        : couponError
                        );
                    }
                }

                seatLockDAO.releaseSeatLocks(Session.userId);
                return PaymentResult.success(createdBookingIds);
            }

            @Override
            protected void done() {
                try {
                    PaymentResult result = get();
                    if (result.ok) {
                        BookingContext.setRecentTicketIds(result.ticketIds);
                        String successMessage = result.ticketIds.size() > 1
                                ? "Booking successful. " + result.ticketIds.size() + " tickets created."
                                : "Booking successful";
                        setBusy(false, "Payment successful");
                        JOptionPane.showMessageDialog(PaymentPanel.this, successMessage);
                        frame.showScreen(MainFrame.SCREEN_SUMMARY);
                    } else {
                        setBusy(false, result.message);
                        JOptionPane.showMessageDialog(PaymentPanel.this, result.message);
                        refreshOffersAsync(false);
                    }
                } catch (Exception ex) {
                    setBusy(false, "");
                    JOptionPane.showMessageDialog(PaymentPanel.this, "Unexpected error during payment");
                }
            }
        };
        worker.execute();
    }

    private static int seatSortKey(String seatNo) {
        try {
            return Integer.parseInt(seatNo.replaceAll("\\D", ""));
        } catch (Exception ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private void rollbackBooking(WalletDAO walletDAO,
                                 BookingDAO bookingDAO,
                                 SeatLockDAO seatLockDAO,
                                 List<Integer> createdBookingIds,
                                 double amount) {
        for (Integer createdId : createdBookingIds) {
            bookingDAO.cancelBooking(createdId);
        }
        walletDAO.addMoney(Session.userId, amount);
        seatLockDAO.releaseSeatLocks(Session.userId);
    }

    @Override
    public void refreshData() {
        tripLabel.setText(BookingContext.fromStop + " -> " + BookingContext.toStop);
        seatsLabel.setText("Seats: " + BookingContext.getSeatListString());
        updateAmountSummary();
        refreshOffersAsync(true);
    }

    private static class PaymentRefreshData {
        double balance;
        List<CouponDAO.CouponOffer> offers = new ArrayList<>();
    }

    private static class PaymentResult {
        private final boolean ok;
        private final String message;
        private final List<Integer> ticketIds;

        private PaymentResult(boolean ok, String message, List<Integer> ticketIds) {
            this.ok = ok;
            this.message = message;
            this.ticketIds = ticketIds == null ? new ArrayList<>() : new ArrayList<>(ticketIds);
        }

        private static PaymentResult success(List<Integer> ticketIds) {
            return new PaymentResult(true, "", ticketIds);
        }

        private static PaymentResult fail(String message) {
            return new PaymentResult(false, message, new ArrayList<>());
        }
    }
}
