package ui.booking;

import config.UIConfig;
import dao.SeatDAO;
import dao.SeatLockDAO;
import ui.common.MainFrame;
import ui.common.SeatButton;
import util.BookingContext;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeatSelectionPanel extends JPanel implements Refreshable {

    private final MainFrame frame;

    private final SeatDAO seatDAO = new SeatDAO();
    private final SeatLockDAO seatLockDAO = new SeatLockDAO();

    private Set<String> unavailableSeats = new HashSet<>();
    private final Set<String> selectedSeats = new HashSet<>();

    private static final int MAX_SEATS = 6;

    private JLabel payableLabel;
    private JLabel selectedLabel;
    private JLabel loadingLabel;

    private final Color AVAILABLE = new Color(232, 245, 233);
    private final Color SELECTED = new Color(0, 150, 136);
    private final Color BOOKED = new Color(220, 53, 69);

    public SeatSelectionPanel(MainFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout(15, 15));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(centerPanel(), BorderLayout.CENTER);
        add(bottomBar(), BorderLayout.SOUTH);
    }

    private JComponent header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton back = new JButton("< Back");
        UIConfig.secondaryBtn(back);
        back.addActionListener(e -> frame.goBack());

        JLabel title = new JLabel(
                BookingContext.fromStop + " -> " + BookingContext.toStop,
                SwingConstants.CENTER
        );
        title.setFont(UIConfig.FONT_SUBTITLE);

        panel.add(back, BorderLayout.WEST);
        panel.add(title, BorderLayout.CENTER);
        panel.add(legend(), BorderLayout.EAST);

        return panel;
    }

    private JPanel legend() {
        JPanel legend = new JPanel(new FlowLayout());
        legend.setOpaque(false);

        legend.add(colorBox(AVAILABLE, "Available"));
        legend.add(colorBox(SELECTED, "Selected"));
        legend.add(colorBox(BOOKED, "Booked/Locked"));

        return legend;
    }

    private JPanel colorBox(Color color, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);

        JLabel box = new JLabel();
        box.setOpaque(true);
        box.setBackground(color);
        box.setPreferredSize(new Dimension(14, 14));

        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConfig.FONT_SMALL);

        p.add(box);
        p.add(lbl);

        return p;
    }

    private JComponent centerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        panel.add(
                new RouteTimelinePanel(
                        BookingContext.routeId,
                        BookingContext.scheduleId,
                        BookingContext.fromStop,
                        BookingContext.toStop
                ),
                BorderLayout.NORTH
        );

        panel.add(scrollSeats(), BorderLayout.CENTER);
        return panel;
    }

    private JComponent loadingCenter() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        loadingLabel = new JLabel("Loading seat configuration...");
        loadingLabel.setFont(UIConfig.FONT_SUBTITLE);
        loadingLabel.setForeground(UIConfig.TEXT_LIGHT);
        panel.add(loadingLabel);

        return panel;
    }

    private JComponent scrollSeats() {
        JScrollPane sp = new JScrollPane(seatLayout());
        sp.setBorder(null);
        sp.getViewport().setBackground(UIConfig.BACKGROUND);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JPanel seatLayout() {
        JPanel busBody = new JPanel();
        busBody.setLayout(new BoxLayout(busBody, BoxLayout.Y_AXIS));
        busBody.setBackground(Color.WHITE);
        busBody.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                BorderFactory.createEmptyBorder(20, 28, 22, 28)
        ));

        JPanel topStrip = new JPanel(new BorderLayout());
        topStrip.setOpaque(false);

        JLabel left = new JLabel("Driver", SwingConstants.CENTER);
        left.setOpaque(true);
        left.setBackground(new Color(232, 236, 240));
        left.setFont(new Font("Segoe UI", Font.BOLD, 12));
        left.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));

        JLabel right = new JLabel("Door", SwingConstants.CENTER);
        right.setOpaque(true);
        right.setBackground(new Color(246, 248, 250));
        right.setFont(UIConfig.FONT_SMALL);
        right.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));

        topStrip.add(left, BorderLayout.WEST);
        topStrip.add(right, BorderLayout.EAST);

        busBody.add(topStrip);
        busBody.add(Box.createVerticalStrut(14));

        int totalSeats = Math.max(0, BookingContext.totalSeats);
        if (totalSeats == 0) {
            JLabel noSeats = new JLabel("No seat configuration available for this bus", SwingConstants.CENTER);
            noSeats.setFont(UIConfig.FONT_SMALL);
            noSeats.setForeground(UIConfig.TEXT_LIGHT);
            noSeats.setAlignmentX(Component.CENTER_ALIGNMENT);
            busBody.add(noSeats);
        } else {
            for (JComponent row : buildSeatRows(totalSeats)) {
                busBody.add(row);
                busBody.add(Box.createVerticalStrut(8));
            }
        }

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapper.setOpaque(false);
        wrapper.add(busBody);
        return wrapper;
    }

    private List<JComponent> buildSeatRows(int totalSeats) {
        List<JComponent> rows = new ArrayList<>();

        int fullRows = totalSeats / 4;
        int remainder = totalSeats % 4;
        int seatNumber = 1;

        for (int rowNumber = 1; rowNumber <= fullRows; rowNumber++) {
            rows.add(seatRow(rowNumber, seatNumber, 4));
            seatNumber += 4;
        }

        if (remainder > 0) {
            rows.add(seatRow(fullRows + 1, seatNumber, remainder));
        }

        return rows;
    }

    private JComponent seatRow(int rowNumber, int seatStart, int count) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel rowLabel = new JLabel(String.format("R%02d", rowNumber));
        rowLabel.setFont(UIConfig.FONT_SMALL);
        rowLabel.setForeground(UIConfig.TEXT_LIGHT);
        rowLabel.setPreferredSize(new Dimension(34, 34));

        JPanel seatsWrap = new JPanel(new GridBagLayout());
        seatsWrap.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);

        int added = 0;

        c.gridx = 0;
        if (added < count) {
            seatsWrap.add(seatBtn("S" + (seatStart + added)), c);
            added++;
        }

        c.gridx = 1;
        if (added < count) {
            seatsWrap.add(seatBtn("S" + (seatStart + added)), c);
            added++;
        }

        c.gridx = 2;
        seatsWrap.add(Box.createHorizontalStrut(56), c);

        c.gridx = 3;
        if (added < count) {
            seatsWrap.add(seatBtn("S" + (seatStart + added)), c);
            added++;
        }

        c.gridx = 4;
        if (added < count) {
            seatsWrap.add(seatBtn("S" + (seatStart + added)), c);
        }

        row.add(rowLabel, BorderLayout.WEST);
        row.add(seatsWrap, BorderLayout.CENTER);

        return row;
    }

    private JButton seatBtn(String seatNo) {
        SeatButton btn = new SeatButton(seatNo);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));

        if (unavailableSeats.contains(seatNo)) {
            btn.setState(SeatButton.State.BOOKED);
            btn.setEnabled(false);
            return btn;
        }

        btn.setState(SeatButton.State.AVAILABLE);
        btn.addActionListener(e -> toggleSeat(btn, seatNo));

        return btn;
    }

    private void toggleSeat(SeatButton btn, String seatNo) {
        if (selectedSeats.contains(seatNo)) {
            selectedSeats.remove(seatNo);
            BookingContext.removeSeat(seatNo);

            seatLockDAO.releaseSeatLock(
                    BookingContext.scheduleId,
                    seatNo,
                    Session.userId
            );

            btn.setState(SeatButton.State.AVAILABLE);

        } else {
            if (selectedSeats.size() >= MAX_SEATS) {
                JOptionPane.showMessageDialog(this, "Max 6 seats allowed");
                return;
            }

            if (seatLockDAO.isSeatLocked(
                    BookingContext.scheduleId,
                    seatNo,
                    BookingContext.fromOrder,
                    BookingContext.toOrder
            )) {
                JOptionPane.showMessageDialog(this, "Seat already locked by another user");
                return;
            }

            boolean locked = seatLockDAO.lockSeat(
                    BookingContext.scheduleId,
                    seatNo,
                    Session.userId,
                    BookingContext.fromOrder,
                    BookingContext.toOrder
            );

            if (!locked) {
                JOptionPane.showMessageDialog(this, "Unable to hold seat right now");
                return;
            }

            selectedSeats.add(seatNo);
            BookingContext.addSeat(seatNo);
            btn.setState(SeatButton.State.SELECTED);
        }

        updatePayable();
    }

    private void updatePayable() {
        selectedLabel.setText("Seats : " + BookingContext.getSeatListString());
        payableLabel.setText("Total : INR " + String.format("%.2f", BookingContext.getFinalAmount()));
    }

    private JComponent bottomBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);

        selectedLabel = new JLabel("Seats : -");
        selectedLabel.setFont(UIConfig.FONT_SMALL);

        payableLabel = new JLabel("Total : INR 0.00");
        payableLabel.setFont(UIConfig.FONT_SUBTITLE);

        left.add(selectedLabel);
        left.add(payableLabel);

        JButton proceed = new JButton("Proceed to Payment");
        UIConfig.primaryBtn(proceed);

        proceed.addActionListener(e -> {
            if (selectedSeats.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Select at least one seat");
                return;
            }
            frame.showScreen(MainFrame.SCREEN_PASSENGER);
        });

        panel.add(left, BorderLayout.WEST);
        panel.add(proceed, BorderLayout.EAST);

        return panel;
    }

    @Override
    public void refreshData() {
        BookingContext.clearSeats();
        selectedSeats.clear();
        removeAll();
        add(header(), BorderLayout.NORTH);
        add(loadingCenter(), BorderLayout.CENTER);
        add(bottomBar(), BorderLayout.SOUTH);
        revalidate();
        repaint();

        SwingWorker<SeatLoadResult, Void> worker = new SwingWorker<>() {
            @Override
            protected SeatLoadResult doInBackground() {
                SeatLoadResult result = new SeatLoadResult();
                seatLockDAO.clearExpiredLocks();
                result.unavailable = seatDAO.getUnavailableSeats(
                        BookingContext.scheduleId,
                        BookingContext.fromOrder,
                        BookingContext.toOrder
                );
                result.unavailable.addAll(
                        seatLockDAO.getLockedSeats(
                                BookingContext.scheduleId,
                                BookingContext.fromOrder,
                                BookingContext.toOrder
                        )
                );
                result.totalSeats = seatDAO.getTotalSeatsBySchedule(BookingContext.scheduleId);
                return result;
            }

            @Override
            protected void done() {
                try {
                    SeatLoadResult result = get();
                    unavailableSeats = result.unavailable;
                    BookingContext.totalSeats = result.totalSeats;
                } catch (Exception e) {
                    unavailableSeats = new HashSet<>();
                    BookingContext.totalSeats = 0;
                }

                removeAll();
                add(header(), BorderLayout.NORTH);
                add(centerPanel(), BorderLayout.CENTER);
                add(bottomBar(), BorderLayout.SOUTH);
                revalidate();
                repaint();
            }
        };
        worker.execute();
    }

    private static class SeatLoadResult {
        Set<String> unavailable = new HashSet<>();
        int totalSeats;
    }
}
