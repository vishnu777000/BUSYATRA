package ui.admin;

import config.UIConfig;
import dao.BusDAO;
import dao.RouteDAO;
import dao.ScheduleDAO;
import util.Refreshable;

import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class ManageSchedulesPanel extends JPanel implements Refreshable {

private JTable table;
private DefaultTableModel model;

private JComboBox<Item> busBox;
private JComboBox<Item> routeBox;

private JDateChooser departDateChooser;
private JDateChooser arriveDateChooser;

private JSpinner departTimeSpinner;
private JSpinner arriveTimeSpinner;

private JTextField searchField;

private int selectedId = -1;

public ManageSchedulesPanel(){

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(),BorderLayout.NORTH);
    add(tableCard(),BorderLayout.CENTER);
    add(formCard(),BorderLayout.EAST);

    refreshData();
}

/* ================= HEADER ================= */

private JPanel header(){

    JPanel p = new JPanel(new BorderLayout());
    p.setOpaque(false);

    JLabel title = new JLabel("Schedule Management");
    title.setFont(UIConfig.FONT_TITLE);

    searchField = new JTextField();
    searchField.setPreferredSize(new Dimension(200,36));

    JButton searchBtn = new JButton("Search");
    UIConfig.primaryBtn(searchBtn);

    searchBtn.addActionListener(e -> search());

    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    right.setOpaque(false);
    right.add(searchField);
    right.add(searchBtn);

    p.add(title,BorderLayout.WEST);
    p.add(right,BorderLayout.EAST);

    return p;
}

/* ================= TABLE ================= */

private JPanel tableCard(){

    model = new DefaultTableModel(
            new Object[]{"ID","Bus","Route","Departure","Arrival"},0){
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
    card.setPreferredSize(new Dimension(420,0));

    JLabel h = new JLabel("Schedule Details");
    h.setFont(UIConfig.FONT_SUBTITLE);

    busBox = new JComboBox<>();
    routeBox = new JComboBox<>();

    departDateChooser = dateChooser();
    arriveDateChooser = dateChooser();

    departTimeSpinner = timeSpinner();
    arriveTimeSpinner = timeSpinner();

    JPanel form = new JPanel();
    form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
    form.setOpaque(false);

    form.add(label("Bus")); form.add(busBox);
    form.add(label("Route")); form.add(routeBox);
    form.add(label("Departure Date")); form.add(departDateChooser);
    form.add(label("Departure Time")); form.add(departTimeSpinner);
    form.add(label("Arrival Date")); form.add(arriveDateChooser);
    form.add(label("Arrival Time")); form.add(arriveTimeSpinner);

    JButton addBtn = btn("Add");
    JButton updateBtn = btn("Update");
    JButton deleteBtn = btn("Delete");

    UIConfig.primaryBtn(addBtn);
    UIConfig.successBtn(updateBtn);
    UIConfig.dangerBtn(deleteBtn);

    addBtn.addActionListener(e -> add());
    updateBtn.addActionListener(e -> update());
    deleteBtn.addActionListener(e -> delete());

    JPanel btns = new JPanel(new GridLayout(3,1,10,10));
    btns.setOpaque(false);
    btns.add(addBtn);
    btns.add(updateBtn);
    btns.add(deleteBtn);

    card.add(h,BorderLayout.NORTH);
    card.add(form,BorderLayout.CENTER);
    card.add(btns,BorderLayout.SOUTH);

    return card;
}

private JLabel label(String t){
    return new JLabel(t);
}

private JButton btn(String t){
    JButton b = new JButton(t);
    b.setPreferredSize(new Dimension(140,36));
    return b;
}

private JDateChooser dateChooser(){
    JDateChooser dc = new JDateChooser();
    dc.setDate(new Date());
    return dc;
}

private JSpinner timeSpinner(){
    JSpinner sp = new JSpinner(new SpinnerDateModel());
    sp.setEditor(new JSpinner.DateEditor(sp,"HH:mm:ss"));
    return sp;
}

/* ================= LOAD ================= */

private void loadSchedules(){

    model.setRowCount(0);

    List<String[]> list = new ScheduleDAO().getAllSchedules();

    for(String[] row : list){
        model.addRow(row);
    }
}

private void loadBusBox(){

    busBox.removeAllItems();

    List<String[]> list = new BusDAO().getAllBuses();

    for(String[] b : list){
        busBox.addItem(new Item(Integer.parseInt(b[0]),b[1]+"-"+b[2]));
    }
}

private void loadRouteBox(){

    routeBox.removeAllItems();

    List<String[]> list = new RouteDAO().getAllRoutes();

    for(String[] r : list){
        routeBox.addItem(new Item(Integer.parseInt(r[0]),r[1]));
    }
}

/* ================= SEARCH ================= */

private void search(){

    String q = searchField.getText();

    model.setRowCount(0);

    List<String[]> list = new ScheduleDAO().searchSchedule(q);

    for(String[] row : list){
        model.addRow(row);
    }
}

/* ================= FILL ================= */

private void fillForm(){

    int row = table.getSelectedRow();
    if(row == -1) return;

    selectedId = Integer.parseInt(model.getValueAt(row,0).toString());
}

/* ================= ADD ================= */

private void add(){

    try{

        Item bus = (Item)busBox.getSelectedItem();
        Item route = (Item)routeBox.getSelectedItem();

        Timestamp dep = getTimestamp(departDateChooser,departTimeSpinner);
        Timestamp arr = getTimestamp(arriveDateChooser,arriveTimeSpinner);

        if(arr.before(dep)){
            JOptionPane.showMessageDialog(this,"Invalid time");
            return;
        }

        boolean ok = new ScheduleDAO().addSchedule(bus.id,route.id,dep,arr);

        if(ok){
            loadSchedules();
            JOptionPane.showMessageDialog(this,"Added ✅");
        }

    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Error ❌");
    }
}

/* ================= UPDATE ================= */

private void update(){

    if(selectedId == -1){
        JOptionPane.showMessageDialog(this,"Select row");
        return;
    }

    try{

        Item bus = (Item)busBox.getSelectedItem();
        Item route = (Item)routeBox.getSelectedItem();

        Timestamp dep = getTimestamp(departDateChooser,departTimeSpinner);
        Timestamp arr = getTimestamp(arriveDateChooser,arriveTimeSpinner);

        boolean ok = new ScheduleDAO()
                .updateSchedule(selectedId,bus.id,route.id,dep,arr);

        if(ok){
            loadSchedules();
            JOptionPane.showMessageDialog(this,"Updated ✅");
        }

    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Error ❌");
    }
}

/* ================= DELETE ================= */

private void delete(){

    if(selectedId == -1){
        JOptionPane.showMessageDialog(this,"Select row");
        return;
    }

    int c = JOptionPane.showConfirmDialog(this,"Delete?");

    if(c != JOptionPane.YES_OPTION) return;

    boolean ok = new ScheduleDAO().deleteSchedule(selectedId);

    if(ok){
        loadSchedules();
        JOptionPane.showMessageDialog(this,"Deleted ✅");
    }
}

/* ================= TIME ================= */

private Timestamp getTimestamp(JDateChooser dc,JSpinner sp){

    Date d = dc.getDate();
    Date t = (Date)sp.getValue();

    long millis = d.getTime() + (t.getTime()%(24*60*60*1000));

    return new Timestamp(millis);
}

@Override
public void refreshData(){

    loadBusBox();
    loadRouteBox();
    loadSchedules();
}

private static class Item{

    int id;
    String label;

    Item(int id,String label){
        this.id=id;
        this.label=label;
    }

    public String toString(){
        return label;
    }
}

}
