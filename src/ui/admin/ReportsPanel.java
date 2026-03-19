package ui.admin;

import config.UIConfig;
import dao.ReportsDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ReportsPanel extends JPanel {

private JTable table;
private DefaultTableModel model;

private JLabel totalLbl;
private JLabel revenueLbl;
private JLabel cancelledLbl;

private JTextField searchField;

public ReportsPanel(){

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(),BorderLayout.NORTH);
    add(statsPanel(),BorderLayout.CENTER);
    add(tableCard(),BorderLayout.SOUTH);

    loadData();
}

/* ================= HEADER ================= */

private JPanel header(){

    JPanel p = new JPanel(new BorderLayout());
    p.setOpaque(false);

    JLabel title = new JLabel("Reports Dashboard");
    title.setFont(UIConfig.FONT_TITLE);

    searchField = new JTextField();
    searchField.setPreferredSize(new Dimension(200,36));

    JButton searchBtn = new JButton("Search");
    JButton refreshBtn = new JButton("Refresh");

    UIConfig.primaryBtn(searchBtn);
    UIConfig.infoBtn(refreshBtn);

    searchBtn.addActionListener(e -> search());
    refreshBtn.addActionListener(e -> loadData());

    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    right.setOpaque(false);
    right.add(searchField);
    right.add(searchBtn);
    right.add(refreshBtn);

    p.add(title,BorderLayout.WEST);
    p.add(right,BorderLayout.EAST);

    return p;
}

/* ================= STATS ================= */

private JPanel statsPanel(){

    JPanel grid = new JPanel(new GridLayout(1,3,20,20));
    grid.setOpaque(false);

    totalLbl = statCard("Total Bookings",grid);
    revenueLbl = statCard("Revenue (₹)",grid);
    cancelledLbl = statCard("Cancelled",grid);

    return grid;
}

private JLabel statCard(String title,JPanel parent){

    JPanel card = new JPanel(new BorderLayout(10,10));
    UIConfig.styleCard(card);

    JLabel t = new JLabel(title);
    t.setForeground(UIConfig.TEXT_LIGHT);

    JLabel v = new JLabel("0");
    v.setFont(new Font("Segoe UI",Font.BOLD,26));
    v.setHorizontalAlignment(SwingConstants.CENTER);

    card.add(t,BorderLayout.NORTH);
    card.add(v,BorderLayout.CENTER);

    parent.add(card);

    return v;
}

/* ================= TABLE ================= */

private JPanel tableCard(){

    model = new DefaultTableModel(
            new Object[]{"ID","Passenger","Route","Amount","Status","Time"},0){
        public boolean isCellEditable(int r,int c){ return false; }
    };

    table = new JTable(model);
    UIConfig.styleTable(table);
    table.setRowHeight(30);

    DefaultTableCellRenderer right = new DefaultTableCellRenderer();
    right.setHorizontalAlignment(SwingConstants.RIGHT);
    table.getColumnModel().getColumn(3).setCellRenderer(right);

    JScrollPane sp = new JScrollPane(table);

    JPanel card = new JPanel(new BorderLayout());
    UIConfig.styleCard(card);
    card.add(sp);

    return card;
}

/* ================= LOAD ================= */

private void loadData(){

    model.setRowCount(0);

    List<String[]> list = new ReportsDAO().getAllBookings();

    int total = 0;
    int cancelled = 0;
    double revenue = 0;

    for(String[] r : list){

        total++;

        double amt = Double.parseDouble(r[4]);

        if("CANCELLED".equalsIgnoreCase(r[5])){
            cancelled++;
        }else{
            revenue += amt;
        }

        model.addRow(new Object[]{
                r[0],
                r[1],
                r[2]+" → "+r[3],
                amt,
                r[5],
                r[6]
        });
    }

    totalLbl.setText(String.valueOf(total));
    revenueLbl.setText(String.format("%.2f",revenue));
    cancelledLbl.setText(String.valueOf(cancelled));
}

/* ================= SEARCH ================= */

private void search(){

    String q = searchField.getText();

    model.setRowCount(0);

    List<String[]> list = new ReportsDAO().searchBooking(q);

    for(String[] r : list){

        model.addRow(new Object[]{
                r[0],
                r[1],
                r[2]+" → "+r[3],
                r[4],
                r[5],
                r[6]
        });
    }
}

}
