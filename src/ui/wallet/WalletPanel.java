package ui.wallet;

import config.UIConfig;
import dao.WalletDAO;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class WalletPanel extends JPanel implements Refreshable {

    private JLabel balanceLabel;
    private JTable table;
    private DefaultTableModel model;
    private JButton addMoneyBtn;
    private JLabel loadingLabel;
    private volatile boolean busy = false;
    private volatile long refreshToken = 0L;

    private final WalletDAO walletDAO = new WalletDAO();

    public WalletPanel() {

        setLayout(new BorderLayout(16, 16));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(topCard(), BorderLayout.NORTH);
        add(tableSection(), BorderLayout.CENTER);
    }

    private JPanel topCard() {

        JPanel card = new JPanel(new BorderLayout(12, 12));
        UIConfig.styleCard(card);

        JLabel title = new JLabel("Wallet Balance");
        title.setFont(UIConfig.FONT_SUBTITLE);
        title.setForeground(UIConfig.TEXT);

        balanceLabel = new JLabel("INR 0.00");
        balanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));

        addMoneyBtn = new JButton("Add Money");
        UIConfig.primaryBtn(addMoneyBtn);
        addMoneyBtn.addActionListener(e -> addMoney());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(balanceLabel, BorderLayout.WEST);
        bottom.add(addMoneyBtn, BorderLayout.EAST);

        card.add(title, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);

        return card;
    }

    private JComponent tableSection() {

        model = new DefaultTableModel(
                new String[]{"Date", "Type", "Amount (INR)", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(model);
        UIConfig.styleTable(table);

        JScrollPane sp = new JScrollPane(table);
        UIConfig.styleScroll(sp);
        sp.setColumnHeaderView(table.getTableHeader());

        loadingLabel = new JLabel(" ", SwingConstants.RIGHT);
        loadingLabel.setFont(UIConfig.FONT_SMALL);
        loadingLabel.setForeground(UIConfig.TEXT_LIGHT);

        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(loadingLabel, BorderLayout.SOUTH);
        return wrap;
    }

    private void setBusy(boolean value, String text) {
        busy = value;
        if (addMoneyBtn != null) {
            addMoneyBtn.setEnabled(!value);
        }
        if (loadingLabel != null) {
            loadingLabel.setText(text == null ? " " : text);
        }
        setCursor(Cursor.getPredefinedCursor(value ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void addMoney() {
        if (busy) return;

        String input = JOptionPane.showInputDialog(
                this,
                "Enter amount to add",
                "Add Money",
                JOptionPane.PLAIN_MESSAGE
        );

        if (input == null) return;

        final double amount;
        try {
            amount = Double.parseDouble(input);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Enter valid amount");
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid amount");
            return;
        }

        setBusy(true, "Adding money...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return walletDAO.addMoney(Session.userId, amount);
            }

            @Override
            protected void done() {
                setBusy(false, " ");
                try {
                    boolean ok = get();
                    if (!ok) {
                        JOptionPane.showMessageDialog(WalletPanel.this, "Failed to add money");
                        return;
                    }
                    refreshData();
                    JOptionPane.showMessageDialog(WalletPanel.this, "Money added successfully");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(WalletPanel.this, "Failed to add money\nReason: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    @Override
    public void refreshData() {
        final long token = System.nanoTime();
        refreshToken = token;
        model.setRowCount(0);
        setBusy(true, "Loading wallet...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            double bal;
            List<String[]> transactions;

            @Override
            protected Void doInBackground() {
                bal = walletDAO.getBalance(Session.userId);
                transactions = walletDAO.getTransactions(Session.userId);
                return null;
            }

            @Override
            protected void done() {
                if (token != refreshToken) return;
                setBusy(false, " ");

                balanceLabel.setText("INR " + String.format("%.2f", bal));
                balanceLabel.setForeground(bal <= 0 ? UIConfig.DANGER : UIConfig.SUCCESS);

                boolean found = false;
                if (transactions != null) {
                    for (String[] tx : transactions) {
                        if (tx == null || tx.length < 4) continue;
                        found = true;
                        model.addRow(new Object[]{tx[0], tx[1], tx[2], tx[3]});
                    }
                }

                if (!found) {
                    model.addRow(new Object[]{"-", "No transactions", "-", "-"});
                }
            }
        };

        worker.execute();
    }
}
