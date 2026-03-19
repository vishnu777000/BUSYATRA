package ui.admin;

import config.UIConfig;
import dao.AdminStatsDAO;

import javax.swing.*;
import java.awt.*;

public class AdminDashboard extends JPanel {

private JLabel usersLbl;
private JLabel ticketsLbl;
private JLabel revenueLbl;
private JLabel cancelledLbl;

public AdminDashboard() {

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(), BorderLayout.NORTH);
    add(content(), BorderLayout.CENTER);

    loadStats();
}

/* ================= HEADER ================= */

private JPanel header(){

    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JLabel title = new JLabel("Dashboard Overview");
    title.setFont(new Font("Segoe UI",Font.BOLD,24));

    JLabel sub = new JLabel("Monitor system performance & analytics");
    sub.setForeground(UIConfig.TEXT_LIGHT);

    JPanel left = new JPanel(new GridLayout(2,1));
    left.setOpaque(false);
    left.add(title);
    left.add(sub);

    panel.add(left,BorderLayout.WEST);

    return panel;
}

/* ================= MAIN CONTENT ================= */

private JPanel content(){

    JPanel panel = new JPanel(new BorderLayout(20,20));
    panel.setOpaque(false);

    panel.add(statsRow(), BorderLayout.NORTH);
    panel.add(bottomSection(), BorderLayout.CENTER);

    return panel;
}

/* ================= TOP CARDS ================= */

private JPanel statsRow(){

    JPanel row = new JPanel(new GridLayout(1,4,20,20));
    row.setOpaque(false);

    usersLbl = createValue();
    ticketsLbl = createValue();
    revenueLbl = createValue();
    cancelledLbl = createValue();

    row.add(modernCard("Users", usersLbl, new Color(33,150,243)));
    row.add(modernCard("Tickets", ticketsLbl, new Color(156,39,176)));
    row.add(modernCard("Revenue", revenueLbl, new Color(76,175,80)));
    row.add(modernCard("Cancelled", cancelledLbl, new Color(244,67,54)));

    return row;
}

private JLabel createValue(){

    JLabel lbl = new JLabel("0");
    lbl.setFont(new Font("Segoe UI",Font.BOLD,28));
    lbl.setForeground(Color.WHITE);
    return lbl;
}

private JPanel modernCard(String title, JLabel value, Color color){

    JPanel card = new JPanel(new BorderLayout());
    card.setPreferredSize(new Dimension(200,100));

    card.setBackground(color);
    card.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));

    JLabel t = new JLabel(title);
    t.setForeground(Color.WHITE);

    card.add(t,BorderLayout.NORTH);
    card.add(value,BorderLayout.CENTER);

    return card;
}

/* ================= BOTTOM ================= */

private JPanel bottomSection(){

    JPanel panel = new JPanel(new GridLayout(1,2,20,20));
    panel.setOpaque(false);

    panel.add(activityCard());
    panel.add(infoCard());

    return panel;
}

private JPanel activityCard(){

    JPanel card = new JPanel(new BorderLayout());
    UIConfig.styleCard(card);

    JLabel title = new JLabel("Recent Activity");
    title.setFont(UIConfig.FONT_SUBTITLE);

    JTextArea area = new JTextArea(
            "• Ticket booked\n• User registered\n• Payment received\n• Complaint resolved"
    );

    area.setEditable(false);
    area.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    card.add(title,BorderLayout.NORTH);
    card.add(new JScrollPane(area),BorderLayout.CENTER);

    return card;
}

private JPanel infoCard(){

    JPanel card = new JPanel(new BorderLayout());
    UIConfig.styleCard(card);

    JLabel title = new JLabel("System Info");
    title.setFont(UIConfig.FONT_SUBTITLE);

    JLabel info = new JLabel(
            "<html>Total System Health: <b>Good</b><br/>Server Status: Online</html>"
    );

    info.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    card.add(title,BorderLayout.NORTH);
    card.add(info,BorderLayout.CENTER);

    return card;
}

/* ================= LOAD ================= */

private void loadStats(){

    AdminStatsDAO dao = new AdminStatsDAO();

    usersLbl.setText(String.valueOf(dao.getTotalUsers()));
    ticketsLbl.setText(String.valueOf(dao.getTotalTickets()));
    cancelledLbl.setText(String.valueOf(dao.getCancelledTickets()));
    revenueLbl.setText("₹" + String.format("%.0f", dao.getTotalRevenue()));
}

}
