package ui.wallet;

import config.UIConfig;
import dao.WalletDAO;
import util.Session;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.ResultSet;

public class WalletPanel extends JPanel implements Refreshable {

    private JLabel balanceLabel;
    private JTable table;
    private DefaultTableModel model;

    // 🔥 SINGLE DAO INSTANCE
    private final WalletDAO walletDAO = new WalletDAO();

    public WalletPanel() {

        setLayout(new BorderLayout(16, 16));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(topCard(), BorderLayout.NORTH);
        add(tableSection(), BorderLayout.CENTER);

        refreshData();
    }

    /* =================================================
       TOP CARD : BALANCE + ADD MONEY
       ================================================= */
    private JPanel topCard() {

        JPanel card = new JPanel(new BorderLayout(12, 12));
        UIConfig.styleCard(card);

        JLabel title = new JLabel("Wallet Balance");
        title.setFont(UIConfig.FONT_SUBTITLE);
        title.setForeground(UIConfig.TEXT);

        balanceLabel = new JLabel("₹ 0.00");
        balanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));

        JButton addMoneyBtn = new JButton("Add Money");
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

    /* =================================================
       TRANSACTION TABLE
       ================================================= */
    private JScrollPane tableSection() {

        model = new DefaultTableModel(
                new String[]{"Date", "Type", "Amount (₹)", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(UIConfig.FONT_NORMAL);
        table.setSelectionBackground(new Color(232, 245, 233));

        JTableHeader header = table.getTableHeader();
        header.setFont(UIConfig.FONT_SMALL);
        header.setBackground(UIConfig.PRIMARY);
        header.setForeground(Color.WHITE);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConfig.BORDER));
        sp.getViewport().setBackground(UIConfig.BACKGROUND);

        return sp;
    }

    /* =================================================
       ADD MONEY
       ================================================= */
    private void addMoney() {

        String input = JOptionPane.showInputDialog(
                this,
                "Enter amount to add",
                "Add Money",
                JOptionPane.PLAIN_MESSAGE
        );

        if (input == null) return;

        try {
            double amount = Double.parseDouble(input);

            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Enter valid amount");
                return;
            }

            if (!walletDAO.addMoney(Session.userId, amount)) {
                JOptionPane.showMessageDialog(this, "Failed to add money");
                return;
            }

            refreshData();
            JOptionPane.showMessageDialog(this, "Money added successfully");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid amount");
        }
    }

    /* =================================================
       LOAD BALANCE
       ================================================= */
    private void loadBalance() {

        double bal = walletDAO.getBalance(Session.userId);
        balanceLabel.setText("₹ " + String.format("%.2f", bal));

        if (bal <= 0) {
            balanceLabel.setForeground(UIConfig.DANGER);
        } else {
            balanceLabel.setForeground(UIConfig.SUCCESS);
        }
    }

    /* =================================================
       LOAD TRANSACTIONS
       ================================================= */
    private void loadTransactions() {

        model.setRowCount(0);

        try {
            ResultSet rs = walletDAO.getTransactions(Session.userId);
            boolean found = false;

            while (rs != null && rs.next()) {
                found = true;

                model.addRow(new Object[]{
                        rs.getString("created_at"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getString("status")
                });
            }

            if (!found) {
                model.addRow(
                        new Object[]{"-", "No transactions", "-", "-"}
                );
            }

        } catch (Exception e) {
            model.addRow(
                    new Object[]{"-", "Error loading data", "-", "-"}
            );
        }
    }

    /* =================================================
       REFRESH
       ================================================= */
    @Override
    public void refreshData() {
        loadBalance();
        loadTransactions();
    }
}
