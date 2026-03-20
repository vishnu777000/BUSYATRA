package ui.admin;

import config.UIConfig;
import dao.ComplaintDAO;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminComplaintsPanel extends JPanel implements Refreshable {

    private JTable table;
    private DefaultTableModel model;
    private JTextArea replyArea;
    private JLabel statusLabel;
    private JButton refreshBtn;
    private JButton progressBtn;
    private JButton resolveBtn;

    public AdminComplaintsPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(centerPanel(), BorderLayout.CENTER);

        loadAllComplaints();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Complaints Management");
        title.setFont(UIConfig.FONT_TITLE);

        JLabel sub = new JLabel("Review, respond and resolve complaints");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(UIConfig.TEXT_LIGHT);

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        panel.add(left, BorderLayout.WEST);
        return panel;
    }

    private JPanel centerPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.add(tableCard(), BorderLayout.CENTER);
        panel.add(actionCard(), BorderLayout.EAST);
        return panel;
    }

    private JPanel tableCard() {
        model = new DefaultTableModel(new Object[]{"ID", "User Email", "Category", "Message", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
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
        UIConfig.styleScroll(sp);

        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel actionCard() {
        JPanel card = new JPanel(new BorderLayout(14, 14));
        UIConfig.styleCard(card);
        card.setPreferredSize(new Dimension(340, 0));

        JLabel heading = new JLabel("Take Action");
        heading.setFont(UIConfig.FONT_SUBTITLE);

        replyArea = new JTextArea(6, 20);
        replyArea.setLineWrap(true);
        replyArea.setWrapStyleWord(true);
        replyArea.setFont(UIConfig.FONT_NORMAL);

        JScrollPane replyScroll = new JScrollPane(replyArea);
        replyScroll.setBorder(BorderFactory.createTitledBorder("Admin Reply"));

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        refreshBtn = createBtn("Refresh");
        progressBtn = createBtn("In Progress");
        resolveBtn = createBtn("Resolve");

        UIConfig.infoBtn(refreshBtn);
        UIConfig.primaryBtn(progressBtn);
        UIConfig.successBtn(resolveBtn);

        refreshBtn.addActionListener(e -> loadAllComplaints());
        progressBtn.addActionListener(e -> updateStatus("IN_PROGRESS"));
        resolveBtn.addActionListener(e -> updateStatus("RESOLVED"));

        JPanel btns = new JPanel(new GridLayout(3, 1, 10, 10));
        btns.setOpaque(false);
        btns.add(refreshBtn);
        btns.add(progressBtn);
        btns.add(resolveBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(statusLabel, BorderLayout.NORTH);
        bottom.add(btns, BorderLayout.CENTER);

        card.add(heading, BorderLayout.NORTH);
        card.add(replyScroll, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JButton createBtn(String text) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(140, 36));
        return b;
    }

    private void setBusy(boolean busy, String message) {
        statusLabel.setText(message == null ? " " : message);
        table.setEnabled(!busy);
        replyArea.setEnabled(!busy);
        refreshBtn.setEnabled(!busy);
        progressBtn.setEnabled(!busy);
        resolveBtn.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void loadAllComplaints() {
        setBusy(true, "Loading complaints...");
        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                return new ComplaintDAO().getAllComplaints();
            }

            @Override
            protected void done() {
                try {
                    model.setRowCount(0);
                    for (String[] row : get()) {
                        model.addRow(row);
                    }
                    setBusy(false, "Loaded " + model.getRowCount() + " complaints");
                } catch (Exception e) {
                    setBusy(false, "Failed to load complaints");
                    JOptionPane.showMessageDialog(AdminComplaintsPanel.this, "Failed to load complaints");
                }
            }
        };
        worker.execute();
    }

    private void updateStatus(String status) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a complaint");
            return;
        }

        String reply = replyArea.getText().trim();
        if (reply.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Reply cannot be empty");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        setBusy(true, "Updating complaint...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return new ComplaintDAO().updateComplaint(id, status, reply);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        replyArea.setText("");
                        loadAllComplaints();
                    } else {
                        setBusy(false, "Update failed");
                        JOptionPane.showMessageDialog(AdminComplaintsPanel.this, "Update failed");
                    }
                } catch (Exception e) {
                    setBusy(false, "Update failed");
                    JOptionPane.showMessageDialog(AdminComplaintsPanel.this, "Update failed");
                }
            }
        };
        worker.execute();
    }

    @Override
    public void refreshData() {
        replyArea.setText("");
        loadAllComplaints();
    }
}
