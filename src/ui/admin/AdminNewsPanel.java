package ui.admin;

import config.UIConfig;
import dao.NewsDAO;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminNewsPanel extends JPanel implements Refreshable {

private JTextArea messageArea;
private JTable table;
private DefaultTableModel model;

public AdminNewsPanel() {

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(), BorderLayout.NORTH);
    add(content(), BorderLayout.CENTER);

    loadNewsTable();
}

/* ================= HEADER ================= */

private JPanel header(){

    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JLabel title = new JLabel("News & Notifications");
    title.setFont(UIConfig.FONT_TITLE);

    JLabel sub = new JLabel("Manage announcements for users");
    sub.setFont(UIConfig.FONT_SMALL);
    sub.setForeground(UIConfig.TEXT_LIGHT);

    JPanel left = new JPanel(new GridLayout(2,1));
    left.setOpaque(false);
    left.add(title);
    left.add(sub);

    panel.add(left,BorderLayout.WEST);
    return panel;
}

/* ================= CONTENT ================= */

private JPanel content(){

    JPanel panel = new JPanel(new GridLayout(1,2,20,20));
    panel.setOpaque(false);

    panel.add(publishCard());
    panel.add(listCard());

    return panel;
}

/* ================= PUBLISH ================= */

private JPanel publishCard(){

    JPanel card = new JPanel(new BorderLayout(12,12));
    UIConfig.styleCard(card);

    JLabel h = new JLabel("Publish News");
    h.setFont(UIConfig.FONT_SUBTITLE);

    messageArea = new JTextArea(6,20);
    messageArea.setLineWrap(true);
    messageArea.setWrapStyleWord(true);
    messageArea.setFont(UIConfig.FONT_NORMAL);

    JScrollPane scroll = new JScrollPane(messageArea);
    scroll.setBorder(BorderFactory.createTitledBorder("Message"));

    JButton publishBtn = createBtn("Publish");
    UIConfig.primaryBtn(publishBtn);

    publishBtn.addActionListener(e -> publish());

    card.add(h,BorderLayout.NORTH);
    card.add(scroll,BorderLayout.CENTER);
    card.add(publishBtn,BorderLayout.SOUTH);

    return card;
}

/* ================= LIST ================= */

private JPanel listCard(){

    JPanel card = new JPanel(new BorderLayout(12,12));
    UIConfig.styleCard(card);

    JLabel h = new JLabel("All News");
    h.setFont(UIConfig.FONT_SUBTITLE);

    model = new DefaultTableModel(
            new Object[]{"ID","Message","Active","Created"},0
    ){
        public boolean isCellEditable(int r,int c){ return false; }
    };

    table = new JTable(model);
    UIConfig.styleTable(table);

    table.setRowHeight(32);

    JScrollPane sp = new JScrollPane(table);

    JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
    btns.setOpaque(false);

    JButton refreshBtn = createBtn("Refresh");
    JButton toggleBtn  = createBtn("Toggle Active");
    JButton deleteBtn  = createBtn("Delete");

    UIConfig.infoBtn(refreshBtn);
    UIConfig.successBtn(toggleBtn);
    UIConfig.dangerBtn(deleteBtn);

    refreshBtn.addActionListener(e -> loadNewsTable());
    toggleBtn.addActionListener(e -> toggleStatus());
    deleteBtn.addActionListener(e -> deleteSelected());

    btns.add(refreshBtn);
    btns.add(toggleBtn);
    btns.add(deleteBtn);

    card.add(h,BorderLayout.NORTH);
    card.add(sp,BorderLayout.CENTER);
    card.add(btns,BorderLayout.SOUTH);

    return card;
}

/* ================= BUTTON SIZE ================= */

private JButton createBtn(String text){

    JButton b = new JButton(text);
    b.setPreferredSize(new Dimension(140,36));
    return b;
}

/* ================= LOAD ================= */

private void loadNewsTable(){

    SwingUtilities.invokeLater(() -> {

        model.setRowCount(0);

        try{
            List<String[]> list = new NewsDAO().getAllNews();

            for(String[] row : list){
                model.addRow(new Object[]{
                        row[0],
                        row[1],
                        row[2].equals("1") ? "YES" : "NO",
                        row[3]
                });
            }

        }catch(Exception e){
            JOptionPane.showMessageDialog(this,"Load failed ❌");
        }
    });
}

/* ================= PUBLISH ================= */

private void publish(){

    String msg = messageArea.getText().trim();

    if(msg.isEmpty()){
        JOptionPane.showMessageDialog(this,"Enter message");
        return;
    }

    boolean ok = new NewsDAO().addNews(msg);

    if(ok){
        messageArea.setText("");
        loadNewsTable();
        JOptionPane.showMessageDialog(this,"Published ✅");
    }else{
        JOptionPane.showMessageDialog(this,"Failed ❌");
    }
}

/* ================= TOGGLE ACTIVE ================= */

private void toggleStatus(){

    int row = table.getSelectedRow();

    if(row == -1){
        JOptionPane.showMessageDialog(this,"Select row");
        return;
    }

    int id = Integer.parseInt(model.getValueAt(row,0).toString());
    String status = model.getValueAt(row,2).toString();

    boolean newStatus = status.equals("NO");

    boolean ok = new NewsDAO().setNewsStatus(id,newStatus);

    if(ok){
        loadNewsTable();
        JOptionPane.showMessageDialog(this,"Updated ✅");
    }else{
        JOptionPane.showMessageDialog(this,"Failed ❌");
    }
}

/* ================= DELETE ================= */

private void deleteSelected(){

    int row = table.getSelectedRow();

    if(row == -1){
        JOptionPane.showMessageDialog(this,"Select row");
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(
            this,"Delete news?","Confirm",
            JOptionPane.YES_NO_OPTION
    );

    if(confirm != JOptionPane.YES_OPTION) return;

    int id = Integer.parseInt(model.getValueAt(row,0).toString());

    boolean ok = new NewsDAO().deleteNews(id);

    if(ok){
        loadNewsTable();
        JOptionPane.showMessageDialog(this,"Deleted ✅");
    }else{
        JOptionPane.showMessageDialog(this,"Failed ❌");
    }
}

@Override
public void refreshData(){
    loadNewsTable();
}

}
