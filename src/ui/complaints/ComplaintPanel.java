package ui.complaints;

import config.UIConfig;
import dao.ComplaintDAO;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ComplaintPanel extends JPanel implements Refreshable {

    private JTextArea complaintArea;
    private JComboBox<String> categoryBox;
    private JPanel listPanel;

    public ComplaintPanel() {

        setLayout(new BorderLayout(20,20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(25,30,25,30));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper,BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        wrapper.add(formPanel());
        wrapper.add(Box.createVerticalStrut(25));
        wrapper.add(listSection());

        add(wrapper);

        refreshData();
    }

    /* ================= FORM ================= */

    private JPanel formPanel(){

        JPanel card = createCard();
        card.setLayout(new BorderLayout(15,15));

        JLabel title = new JLabel("Raise a Complaint");
        title.setFont(new Font("Segoe UI",Font.BOLD,18));

        categoryBox = new JComboBox<>(new String[]{
                "Booking Issue",
                "Payment / Wallet",
                "Bus / Seat",
                "Cancellation / Refund",
                "Other"
        });

        complaintArea = new JTextArea(4,20);
        complaintArea.setLineWrap(true);
        complaintArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(complaintArea);

        JButton submit = new JButton("Submit Complaint");
        UIConfig.primaryBtn(submit);

        submit.addActionListener(e -> submitComplaint());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        top.add(title,BorderLayout.NORTH);
        top.add(categoryBox,BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(submit);

        card.add(top,BorderLayout.NORTH);
        card.add(scroll,BorderLayout.CENTER);
        card.add(bottom,BorderLayout.SOUTH);

        return card;
    }

    /* ================= LIST ================= */

    private JPanel listSection(){

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        JLabel title = new JLabel("My Complaints");
        title.setFont(new Font("Segoe UI",Font.BOLD,18));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel,BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);

        container.add(title,BorderLayout.NORTH);
        container.add(scroll,BorderLayout.CENTER);

        return container;
    }

    /* ================= SUBMIT ================= */

    private void submitComplaint(){

        String msg = complaintArea.getText().trim();
        String category = (String)categoryBox.getSelectedItem();

        if(msg.isEmpty()){
            JOptionPane.showMessageDialog(this,"Enter complaint message");
            return;
        }

        boolean ok = new ComplaintDAO()
                .addComplaint(Session.userId,category,msg);

        if(ok){
            complaintArea.setText("");
            refreshData();
            JOptionPane.showMessageDialog(this,"Submitted successfully");
        }else{
            JOptionPane.showMessageDialog(this,"Failed");
        }
    }

    /* ================= LOAD ================= */

    private void loadMyComplaints(){

        listPanel.removeAll();

        List<String[]> list =
                new ComplaintDAO().getComplaintsByUser(Session.userId);

        if(list.isEmpty()){

            JLabel empty = new JLabel("No complaints yet");
            empty.setForeground(UIConfig.TEXT_LIGHT);

            listPanel.add(empty);

        }else{

            for(String[] r : list){

                listPanel.add(complaintCard(r));
                listPanel.add(Box.createVerticalStrut(12));
            }
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    /* ================= CARD ================= */

    private JPanel complaintCard(String[] r){

        JPanel card = createCard();
        card.setLayout(new BorderLayout(10,10));

        String category = r[1];
        String message = r[2];
        String status = r[3];
        String reply = r[4];
        String time = r[5];

        JLabel statusLbl = new JLabel(status);
        statusLbl.setOpaque(true);
        statusLbl.setForeground(Color.WHITE);
        statusLbl.setBorder(BorderFactory.createEmptyBorder(4,10,4,10));

        if("OPEN".equalsIgnoreCase(status))
            statusLbl.setBackground(new Color(243,156,18));
        else if("IN_PROGRESS".equalsIgnoreCase(status))
            statusLbl.setBackground(new Color(52,152,219));
        else
            statusLbl.setBackground(new Color(46,204,113));

        JLabel cat = new JLabel(category);
        cat.setFont(new Font("Segoe UI",Font.BOLD,14));

        JLabel timeLbl = new JLabel(time);
        timeLbl.setForeground(UIConfig.TEXT_LIGHT);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        top.add(cat,BorderLayout.WEST);
        top.add(statusLbl,BorderLayout.EAST);

        JTextArea msg = new JTextArea(message);
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        msg.setEditable(false);
        msg.setOpaque(false);

        card.add(top,BorderLayout.NORTH);
        card.add(msg,BorderLayout.CENTER);
        card.add(timeLbl,BorderLayout.SOUTH);

        if(reply != null && !reply.isEmpty()){

            JLabel rep = new JLabel("Admin Reply:");
            rep.setFont(new Font("Segoe UI",Font.BOLD,12));

            JTextArea repArea = new JTextArea(reply);
            repArea.setEditable(false);
            repArea.setOpaque(false);

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setOpaque(false);

            bottom.add(rep,BorderLayout.NORTH);
            bottom.add(repArea,BorderLayout.CENTER);

            card.add(bottom,BorderLayout.SOUTH);
        }

        return card;
    }

    /* ================= CARD ================= */

    private JPanel createCard(){

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,220)),
                BorderFactory.createEmptyBorder(15,15,15,15)
        ));

        return card;
    }

    /* ================= REFRESH ================= */

    public void refreshData(){
        loadMyComplaints();
    }
}