package ui.admin;

import config.UIConfig;
import dao.ComplaintDAO;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;

public class AdminComplaintsPanel extends JPanel implements Refreshable {


private JTable table;
private DefaultTableModel model;
private JTextArea replyArea;

public AdminComplaintsPanel() {

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(), BorderLayout.NORTH);
    add(centerPanel(), BorderLayout.CENTER);

    loadAllComplaints();
}

/* ================= HEADER ================= */

private JPanel header(){

    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JLabel title = new JLabel("Complaints Management");
    title.setFont(UIConfig.FONT_TITLE);

    JLabel sub = new JLabel("Review, respond and resolve complaints");
    sub.setFont(UIConfig.FONT_SMALL);
    sub.setForeground(UIConfig.TEXT_LIGHT);

    JPanel left = new JPanel(new GridLayout(2,1));
    left.setOpaque(false);
    left.add(title);
    left.add(sub);

    panel.add(left,BorderLayout.WEST);
    return panel;
}

/* ================= CENTER ================= */

private JPanel centerPanel(){

    JPanel panel = new JPanel(new BorderLayout(20,20));
    panel.setOpaque(false);

    panel.add(tableCard(), BorderLayout.CENTER);
    panel.add(actionCard(), BorderLayout.EAST);

    return panel;
}

/* ================= TABLE ================= */

private JPanel tableCard(){

    model = new DefaultTableModel(
            new Object[]{"ID","User Email","Category","Message","Status"},0){

        public boolean isCellEditable(int r,int c){
            return false;
        }
    };

    table = new JTable(model);
    UIConfig.styleTable(table);

    table.setRowHeight(34);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    table.getColumnModel().getColumn(0).setMaxWidth(60);
    table.getColumnModel().getColumn(4).setMaxWidth(130);

    JScrollPane sp = new JScrollPane(table);

    JPanel card = new JPanel(new BorderLayout());
    UIConfig.styleCard(card);
    card.add(sp,BorderLayout.CENTER);

    return card;
}

/* ================= ACTION ================= */

private JPanel actionCard(){

    JPanel card = new JPanel(new BorderLayout(14,14));
    UIConfig.styleCard(card);
    card.setPreferredSize(new Dimension(340,0));

    JLabel heading = new JLabel("Take Action");
    heading.setFont(UIConfig.FONT_SUBTITLE);

    replyArea = new JTextArea(6,20);
    replyArea.setLineWrap(true);
    replyArea.setWrapStyleWord(true);
    replyArea.setFont(UIConfig.FONT_NORMAL);

    JScrollPane replyScroll = new JScrollPane(replyArea);
    replyScroll.setBorder(
            BorderFactory.createTitledBorder("Admin Reply")
    );

    JButton refreshBtn = createBtn("Refresh");
    JButton progressBtn = createBtn("In Progress");
    JButton resolveBtn = createBtn("Resolve");

    UIConfig.infoBtn(refreshBtn);
    UIConfig.primaryBtn(progressBtn);
    UIConfig.successBtn(resolveBtn);

    refreshBtn.addActionListener(e -> loadAllComplaints());
    progressBtn.addActionListener(e -> updateStatus("IN_PROGRESS"));
    resolveBtn.addActionListener(e -> updateStatus("RESOLVED"));

    JPanel btns = new JPanel(new GridLayout(3,1,10,10));
    btns.setOpaque(false);
    btns.add(refreshBtn);
    btns.add(progressBtn);
    btns.add(resolveBtn);

    card.add(heading,BorderLayout.NORTH);
    card.add(replyScroll,BorderLayout.CENTER);
    card.add(btns,BorderLayout.SOUTH);

    return card;
}

private JButton createBtn(String text){

    JButton b = new JButton(text);
    b.setPreferredSize(new Dimension(140,36)); // 🔥 consistent buttons
    return b;
}

/* ================= LOAD ================= */

private void loadAllComplaints(){

SwingUtilities.invokeLater(() -> {

    model.setRowCount(0);
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    try{

        java.util.List<String[]> list =
                new ComplaintDAO().getAllComplaints();

        for(String[] row : list){
            model.addRow(row);
        }

    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Failed to load ❌");
    }

    setCursor(Cursor.getDefaultCursor());
});


}


/* ================= UPDATE ================= */

private void updateStatus(String status){

    int row = table.getSelectedRow();

    if(row == -1){
        JOptionPane.showMessageDialog(this,"Select a complaint");
        return;
    }

    String reply = replyArea.getText().trim();

    if(reply.isEmpty()){
        JOptionPane.showMessageDialog(this,"Reply cannot be empty");
        return;
    }

    int id = Integer.parseInt(model.getValueAt(row,0).toString());

    boolean ok = new ComplaintDAO().updateComplaint(id,status,reply);

    JOptionPane.showMessageDialog(this,
            ok ? "Updated successfully ✅" : "Update failed ❌");

    if(ok){
        replyArea.setText("");
        loadAllComplaints();
    }
}

@Override
public void refreshData(){
    loadAllComplaints();
    replyArea.setText("");
}

}
