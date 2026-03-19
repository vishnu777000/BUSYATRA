package ui.dashboard;

import dao.BookingDAO;
import dao.WalletDAO;
import dao.NewsDAO;

import ui.common.MainFrame;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class UserDashboard extends JPanel implements Refreshable {

    private final MainFrame frame;

    private JLabel walletValue;
    private JLabel journeyValue;

    public UserDashboard(MainFrame frame) {

        this.frame = frame;

        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        add(scrollContent(), BorderLayout.CENTER);

        refreshData();
    }

    /* ================= SCROLL ================= */

    private JScrollPane scrollContent() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(getBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        panel.add(heroSection()); // 🔥 NEW HERO
        panel.add(Box.createVerticalStrut(20));

        panel.add(statsSection());
        panel.add(Box.createVerticalStrut(20));

        panel.add(actionsSection());
        panel.add(Box.createVerticalStrut(20));

        panel.add(newsSection());

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);

        return scroll;
    }

    /* ================= HERO ================= */

    private JPanel heroSection() {

        JPanel hero = new JPanel(new BorderLayout(15, 10));
        hero.setBackground(new Color(220, 53, 69));
        hero.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Book Your Journey");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        row.setOpaque(false);

        JTextField from = new JTextField(10);
        from.setBorder(BorderFactory.createTitledBorder("From"));

        JTextField to = new JTextField(10);
        to.setBorder(BorderFactory.createTitledBorder("To"));

        JTextField date = new JTextField(8);
        date.setBorder(BorderFactory.createTitledBorder("Date"));

        JButton search = new JButton("Search");
        search.setBackground(Color.WHITE);
        search.setForeground(new Color(220, 53, 69));
        search.setFocusPainted(false);

        search.addActionListener(e -> frame.showScreen(MainFrame.SCREEN_SEARCH));

        row.add(from);
        row.add(to);
        row.add(date);
        row.add(search);

        hero.add(title, BorderLayout.NORTH);
        hero.add(row, BorderLayout.CENTER);

        return hero;
    }

    /* ================= STATS ================= */

    private JPanel statsSection() {

        JPanel row = new JPanel(new GridLayout(1, 3, 20, 0));
        row.setOpaque(false);

        walletValue = bigText("₹ 0");
        journeyValue = bigText("No Trips");

        row.add(statCard("Wallet", walletValue));
        row.add(statCard("Upcoming", journeyValue));
        row.add(statCard("Bookings", bigText("0")));

        return row;
    }

    private JPanel statCard(String title, JLabel value) {

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel t = new JLabel(title);
        t.setForeground(Color.GRAY);

        card.add(t, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }

    private JLabel bigText(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 20));
        return l;
    }

    /* ================= ACTIONS ================= */

    private JPanel actionsSection() {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        panel.setOpaque(false);

        panel.add(actionBtn("🎟 Book Ticket", MainFrame.SCREEN_SEARCH));
        panel.add(actionBtn("📄 My Tickets", MainFrame.SCREEN_MY_TICKETS));
        panel.add(actionBtn("❌ Cancel", MainFrame.SCREEN_CANCEL));
        panel.add(actionBtn("💰 Wallet", MainFrame.SCREEN_WALLET));

        return panel;
    }

    private JButton actionBtn(String text, String screen) {

        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(220, 53, 69));
        btn.setBorder(BorderFactory.createLineBorder(new Color(220, 53, 69), 1));
        btn.setPreferredSize(new Dimension(150, 40));

        btn.addActionListener(e -> frame.showScreen(screen));

        return btn;
    }

    /* ================= NEWS ================= */

    private JPanel newsSection() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel title = new JLabel("📰 Latest Updates");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        List<String[]> newsList = new NewsDAO().getActiveNews();

        if (newsList.isEmpty()) {
            panel.add(new JLabel("No updates available"));
        } else {
            for (String[] news : newsList) {
                panel.add(newsItem(news[1], news[2]));
            }
        }

        return panel;
    }

    private JPanel newsItem(String message, String date) {

        JPanel item = new JPanel(new BorderLayout());
        item.setOpaque(false);

        JLabel msg = new JLabel("• " + message);
        JLabel dt = new JLabel(date);
        dt.setForeground(Color.GRAY);

        item.add(msg, BorderLayout.CENTER);
        item.add(dt, BorderLayout.EAST);

        return item;
    }

    /* ================= DATA ================= */

    public void refreshData() {

        double balance = new WalletDAO().getBalance(Session.userId);
        walletValue.setText("₹ " + String.format("%.2f", balance));

        String upcoming = new BookingDAO().getUpcomingBookingText(Session.userId);

        if (upcoming != null && !upcoming.isEmpty())
            journeyValue.setText(upcoming);
    }
}