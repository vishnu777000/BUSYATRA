package ui.clerk;

import config.UIConfig;
import dao.ReportsDAO;
import ui.common.MainFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

public class BookingClerkDashboard extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private final MainFrame frame;

    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    public BookingClerkDashboard(MainFrame frame) {

        this.frame = frame;

        setLayout(new BorderLayout(16, 16));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(tableCard(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);

        loadData();
    }

    /* ================= HEADER ================= */

    private JComponent header() {

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel title = new JLabel("Booking Counter");
        title.setFont(UIConfig.FONT_TITLE);

        JLabel sub = new JLabel(
                "Create bookings • Assist passengers • Manage tickets"
        );

        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(UIConfig.TEXT_LIGHT);

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);

        left.add(title);
        left.add(sub);

        p.add(left, BorderLayout.WEST);

        return p;
    }

    /* ================= TABLE ================= */

    private JComponent tableCard() {

        model = new DefaultTableModel(
                new Object[]{
                        "Ticket ID",
                        "Passenger",
                        "Route",
                        "Amount (₹)",
                        "Status",
                        "Booking Time"
                }, 0
        ) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(model);

        UIConfig.styleTable(table);
        table.setRowHeight(32);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);

        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);

        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    /* ================= ACTIONS ================= */

    private JComponent actions() {

        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        p.setOpaque(false);

        JButton newBooking = new JButton("New Booking");
        UIConfig.primaryBtn(newBooking);

        newBooking.addActionListener(
                e -> frame.showScreen(MainFrame.SCREEN_SEARCH)
        );

        JButton refresh = new JButton("Refresh");
        UIConfig.secondaryBtn(refresh);

        refresh.addActionListener(e -> loadData());

        p.add(newBooking);
        p.add(refresh);

        return p;
    }

    /* ================= LOAD DATA ================= */

    private void loadData() {

        model.setRowCount(0);

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {

            List<String[]> list = new ReportsDAO().getAllBookings();

            if (list.isEmpty()) {

                model.addRow(new Object[]{
                        "-", "No bookings found", "-", "-", "-", "-"
                });

            } else {

                for (String[] r : list) {

                    String route = r[2] + " → " + r[3];

                    double amount;
                    try {
                        amount = Double.parseDouble(r[4]);
                    } catch (Exception e) {
                        amount = 0;
                    }

                    model.addRow(new Object[]{
                            r[0], // id
                            r[1], // name
                            route,
                            "₹ " + df.format(amount),
                            r[5], // status
                            r[6]  // time
                    });
                }
            }

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to load bookings"
            );

            e.printStackTrace();
        }

        setCursor(Cursor.getDefaultCursor());
    }
}