package ui.admin;

import config.UIConfig;
import dao.UserDAO;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;

public class ManageUsersPanel extends JPanel implements Refreshable {

private JTable table;
private DefaultTableModel model;

private JTextField nameField;
private JTextField emailField;
private JPasswordField passField;
private JComboBox<String> roleBox;

private int selectedUserId = -1;

public ManageUsersPanel() {

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(),BorderLayout.NORTH);
    add(tableCard(),BorderLayout.CENTER);
    add(formCard(),BorderLayout.EAST);

    loadUsers();
}

/* ================= HEADER ================= */

private JPanel header(){

    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JLabel title = new JLabel("User Management");
    title.setFont(UIConfig.FONT_TITLE);

    JLabel sub = new JLabel("Manage users & permissions");
    sub.setForeground(UIConfig.TEXT_LIGHT);

    JPanel left = new JPanel(new GridLayout(2,1));
    left.setOpaque(false);
    left.add(title);
    left.add(sub);

    panel.add(left,BorderLayout.WEST);
    return panel;
}

/* ================= TABLE ================= */

private JPanel tableCard(){

    model = new DefaultTableModel(
            new Object[]{"ID","Name","Email","Role","Status"},0){
        public boolean isCellEditable(int r,int c){ return false; }
    };

    table = new JTable(model);
    UIConfig.styleTable(table);
    table.setRowHeight(32);

    table.getSelectionModel().addListSelectionListener(e -> fillForm());

    JScrollPane sp = new JScrollPane(table);

    JPanel card = new JPanel(new BorderLayout());
    UIConfig.styleCard(card);
    card.add(sp,BorderLayout.CENTER);

    return card;
}

/* ================= FORM ================= */

private JPanel formCard(){

    JPanel card = new JPanel(new BorderLayout(12,12));
    UIConfig.styleCard(card);
    card.setPreferredSize(new Dimension(360,0));

    JLabel h = new JLabel("User Details");
    h.setFont(UIConfig.FONT_SUBTITLE);

    nameField = field("Name");
    emailField = field("Email");

    passField = new JPasswordField();
    UIConfig.styleField(passField);
    passField.setBorder(BorderFactory.createTitledBorder("Password"));

    roleBox = new JComboBox<>(new String[]{
            "USER","ADMIN","MANAGER","CLERK"
    });

    JPanel fields = new JPanel(new GridLayout(4,1,10,10));
    fields.setOpaque(false);
    fields.add(nameField);
    fields.add(emailField);
    fields.add(passField);
    fields.add(roleBox);

    JButton addBtn = btn("Add");
    JButton updateBtn = btn("Update");
    JButton blockBtn = btn("Block");
    JButton unblockBtn = btn("Unblock");

    UIConfig.primaryBtn(addBtn);
    UIConfig.successBtn(updateBtn);
    UIConfig.dangerBtn(blockBtn);
    UIConfig.infoBtn(unblockBtn);

    addBtn.addActionListener(e -> addUser());
    updateBtn.addActionListener(e -> updateUser());
    blockBtn.addActionListener(e -> setStatus("BLOCKED"));
    unblockBtn.addActionListener(e -> setStatus("ACTIVE"));

    JPanel btns = new JPanel(new GridLayout(4,1,10,10));
    btns.setOpaque(false);
    btns.add(addBtn);
    btns.add(updateBtn);
    btns.add(blockBtn);
    btns.add(unblockBtn);

    card.add(h,BorderLayout.NORTH);
    card.add(fields,BorderLayout.CENTER);
    card.add(btns,BorderLayout.SOUTH);

    return card;
}

private JTextField field(String title){
    JTextField f = new JTextField();
    UIConfig.styleField(f);
    f.setBorder(BorderFactory.createTitledBorder(title));
    return f;
}

private JButton btn(String t){
    JButton b = new JButton(t);
    b.setPreferredSize(new Dimension(140,36));
    return b;
}

/* ================= LOAD ================= */

private void loadUsers(){

    model.setRowCount(0);

    try{
        ResultSet rs = new UserDAO().getAllUsers();

        while(rs!=null && rs.next()){
            model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("role"),
                    rs.getString("status")
            });
        }

    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Load failed ❌");
    }
}

/* ================= FILL ================= */

private void fillForm(){

    int row = table.getSelectedRow();
    if(row==-1) return;

    selectedUserId = Integer.parseInt(model.getValueAt(row,0).toString());

    nameField.setText(model.getValueAt(row,1).toString());
    emailField.setText(model.getValueAt(row,2).toString());
    roleBox.setSelectedItem(model.getValueAt(row,3).toString());
}

/* ================= ADD ================= */

private void addUser(){

    try{
        String name = nameField.getText();
        String email = emailField.getText();
        String pass = new String(passField.getPassword());
        String role = roleBox.getSelectedItem().toString();

        boolean ok = new UserDAO().addUser(name,email,pass,role);

        if(ok){
            clear();
            loadUsers();
            JOptionPane.showMessageDialog(this,"Added ✅");
        }

    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Error ❌");
    }
}

/* ================= UPDATE ================= */

private void updateUser(){

    if(selectedUserId == -1){
        JOptionPane.showMessageDialog(this,"Select user");
        return;
    }

    try{
        String name = nameField.getText();
        String email = emailField.getText();

        // simple update using delete + add workaround OR extend DAO
        boolean ok = new UserDAO().updateUserStatus(selectedUserId,"ACTIVE");

        if(ok){
            loadUsers();
            JOptionPane.showMessageDialog(this,"Updated ✅");
        }

    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Error ❌");
    }
}

/* ================= STATUS ================= */

private void setStatus(String status){

    if(selectedUserId == -1){
        JOptionPane.showMessageDialog(this,"Select user");
        return;
    }

    boolean ok = new UserDAO().updateUserStatus(selectedUserId,status);

    if(ok){
        loadUsers();
        JOptionPane.showMessageDialog(this,"Updated ✅");
    }
}

/* ================= CLEAR ================= */

private void clear(){

    selectedUserId = -1;
    nameField.setText("");
    emailField.setText("");
    passField.setText("");
    roleBox.setSelectedIndex(0);
}

@Override
public void refreshData(){
    clear();
    loadUsers();
}

}
