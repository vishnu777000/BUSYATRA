package ui.accounts;

import config.UIConfig;
import dao.AccountsDAO;
import util.Refreshable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AccountsDashboard extends JPanel implements Refreshable {

    private double bookedRevenue;
    private double refundedAmount;

    private ChartType chartType = ChartType.PIE;
    private ChartPanel chartPanel;

    enum ChartType { PIE, BAR }

    public AccountsDashboard() {

        setLayout(new BorderLayout(20,20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        refreshData();
    }

    /* ================= HEADER ================= */

    private JPanel header() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        left.setOpaque(false);

        JLabel logo = new JLabel(
                new ImageIcon(getClass().getResource("/resources/icons/app/buslogo.png"))
        );

        JLabel title = new JLabel("Accounts Dashboard");
        title.setFont(UIConfig.FONT_TITLE);

        left.add(logo);
        left.add(title);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,0));
        actions.setOpaque(false);

        JButton pieBtn = createBtn("Pie");
        JButton barBtn = createBtn("Bar");

        pieBtn.addActionListener(e -> switchChart(ChartType.PIE));
        barBtn.addActionListener(e -> switchChart(ChartType.BAR));

        actions.add(pieBtn);
        actions.add(barBtn);

        panel.add(left, BorderLayout.WEST);
        panel.add(actions, BorderLayout.EAST);

        return panel;
    }

    private JButton createBtn(String text){

        JButton b = new JButton(text);
        UIConfig.primaryBtn(b);
        b.setPreferredSize(new Dimension(110,36));

        return b;
    }

    private void switchChart(ChartType type){
        chartType = type;
        chartPanel.repaint();
    }

    /* ================= MAIN ================= */

    private JPanel mainPanel(){

        JPanel panel = new JPanel(new BorderLayout(20,20));
        panel.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1,3,20,20));
        cards.setOpaque(false);

        cards.add(createCard("Booked Revenue", bookedRevenue,
                "/resources/icons/actions/payment.png", UIConfig.SUCCESS));

        cards.add(createCard("Refunded Amount", refundedAmount,
                "/resources/icons/status/warning.png", UIConfig.DANGER));

        cards.add(createCard("Net Revenue", bookedRevenue - refundedAmount,
                "/resources/icons/actions/download.png", UIConfig.PRIMARY));

        chartPanel = new ChartPanel();
        UIConfig.styleCard(chartPanel);

        panel.add(cards, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCard(String title, double value,
                              String iconPath, Color color){

        JPanel card = new JPanel(new BorderLayout(10,10));
        UIConfig.styleCard(card);

        JLabel icon = new JLabel(
                new ImageIcon(getClass().getResource(iconPath))
        );

        JLabel t = new JLabel(title);
        t.setFont(UIConfig.FONT_SMALL);

        JLabel v = new JLabel("₹ " + String.format("%.2f", value));
        v.setFont(new Font("Segoe UI", Font.BOLD, 22));
        v.setForeground(color);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(icon, BorderLayout.WEST);
        top.add(t, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);

        // Hover
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e){
                card.setBackground(new Color(245,245,245));
            }
            public void mouseExited(MouseEvent e){
                card.setBackground(Color.WHITE);
            }
        });

        return card;
    }

    /* ================= DATA ================= */

    @Override
    public void refreshData() {

        AccountsDAO dao = new AccountsDAO();

        bookedRevenue = dao.getTotalRevenueBooked();
        refundedAmount = dao.getTotalRefunded();

        removeAll();
        add(header(), BorderLayout.NORTH);
        add(mainPanel(), BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /* ================= CHART ================= */

    class ChartPanel extends JPanel {

        protected void paintComponent(Graphics g){
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            if(chartType == ChartType.PIE){
                drawPie(g2);
            }else{
                drawBar(g2);
            }
        }

        private void drawPie(Graphics2D g){

            double total = bookedRevenue + refundedAmount;
            if(total == 0) total = 1;

            int bookedAngle = (int)((bookedRevenue * 360)/total);

            int size = Math.min(getWidth(), getHeight()) - 120;
            int x = (getWidth() - size)/2;
            int y = 80;

            g.setFont(UIConfig.FONT_SUBTITLE);
            g.drawString("Revenue Distribution", 20, 30);

            g.setColor(UIConfig.SUCCESS);
            g.fillArc(x,y,size,size,0,bookedAngle);

            g.setColor(UIConfig.DANGER);
            g.fillArc(x,y,size,size,bookedAngle,360-bookedAngle);

            // Labels
            g.setColor(Color.BLACK);
            g.drawString("Booked: ₹" + bookedRevenue, 20, getHeight()-40);
            g.drawString("Refund: ₹" + refundedAmount, 20, getHeight()-20);
        }

        private void drawBar(Graphics2D g){

            int baseY = getHeight() - 80;
            int barWidth = 80;

            double max = Math.max(bookedRevenue, refundedAmount);
            if(max == 0) max = 1;

            int bookedHeight = (int)((bookedRevenue/max)*220);
            int refundHeight = (int)((refundedAmount/max)*220);

            g.setFont(UIConfig.FONT_SUBTITLE);
            g.drawString("Revenue Comparison", 20, 30);

            int x1 = getWidth()/2 - 120;
            int x2 = getWidth()/2 + 40;

            g.setColor(UIConfig.SUCCESS);
            g.fillRoundRect(x1, baseY-bookedHeight, barWidth, bookedHeight,20,20);
            g.drawString("₹" + bookedRevenue, x1, baseY-bookedHeight-10);

            g.setColor(UIConfig.DANGER);
            g.fillRoundRect(x2, baseY-refundHeight, barWidth, refundHeight,20,20);
            g.drawString("₹" + refundedAmount, x2, baseY-refundHeight-10);
        }
    }
}