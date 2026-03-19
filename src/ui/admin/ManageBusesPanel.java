package ui.admin;

import config.UIConfig;
import dao.BusDAO;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ManageBusesPanel extends JPanel implements Refreshable {

private JTable table;
private DefaultTableModel model;

private JTextField operatorField;
private JTextField typeField;
private JTextField seatsField;
private JTextField fareField;

private int selectedBusId = -1;

public ManageBusesPanel() {

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(), BorderLayout.NORTH);
    add(tableCard(), BorderLayout.CENTER);
    add(formCard(), BorderLayout.EAST);

    loadBuses();
}

/* ================= HEADER ================= */

private JPanel header(){

    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JLabel title = new JLabel("Manage Buses");
    title.setFont(UIConfig.FONT_TITLE);

    JLabel sub = new JLabel("Add, edit & manage buses");
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
            new Object[]{"ID","Operator","Type","Seats","Fare","Status"},0
    ){
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
    card.setPreferredSize(new Dimension(340,0));

    JLabel heading = new JLabel("Bus Details");
    heading.setFont(UIConfig.FONT_SUBTITLE);

    operatorField = field("Operator");
    typeField = field("Bus Type");
    seatsField = field("Seats");
    fareField = field("Fare");

    JPanel fields = new JPanel(new GridLayout(4,1,10,10));
    fields.setOpaque(false);
    fields.add(operatorField);
    fields.add(typeField);
    fields.add(seatsField);
    fields.add(fareField);

    JButton addBtn = btn("Add");
    JButton updateBtn = btn("Update");
    JButton statusBtn = btn("Toggle Status");
    JButton deleteBtn = btn("Delete");

    UIConfig.primaryBtn(addBtn);
    UIConfig.successBtn(updateBtn);
    UIConfig.infoBtn(statusBtn);
    UIConfig.dangerBtn(deleteBtn);

    addBtn.addActionListener(e -> addBus());
    updateBtn.addActionListener(e -> updateBus());
    statusBtn.addActionListener(e -> toggleStatus());
    deleteBtn.addActionListener(e -> deleteBus());

    JPanel btns = new JPanel(new GridLayout(4,1,10,10));
    btns.setOpaque(false);
    btns.add(addBtn);
    btns.add(updateBtn);
    btns.add(statusBtn);
    btns.add(deleteBtn);

    card.add(heading,BorderLayout.NORTH);
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

private JButton btn(String text){
    JButton b = new JButton(text);
    b.setPreferredSize(new Dimension(140,36));
    return b;
}

/* ================= LOAD ================= */

private void loadBuses(){

    SwingUtilities.invokeLater(() -> {

        model.setRowCount(0);

        List<String[]> list = new BusDAO().getAllBuses();

        for(String[] row : list){
            model.addRow(row);
        }
    });
}

/* ================= FILL FORM ================= */

private void fillForm(){

    int row = table.getSelectedRow();
    if(row == -1) return;

    selectedBusId = Integer.parseInt(model.getValueAt(row,0).toString());

    operatorField.setText(model.getValueAt(row,1).toString());
    typeField.setText(model.getValueAt(row,2).toString());
    seatsField.setText(model.getValueAt(row,3).toString());
    fareField.setText(model.getValueAt(row,4).toString());
}

/* ================= ADD ================= */

private void addBus(){

    try{
        String op = operatorField.getText().trim();
        String type = typeField.getText().trim();
        int seats = Integer.parseInt(seatsField.getText().trim());
        double fare = Double.parseDouble(fareField.getText().trim());

        if(op.isEmpty() || type.isEmpty()){
            JOptionPane.showMessageDialog(this,"Fill all fields");
            return;
        }

        boolean ok = new BusDAO().addBus(op,type,seats,fare);

        if(ok){
            clear();
            loadBuses();
            JOptionPane.showMessageDialog(this,"Added ✅");
        }

    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Invalid input ❌");
    }
}

/* ================= UPDATE ================= */

private void updateBus(){

    if(selectedBusId == -1){
        JOptionPane.showMessageDialog(this,"Select bus");
        return;
    }

    try{
        String op = operatorField.getText();
        String type = typeField.getText();
        int seats = Integer.parseInt(seatsField.getText());
        double fare = Double.parseDouble(fareField.getText());

        boolean ok = new BusDAO().updateBus(selectedBusId,op,type,seats,fare);

        if(ok){
            loadBuses();
            JOptionPane.showMessageDialog(this,"Updated ✅");
        }

    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Error ❌");
    }
}

/* ================= STATUS ================= */

private void toggleStatus(){

    if(selectedBusId == -1){
        JOptionPane.showMessageDialog(this,"Select bus");
        return;
    }

    int row = table.getSelectedRow();
    String status = model.getValueAt(row,5).toString();

    String newStatus = status.equals("ACTIVE") ? "INACTIVE" : "ACTIVE";

    boolean ok = new BusDAO().setBusStatus(selectedBusId,newStatus);

    if(ok){
        loadBuses();
        JOptionPane.showMessageDialog(this,"Status updated ✅");
    }
}

/* ================= DELETE ================= */

private void deleteBus(){

    if(selectedBusId == -1){
        JOptionPane.showMessageDialog(this,"Select bus");
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(
            this,"Delete bus?","Confirm",
            JOptionPane.YES_NO_OPTION
    );

    if(confirm != JOptionPane.YES_OPTION) return;

    boolean ok = new BusDAO().deleteBus(selectedBusId);

    if(ok){
        clear();
        loadBuses();
        JOptionPane.showMessageDialog(this,"Deleted ✅");
    }
}

/* ================= CLEAR ================= */

private void clear(){

    selectedBusId = -1;
    operatorField.setText("");
    typeField.setText("");
    seatsField.setText("");
    fareField.setText("");
}

@Override
public void refreshData(){
    loadBuses();
}

}
