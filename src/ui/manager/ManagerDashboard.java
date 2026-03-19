package ui.manager;

import config.UIConfig;
import dao.AdminStatsDAO;

import javax.swing.*;
import java.awt.*;

public class ManagerDashboard extends JPanel {

    private final AdminStatsDAO dao = new AdminStatsDAO();

    public ManagerDashboard() {

        setLayout(new BorderLayout(20,20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(25,30,25,30));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper,BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        wrapper.add(header());
        wrapper.add(Box.createVerticalStrut(20));
        wrapper.add(kpiRow());
        wrapper.add(Box.createVerticalStrut(20));
        wrapper.add(progressSection());
        wrapper.add(Box.createVerticalStrut(20));
        wrapper.add(actionsSection());

        add(wrapper);
    }

    /* ================= HEADER ================= */

    private JPanel header(){

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Manager Dashboard");
        title.setFont(new Font("Segoe UI",Font.BOLD,28));

        JLabel sub = new JLabel("Operational overview & performance metrics");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(UIConfig.TEXT_LIGHT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));
        left.setOpaque(false);

        left.add(title);
        left.add(sub);

        panel.add(left,BorderLayout.WEST);

        return panel;
    }

    /* ================= KPI ROW ================= */

    private JPanel kpiRow(){

        JPanel grid = new JPanel(new GridLayout(1,4,20,0));
        grid.setOpaque(false);

        grid.add(kpiCard("Total Users", dao.getTotalUsers(), UIConfig.INFO));
        grid.add(kpiCard("Total Bookings", dao.getTotalTickets(), UIConfig.PRIMARY));
        grid.add(kpiCard("Cancelled Tickets", dao.getCancelledTickets(), UIConfig.DANGER));
        grid.add(kpiCard("Revenue (₹)", formatMoney(dao.getTotalRevenue()), UIConfig.SUCCESS));

        return grid;
    }

    private JPanel kpiCard(String title,Object value,Color accent){

        JPanel card = createCard();

        JLabel t = new JLabel(title);
        t.setFont(UIConfig.FONT_SMALL);
        t.setForeground(UIConfig.TEXT_LIGHT);

        JLabel v = new JLabel(String.valueOf(value),SwingConstants.CENTER);
        v.setFont(new Font("Segoe UI",Font.BOLD,26));
        v.setForeground(accent);

        card.add(t,BorderLayout.NORTH);
        card.add(v,BorderLayout.CENTER);

        return card;
    }

    /* ================= PROGRESS SECTION ================= */

    private JPanel progressSection(){

        JPanel grid = new JPanel(new GridLayout(1,2,20,0));
        grid.setOpaque(false);

        int total = dao.getTotalTickets();
        int cancelled = dao.getCancelledTickets();
        int active = Math.max(0,total-cancelled);

        grid.add(progressCard(
                "Booking Status",
                new String[]{"Active","Cancelled"},
                new int[]{active,cancelled},
                Math.max(total,1)
        ));

        grid.add(progressCard(
                "Revenue Target",
                new String[]{"Collected","Target"},
                new int[]{(int)dao.getTotalRevenue(),100000},
                100000
        ));

        return grid;
    }

    private JPanel progressCard(String title,String[] labels,int[] values,int max){

        JPanel card = createCard();

        JLabel t = new JLabel(title);
        t.setFont(UIConfig.FONT_SUBTITLE);

        JPanel bars = new JPanel(new GridLayout(labels.length,1,12,12));
        bars.setOpaque(false);

        for(int i=0;i<labels.length;i++){
            bars.add(progressBar(labels[i],values[i],max));
        }

        card.add(t,BorderLayout.NORTH);
        card.add(bars,BorderLayout.CENTER);

        return card;
    }

    private JPanel progressBar(String label,int value,int max){

        JPanel p = new JPanel(new BorderLayout(6,6));
        p.setOpaque(false);

        JLabel lbl = new JLabel(label+" : "+value);
        lbl.setFont(UIConfig.FONT_NORMAL);

        JProgressBar bar = new JProgressBar(0,max);
        bar.setValue(value);
        bar.setPreferredSize(new Dimension(260,16));
        bar.setBorderPainted(false);

        if(label.toLowerCase().contains("cancel"))
            bar.setForeground(UIConfig.DANGER);
        else
            bar.setForeground(UIConfig.SUCCESS);

        p.add(lbl,BorderLayout.NORTH);
        p.add(bar,BorderLayout.CENTER);

        return p;
    }

    /* ================= QUICK ACTIONS ================= */

    private JPanel actionsSection(){

        JPanel card = createCard();

        JLabel title = new JLabel("Quick Actions");
        title.setFont(UIConfig.FONT_SUBTITLE);

        JPanel grid = new JPanel(new GridLayout(1,4,15,0));
        grid.setOpaque(false);

        grid.add(actionBtn("View Complaints"));
        grid.add(actionBtn("Manage Schedules"));
        grid.add(actionBtn("Generate Reports"));
        grid.add(actionBtn("User Analytics"));

        card.add(title,BorderLayout.NORTH);
        card.add(grid,BorderLayout.CENTER);

        return card;
    }

    private JButton actionBtn(String text){

        JButton btn = new JButton(text);

        btn.setFont(new Font("Segoe UI",Font.BOLD,14));
        btn.setPreferredSize(new Dimension(150,40));
        btn.setFocusPainted(false);

        return btn;
    }

    /* ================= CARD STYLE ================= */

    private JPanel createCard(){

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,220)),
                BorderFactory.createEmptyBorder(18,18,18,18)
        ));

        return card;
    }

    private String formatMoney(double amt){
        return String.format("%.2f",amt);
    }
}
