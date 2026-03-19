package ui.booking;

import dao.SearchDAO;
import dao.CityDAO;
import ui.common.MainFrame;
import util.BookingContext;

import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.Timer;

// ✅ CLEAN AWT IMPORTS (NO WILDCARD)
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.text.SimpleDateFormat;

// ✅ CLEAN UTIL IMPORTS (NO AMBIGUITY)
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

public class SearchBusPanel extends JPanel {

    private final MainFrame frame;

    private JTextField sourceField;
    private JTextField destinationField;
    private JDateChooser dateChooser;

    private JPanel resultsPanel;

    private final CityDAO cityDAO = new CityDAO();
    private final Map<String, List<String>> cityCache = new HashMap<>();

    public SearchBusPanel(MainFrame frame) {

        this.frame = frame;

        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(topSearchBar(), BorderLayout.NORTH);
        add(resultsSection(), BorderLayout.CENTER);
    }

    /* ================= SEARCH BAR ================= */

    private JComponent topSearchBar() {

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        sourceField = input("From");
        destinationField = input("To");

        addAutoSuggest(sourceField);
        addAutoSuggest(destinationField);

        JButton swap = new JButton("⇄");
        swap.setFocusPainted(false);

        swap.addActionListener(e -> {
            String tmp = sourceField.getText();
            sourceField.setText(destinationField.getText());
            destinationField.setText(tmp);
        });

        dateChooser = new JDateChooser();
        dateChooser.setDate(new Date());

        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(new Color(220, 53, 69));
        searchBtn.setForeground(Color.WHITE);

        searchBtn.addActionListener(e -> search());

        card.add(sourceField);
        card.add(swap);
        card.add(destinationField);
        card.add(dateChooser);
        card.add(searchBtn);

        wrapper.add(card, BorderLayout.CENTER);

        return wrapper;
    }

    private JTextField input(String title) {
        JTextField f = new JTextField(10);
        f.setBorder(BorderFactory.createTitledBorder(title));
        return f;
    }

    /* ================= AUTO SUGGEST ================= */

    private void addAutoSuggest(JTextField field) {

        JPopupMenu popup = new JPopupMenu();

        Timer debounce = new Timer(300, null);
        debounce.setRepeats(false);

        field.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                debounce.restart();
            }
        });

        debounce.addActionListener(ev -> {

            String text = field.getText().trim().toUpperCase();

            popup.setVisible(false);
            popup.removeAll();

            if (text.length() < 1) return;

            List<String> cities;

            if (cityCache.containsKey(text)) {
                cities = cityCache.get(text);
            } else {
                cities = cityDAO.searchCities(text);
                cityCache.put(text, cities);
            }

            // cache cleanup
            if (cityCache.size() > 50) {
                cityCache.clear();
            }

            for (String city : cities) {

                JMenuItem item = new JMenuItem(city);

                item.addActionListener(a -> {
                    field.setText(city);
                    popup.setVisible(false);
                });

                popup.add(item);
            }

            if (popup.getComponentCount() > 0) {
                popup.show(field, 0, field.getHeight());
            }
        });
    }

    /* ================= RESULTS ================= */

    private JComponent resultsSection() {

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    /* ================= SEARCH ================= */

    private void search() {

        try {

            String src = sourceField.getText().trim().toUpperCase();
            String dst = destinationField.getText().trim().toUpperCase();

            if (src.isEmpty() || dst.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter source & destination");
                return;
            }

            String date = new SimpleDateFormat("yyyy-MM-dd")
                    .format(dateChooser.getDate());

            resultsPanel.removeAll();

            List<String[]> list =
                    new SearchDAO().searchSchedules(src, dst, date);

            if (list.isEmpty()) {

                JLabel no = new JLabel("No buses available 😕");
                no.setAlignmentX(Component.CENTER_ALIGNMENT);

                resultsPanel.add(Box.createVerticalStrut(40));
                resultsPanel.add(no);

            } else {

                for (String[] r : list) {

                    resultsPanel.add(createBusCard(r));
                    resultsPanel.add(Box.createVerticalStrut(15));
                }
            }

            resultsPanel.revalidate();
            resultsPanel.repaint();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= BUS CARD ================= */

    private JPanel createBusCard(String[] r) {

        int scheduleId = Integer.parseInt(r[0]);
        int routeId = Integer.parseInt(r[1]);

        String operator = r[2];
        String busType = r[3];

        double fare = parseDouble(r[4]);

        String dep = r[5];
        String arr = r[6];

        int fromOrder = Integer.parseInt(r[7]);
        int toOrder = Integer.parseInt(r[8]);

        String seats = (r.length > 9) ? r[9] : "0";

        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        /* LEFT */
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel op = new JLabel(operator + " (" + busType + ")");
        op.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel time = new JLabel(dep + " → " + arr);
        time.setForeground(Color.GRAY);

        JLabel seatInfo = new JLabel(seats + " seats left");
        seatInfo.setForeground(new Color(0, 128, 0));

        left.add(op);
        left.add(time);
        left.add(seatInfo);

        /* RIGHT */
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);

        JLabel price = new JLabel("₹ " + fare);
        price.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JButton select = new JButton("View");
        select.setBackground(new Color(220, 53, 69));
        select.setForeground(Color.WHITE);

        select.addActionListener(e -> {

            BookingContext.clear();

            BookingContext.scheduleId = scheduleId;
            BookingContext.routeId = routeId;
            BookingContext.fromOrder = fromOrder;
            BookingContext.toOrder = toOrder;

            BookingContext.fromStop = sourceField.getText().toUpperCase();
            BookingContext.toStop = destinationField.getText().toUpperCase();

            BookingContext.farePerSeat = fare;
            BookingContext.journeyDate =
                    new SimpleDateFormat("yyyy-MM-dd")
                            .format(dateChooser.getDate());

            frame.showScreen(MainFrame.SCREEN_BUS_DETAILS);
        });

        right.add(price, BorderLayout.NORTH);
        right.add(select, BorderLayout.SOUTH);

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);

        return card;
    }

    /* ================= HELPER ================= */

    private double parseDouble(String val) {
        try {
            return Double.parseDouble(val);
        } catch (Exception e) {
            return 0;
        }
    }
}