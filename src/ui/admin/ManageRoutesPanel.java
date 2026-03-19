package ui.admin;

import config.UIConfig;
import dao.RouteDAO;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

public class ManageRoutesPanel extends JPanel implements Refreshable {

private JTable table;
private DefaultTableModel model;

private JTextField routeNameField;
private JTextField distanceField;
private JTextField rateField;

private JLabel mapPreview;

private int selectedRouteId = -1;

private final RouteDAO dao = new RouteDAO();

public ManageRoutesPanel(){

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(),BorderLayout.NORTH);
    add(center(),BorderLayout.CENTER);

    loadRoutes();
}

/* ================= HEADER ================= */

private JPanel header(){

    JPanel p = new JPanel(new BorderLayout());
    p.setOpaque(false);

    JLabel title = new JLabel("Route Management");
    title.setFont(UIConfig.FONT_TITLE);

    JLabel sub = new JLabel("Manage routes, maps & fares");
    sub.setForeground(UIConfig.TEXT_LIGHT);

    JPanel left = new JPanel(new GridLayout(2,1));
    left.setOpaque(false);
    left.add(title);
    left.add(sub);

    p.add(left,BorderLayout.WEST);
    return p;
}

/* ================= CENTER ================= */

private JPanel center(){

    JPanel p = new JPanel(new GridLayout(1,2,20,20));
    p.setOpaque(false);

    p.add(tableCard());
    p.add(formCard());

    return p;
}

/* ================= TABLE ================= */

private JPanel tableCard(){

    model = new DefaultTableModel(
            new Object[]{"ID","Route","KM","Rate","Map"},0){
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

    JLabel h = new JLabel("Route Details");
    h.setFont(UIConfig.FONT_SUBTITLE);

    routeNameField = field("Route Name");
    distanceField = field("Distance KM");
    rateField = field("Rate / KM");

    mapPreview = new JLabel("No Map",SwingConstants.CENTER);

    JPanel fields = new JPanel(new GridLayout(4,1,10,10));
    fields.setOpaque(false);
    fields.add(routeNameField);
    fields.add(distanceField);
    fields.add(rateField);
    fields.add(mapPreview);

    JButton addBtn = btn("Add");
    JButton updateBtn = btn("Update");
    JButton mapBtn = btn("Upload Map");
    JButton deleteBtn = btn("Delete");

    UIConfig.primaryBtn(addBtn);
    UIConfig.successBtn(updateBtn);
    UIConfig.infoBtn(mapBtn);
    UIConfig.dangerBtn(deleteBtn);

    addBtn.addActionListener(e -> addRoute());
    updateBtn.addActionListener(e -> updateRoute());
    mapBtn.addActionListener(e -> uploadMap());
    deleteBtn.addActionListener(e -> deleteRoute());

    JPanel btns = new JPanel(new GridLayout(4,1,10,10));
    btns.setOpaque(false);
    btns.add(addBtn);
    btns.add(updateBtn);
    btns.add(mapBtn);
    btns.add(deleteBtn);

    card.add(h,BorderLayout.NORTH);
    card.add(fields,BorderLayout.CENTER);
    card.add(btns,BorderLayout.SOUTH);

    return card;
}

private JTextField field(String t){
    JTextField f = new JTextField();
    UIConfig.styleField(f);
    f.setBorder(BorderFactory.createTitledBorder(t));
    return f;
}

private JButton btn(String t){
    JButton b = new JButton(t);
    b.setPreferredSize(new Dimension(140,36));
    return b;
}

/* ================= LOAD ================= */

private void loadRoutes(){

    SwingUtilities.invokeLater(() -> {

        model.setRowCount(0);

        List<String[]> list = dao.getAllRoutes();

        for(String[] r : list){
            model.addRow(r);
        }
    });
}

/* ================= FILL ================= */

private void fillForm(){

    int row = table.getSelectedRow();
    if(row == -1) return;

    selectedRouteId = Integer.parseInt(model.getValueAt(row,0).toString());

    routeNameField.setText(model.getValueAt(row,1).toString());
    distanceField.setText(model.getValueAt(row,2).toString());
    rateField.setText(model.getValueAt(row,3).toString());

    String map = model.getValueAt(row,4).toString();

    if(map != null && !map.isEmpty()){
        try{
            ImageIcon icon = new ImageIcon(map);
            Image img = icon.getImage().getScaledInstance(200,120,Image.SCALE_SMOOTH);
            mapPreview.setIcon(new ImageIcon(img));
            mapPreview.setText("");
        }catch(Exception e){
            mapPreview.setText("Preview error");
        }
    }
}

/* ================= ADD ================= */

private void addRoute(){

    try{
        String name = routeNameField.getText();
        int km = Integer.parseInt(distanceField.getText());
        double rate = Double.parseDouble(rateField.getText());

        boolean ok = dao.addRouteMaster(name,km,rate);

        if(ok){
            clear();
            loadRoutes();
            JOptionPane.showMessageDialog(this,"Added ✅");
        }

    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Invalid input ❌");
    }
}

/* ================= UPDATE ================= */

private void updateRoute(){

    if(selectedRouteId == -1){
        JOptionPane.showMessageDialog(this,"Select route");
        return;
    }

    try{
        String name = routeNameField.getText();
        int km = Integer.parseInt(distanceField.getText());
        double rate = Double.parseDouble(rateField.getText());

        boolean ok = dao.updateRoute(selectedRouteId,name,km,rate);

        if(ok){
            loadRoutes();
            JOptionPane.showMessageDialog(this,"Updated ✅");
        }

    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Error ❌");
    }
}

/* ================= MAP ================= */

private void uploadMap(){

    if(selectedRouteId == -1){
        JOptionPane.showMessageDialog(this,"Select route");
        return;
    }

    JFileChooser ch = new JFileChooser();

    if(ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){

        File f = ch.getSelectedFile();
        String path = f.getAbsolutePath().replace("\\","/");

        boolean ok = dao.updateRouteMap(selectedRouteId,path);

        if(ok){
            loadRoutes();
            JOptionPane.showMessageDialog(this,"Map updated ✅");
        }
    }
}

/* ================= DELETE ================= */

private void deleteRoute(){

    if(selectedRouteId == -1){
        JOptionPane.showMessageDialog(this,"Select route");
        return;
    }

    int c = JOptionPane.showConfirmDialog(this,"Delete?");

    if(c != JOptionPane.YES_OPTION) return;

    boolean ok = dao.deleteRoute(selectedRouteId);

    if(ok){
        clear();
        loadRoutes();
        JOptionPane.showMessageDialog(this,"Deleted ✅");
    }
}

private void clear(){

    selectedRouteId = -1;
    routeNameField.setText("");
    distanceField.setText("");
    rateField.setText("");
    mapPreview.setIcon(null);
    mapPreview.setText("No Map");
}

@Override
public void refreshData(){
    loadRoutes();
}

}
