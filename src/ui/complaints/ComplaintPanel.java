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
    private JLabel statusLabel;
    private JButton submitBtn;

    public ComplaintPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        wrapper.add(formPanel());
        wrapper.add(Box.createVerticalStrut(25));
        wrapper.add(listSection());

        add(wrapper, BorderLayout.CENTER);
        refreshData();
    }

    private JPanel formPanel() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(15, 15));

        JLabel title = new JLabel("Raise a Complaint");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        categoryBox = new JComboBox<>(new String[]{
                "Booking Issue",
                "Payment / Wallet",
                "Bus / Seat",
                "Cancellation / Refund",
                "Other"
        });
        UIConfig.styleCombo(categoryBox);

        complaintArea = new JTextArea(4, 20);
        complaintArea.setLineWrap(true);
        complaintArea.setWrapStyleWord(true);
        complaintArea.setFont(UIConfig.FONT_NORMAL);

        JScrollPane scroll = new JScrollPane(complaintArea);
        UIConfig.styleScroll(scroll);

        submitBtn = new JButton("Submit Complaint");
        UIConfig.primaryBtn(submitBtn);
        submitBtn.addActionListener(e -> submitComplaint());

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(categoryBox, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(statusLabel, BorderLayout.WEST);

        JPanel action = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        action.setOpaque(false);
        action.add(submitBtn);
        bottom.add(action, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    private JPanel listSection() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        JLabel title = new JLabel("My Complaints");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);

        container.add(title, BorderLayout.NORTH);
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    private void setBusy(boolean busy, String message) {
        statusLabel.setText(message == null ? " " : message);
        submitBtn.setEnabled(!busy);
        categoryBox.setEnabled(!busy);
        complaintArea.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void submitComplaint() {
        String msg = complaintArea.getText().trim();
        String category = (String) categoryBox.getSelectedItem();

        if (msg.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter complaint message");
            return;
        }

        setBusy(true, "Submitting complaint...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return new ComplaintDAO().addComplaint(Session.userId, category, msg);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        complaintArea.setText("");
                        refreshData();
                        JOptionPane.showMessageDialog(ComplaintPanel.this, "Complaint submitted");
                    } else {
                        setBusy(false, "Submit failed");
                        JOptionPane.showMessageDialog(ComplaintPanel.this, "Failed to submit complaint");
                    }
                } catch (Exception ex) {
                    setBusy(false, "Submit failed");
                    JOptionPane.showMessageDialog(ComplaintPanel.this, "Failed to submit complaint");
                }
            }
        };
        worker.execute();
    }

    private void loadMyComplaints() {
        setBusy(true, "Loading complaints...");
        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                return new ComplaintDAO().getComplaintsByUser(Session.userId);
            }

            @Override
            protected void done() {
                try {
                    List<String[]> list = get();
                    listPanel.removeAll();
                    if (list.isEmpty()) {
                        JLabel empty = new JLabel("No complaints yet");
                        empty.setForeground(UIConfig.TEXT_LIGHT);
                        listPanel.add(empty);
                    } else {
                        for (String[] r : list) {
                            listPanel.add(complaintCard(r));
                            listPanel.add(Box.createVerticalStrut(12));
                        }
                    }
                    listPanel.revalidate();
                    listPanel.repaint();
                    setBusy(false, "Loaded " + list.size() + " complaints");
                } catch (Exception ex) {
                    setBusy(false, "Failed to load complaints");
                }
            }
        };
        worker.execute();
    }

    private JPanel complaintCard(String[] r) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(10, 10));

        String category = r[1];
        String message = r[2];
        String status = r[3];
        String reply = r[4];
        String time = r[5];

        JLabel statusLbl = new JLabel(status);
        statusLbl.setOpaque(true);
        statusLbl.setForeground(Color.WHITE);
        statusLbl.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        if ("OPEN".equalsIgnoreCase(status)) {
            statusLbl.setBackground(new Color(243, 156, 18));
        } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            statusLbl.setBackground(new Color(52, 152, 219));
        } else {
            statusLbl.setBackground(new Color(46, 204, 113));
        }

        JLabel cat = new JLabel(category);
        cat.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel timeLbl = new JLabel(time);
        timeLbl.setForeground(UIConfig.TEXT_LIGHT);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(cat, BorderLayout.WEST);
        top.add(statusLbl, BorderLayout.EAST);

        JTextArea msg = new JTextArea(message);
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        msg.setEditable(false);
        msg.setOpaque(false);

        card.add(top, BorderLayout.NORTH);
        card.add(msg, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(timeLbl, BorderLayout.NORTH);

        if (reply != null && !reply.isEmpty()) {
            JLabel rep = new JLabel("Admin Reply:");
            rep.setFont(new Font("Segoe UI", Font.BOLD, 12));

            JTextArea repArea = new JTextArea(reply);
            repArea.setEditable(false);
            repArea.setOpaque(false);
            repArea.setLineWrap(true);
            repArea.setWrapStyleWord(true);

            JPanel replyPanel = new JPanel(new BorderLayout());
            replyPanel.setOpaque(false);
            replyPanel.add(rep, BorderLayout.NORTH);
            replyPanel.add(repArea, BorderLayout.CENTER);
            south.add(replyPanel, BorderLayout.SOUTH);
        }

        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        return card;
    }

    @Override
    public void refreshData() {
        loadMyComplaints();
    }
}
