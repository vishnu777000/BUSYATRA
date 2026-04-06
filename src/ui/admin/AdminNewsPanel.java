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
    private JLabel statusLabel;
    private JButton publishBtn;
    private JButton refreshBtn;
    private JButton toggleBtn;
    private JButton deleteBtn;
    private volatile boolean busy = false;
    private volatile long lastLoadMs = 0L;

    public AdminNewsPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);

        loadNewsTable();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("News and Notifications");
        title.setFont(UIConfig.FONT_TITLE);

        JLabel sub = new JLabel("Manage announcements for users");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(UIConfig.TEXT_LIGHT);

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        panel.add(left, BorderLayout.WEST);
        return panel;
    }

    private JPanel content() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 20));
        panel.setOpaque(false);
        panel.add(publishCard());
        panel.add(listCard());
        return panel;
    }

    private JPanel publishCard() {
        JPanel card = new JPanel(new BorderLayout(12, 12));
        UIConfig.styleCard(card);

        JLabel h = new JLabel("Publish News");
        h.setFont(UIConfig.FONT_SUBTITLE);

        messageArea = new JTextArea(6, 20);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(UIConfig.FONT_NORMAL);

        JScrollPane scroll = new JScrollPane(messageArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Message"));

        publishBtn = createBtn("Publish");
        UIConfig.primaryBtn(publishBtn);
        publishBtn.addActionListener(e -> publish());

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(statusLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(publishBtn);
        bottom.add(right, BorderLayout.EAST);

        card.add(h, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JPanel listCard() {
        JPanel card = new JPanel(new BorderLayout(12, 12));
        UIConfig.styleCard(card);

        JLabel h = new JLabel("All News");
        h.setFont(UIConfig.FONT_SUBTITLE);

        model = new DefaultTableModel(new Object[]{"ID", "Message", "Active", "Created"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(model);
        UIConfig.styleTable(table);
        table.setRowHeight(32);

        JScrollPane sp = new JScrollPane(table);
        UIConfig.styleScroll(sp);
        sp.setColumnHeaderView(table.getTableHeader());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);

        refreshBtn = createBtn("Refresh");
        toggleBtn = createBtn("Toggle Active");
        deleteBtn = createBtn("Delete");

        UIConfig.infoBtn(refreshBtn);
        UIConfig.successBtn(toggleBtn);
        UIConfig.dangerBtn(deleteBtn);

        refreshBtn.addActionListener(e -> loadNewsTable());
        toggleBtn.addActionListener(e -> toggleStatus());
        deleteBtn.addActionListener(e -> deleteSelected());

        btns.add(refreshBtn);
        btns.add(toggleBtn);
        btns.add(deleteBtn);

        card.add(h, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        card.add(btns, BorderLayout.SOUTH);
        return card;
    }

    private JButton createBtn(String text) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(140, 36));
        return b;
    }

    private void setBusy(boolean busy, String msg) {
        this.busy = busy;
        statusLabel.setText(msg == null ? " " : msg);
        publishBtn.setEnabled(!busy);
        refreshBtn.setEnabled(!busy);
        toggleBtn.setEnabled(!busy);
        deleteBtn.setEnabled(!busy);
        table.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void loadNewsTable() {
        loadNewsTable(false);
    }

    private void loadNewsTable(boolean force) {
        if (busy && !force) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastLoadMs < 3000L && model.getRowCount() > 0) {
            setBusy(false, "Using recent data");
            return;
        }
        setBusy(true, "Loading news...");
        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                return new NewsDAO().getAllNews();
            }

            @Override
            protected void done() {
                try {
                    model.setRowCount(0);
                    for (String[] row : get()) {
                        String status = row[2] == null ? "" : row[2].trim().toUpperCase();
                        model.addRow(new Object[]{
                                row[0],
                                row[1],
                                ("1".equals(status) || "YES".equals(status) || "TRUE".equals(status) || "ACTIVE".equals(status)) ? "YES" : "NO",
                                row[3]
                        });
                    }
                    lastLoadMs = System.currentTimeMillis();
                    setBusy(false, "Loaded " + model.getRowCount() + " rows");
                } catch (Exception e) {
                    setBusy(false, "Load failed");
                    JOptionPane.showMessageDialog(AdminNewsPanel.this, "Load failed");
                }
            }
        };
        worker.execute();
    }

    private void publish() {
        String msg = messageArea.getText().trim();
        if (msg.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter message");
            return;
        }

        setBusy(true, "Publishing...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return new NewsDAO().addNews(msg);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        messageArea.setText("");
                        loadNewsTable(true);
                    } else {
                        setBusy(false, "Publish failed");
                        JOptionPane.showMessageDialog(AdminNewsPanel.this, "Publish failed");
                    }
                } catch (Exception e) {
                    setBusy(false, "Publish failed");
                    JOptionPane.showMessageDialog(AdminNewsPanel.this, "Publish failed");
                }
            }
        };
        worker.execute();
    }

    private void toggleStatus() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select row");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        String status = model.getValueAt(row, 2).toString();
        boolean newStatus = "NO".equals(status);

        setBusy(true, "Updating...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return new NewsDAO().setNewsStatus(id, newStatus);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        loadNewsTable(true);
                    } else {
                        setBusy(false, "Update failed");
                        JOptionPane.showMessageDialog(AdminNewsPanel.this, "Update failed");
                    }
                } catch (Exception e) {
                    setBusy(false, "Update failed");
                    JOptionPane.showMessageDialog(AdminNewsPanel.this, "Update failed");
                }
            }
        };
        worker.execute();
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select row");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Delete news?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        setBusy(true, "Deleting...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return new NewsDAO().deleteNews(id);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        loadNewsTable(true);
                    } else {
                        setBusy(false, "Delete failed");
                        JOptionPane.showMessageDialog(AdminNewsPanel.this, "Delete failed");
                    }
                } catch (Exception e) {
                    setBusy(false, "Delete failed");
                    JOptionPane.showMessageDialog(AdminNewsPanel.this, "Delete failed");
                }
            }
        };
        worker.execute();
    }

    @Override
    public void refreshData() {
        loadNewsTable();
    }

    @Override
    public boolean refreshOnFirstShow() {
        return false;
    }
}
