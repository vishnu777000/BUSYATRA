package ui.common;

import config.UIConfig;
import dao.WalletDAO;
import ui.auth.LoginFrame;
import util.Session;
import util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class ProfileDialog extends JDialog {

    private JLabel walletLabel;

    public ProfileDialog(JFrame parent, MainFrame frame) {

        super(parent, "Profile", true);

        setSize(420, 420);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIConfig.BACKGROUND);
        setResizable(false);

        add(mainCard(frame), BorderLayout.CENTER);
        loadWalletBalance();

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        setVisible(true);
    }

    private JComponent mainCard(MainFrame frame) {

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        UIConfig.styleCard(card);

        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("My Profile");
        title.setFont(UIConfig.FONT_TITLE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Account Information");
        subtitle.setFont(UIConfig.FONT_SMALL);
        subtitle.setForeground(UIConfig.TEXT_LIGHT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(5));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(15));

        card.add(infoRow("Name", Session.username));
        card.add(infoRow("Role", Session.role));
        card.add(infoRow("Email", Session.userEmail));

        card.add(Box.createVerticalStrut(10));

        walletLabel = new JLabel("Wallet Balance : Loading...");
        walletLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        walletLabel.setForeground(UIConfig.TEXT_LIGHT);
        walletLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(walletLabel);
        card.add(Box.createVerticalStrut(15));

        JPanel btnRow = new JPanel(new GridLayout(1, 3, 10, 0));
        btnRow.setOpaque(false);

        JButton settingsBtn = new JButton("Settings");
        UIConfig.infoBtn(settingsBtn);

        JButton logoutBtn = new JButton("Logout");
        UIConfig.dangerBtn(logoutBtn);

        JButton closeBtn = new JButton("Close");
        UIConfig.primaryBtn(closeBtn);

        settingsBtn.addActionListener(e -> {
            dispose();
            frame.showScreen(MainFrame.SCREEN_SETTINGS);
        });

        logoutBtn.addActionListener(e -> {
            dispose();
            SessionManager.clear();
            Session.clear();
            frame.dispose();
            new LoginFrame().setVisible(true);
        });

        closeBtn.addActionListener(e -> dispose());

        btnRow.add(settingsBtn);
        btnRow.add(logoutBtn);
        btnRow.add(closeBtn);

        card.add(btnRow);

        return card;
    }

    private void loadWalletBalance() {
        SwingWorker<Double, Void> worker = new SwingWorker<>() {
            @Override
            protected Double doInBackground() {
                return new WalletDAO().getBalance(Session.userId);
            }

            @Override
            protected void done() {
                if (walletLabel == null || !isDisplayable()) {
                    return;
                }

                try {
                    double balance = get();
                    walletLabel.setText("Wallet Balance : INR " + String.format("%.2f", balance));
                    walletLabel.setForeground(balance <= 0 ? UIConfig.TEXT_LIGHT : UIConfig.SUCCESS);
                } catch (Exception e) {
                    walletLabel.setText("Wallet Balance : Unavailable");
                    walletLabel.setForeground(UIConfig.TEXT_LIGHT);
                }
            }
        };

        worker.execute();
    }

    private JComponent infoRow(String key, String value) {

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel left = new JLabel(key);
        left.setFont(UIConfig.FONT_SMALL);
        left.setForeground(UIConfig.TEXT_LIGHT);

        JLabel right = new JLabel(value);
        right.setFont(UIConfig.FONT_NORMAL);
        right.setForeground(UIConfig.TEXT);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);

        return row;
    }
}
