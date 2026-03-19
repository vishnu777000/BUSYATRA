package ui.clerk;

import config.UIConfig;
import ui.common.MainFrame;
import util.IconUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ClerkQuickActionsPanel extends JPanel {

    private final MainFrame frame;

    public ClerkQuickActionsPanel(MainFrame frame) {

        this.frame = frame;

        setLayout(new GridLayout(5, 1, 12, 12));
        setBackground(UIConfig.CARD);
        setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConfig.BORDER),
                        BorderFactory.createEmptyBorder(15, 15, 15, 15)
                )
        );

        JButton bookBtn   = actionButton("New Booking   (F1)", "book.png");
        JButton cancelBtn = actionButton("Cancel Ticket (F2)", "cancel.png");
        JButton searchBtn = actionButton("Search Ticket (F3)", "search.png");
        JButton printBtn  = actionButton("Print Ticket  (F4)", "printing.png");
        JButton walletBtn = actionButton("Wallet / Cash (F5)", "wallet.png");

        /* ================= ACTIONS ================= */

        bookBtn.addActionListener(e ->
                frame.showScreen(MainFrame.SCREEN_SEARCH)
        );

        cancelBtn.addActionListener(e ->
                frame.showScreen(MainFrame.SCREEN_CANCEL)
        );

        searchBtn.addActionListener(e ->
                frame.showScreen(MainFrame.SCREEN_MY_TICKETS)
        );

        printBtn.addActionListener(e ->
                frame.showScreen(MainFrame.SCREEN_TICKET_PREVIEW)
        );

        walletBtn.addActionListener(e ->
                frame.showScreen(MainFrame.SCREEN_WALLET)
        );

        add(bookBtn);
        add(cancelBtn);
        add(searchBtn);
        add(printBtn);
        add(walletBtn);

        registerShortcuts(
                bookBtn,
                cancelBtn,
                searchBtn,
                printBtn,
                walletBtn
        );
    }

    /* ================= BUTTON ================= */

    private JButton actionButton(String text, String icon) {

        JButton btn = new JButton(
                text,
                IconUtil.load(icon, 20, 20)
        );

        UIConfig.secondaryBtn(btn);

        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(12);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConfig.BORDER),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                )
        );

        return btn;
    }

    /* ================= SHORTCUTS ================= */

    private void registerShortcuts(
            JButton book,
            JButton cancel,
            JButton search,
            JButton print,
            JButton wallet
    ) {

        bindKey("F1", book);
        bindKey("F2", cancel);
        bindKey("F3", search);
        bindKey("F4", print);
        bindKey("F5", wallet);
    }

    private void bindKey(String key, JButton btn) {

        String actionKey = "ACTION_" + key;

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), actionKey);

        getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btn.doClick();
            }
        });
    }
}