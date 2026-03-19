package ui.settings;

import config.UIConfig;
import dao.UserDAO;
import util.PreferencesUtil;
import util.Session;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {

    /* ===== Preferences ===== */

    private JComboBox<String> themeBox;
    private JComboBox<String> languageBox;
    private JComboBox<String> dateFormatBox;
    private JComboBox<String> currencyBox;

    private JCheckBox notifEnable;
    private JCheckBox emailNotif;
    private JCheckBox smsNotif;
    private JCheckBox promoNotif;
    private JCheckBox soundNotif;

    private JCheckBox autoLogout;
    private JCheckBox rememberFilters;
    private JCheckBox confirmBeforePay;
    private JCheckBox showTooltips;
    private JCheckBox compactMode;

    /* ===== Security ===== */

    private JPasswordField oldPass;
    private JPasswordField newPass;
    private JPasswordField confirmPass;

    private JCheckBox twoFactor;
    private JCheckBox deviceLock;
    private JCheckBox hideBalance;
    private JCheckBox loginAlerts;

    public SettingsPanel() {

        setLayout(new BorderLayout(20,20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JLabel title = new JLabel("Settings");
        title.setFont(UIConfig.FONT_TITLE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("General", generalTab());
        tabs.add("Notifications", notificationTab());
        tabs.add("Privacy & Security", securityTab());

        add(title, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    /* =================================================
       GENERAL SETTINGS
       ================================================= */

    private JPanel generalTab() {

        JPanel card = cardPanel(10);

        themeBox = combo("LIGHT","DARK","SYSTEM");
        languageBox = combo("English","Hindi","German");
        dateFormatBox = combo("dd-MM-yyyy","yyyy-MM-dd","MM/dd/yyyy");
        currencyBox = combo("INR (₹)","USD ($)","EUR (€)");

        autoLogout = check("Auto logout on inactivity");
        rememberFilters = check("Remember search filters");
        confirmBeforePay = check("Confirm before payment");
        showTooltips = check("Show tooltips & hints");
        compactMode = check("Compact UI mode");

        /* ===== REALTIME EVENTS ===== */

        themeBox.addActionListener(e -> applyTheme());

        autoLogout.addActionListener(e ->
                PreferencesUtil.setAutoLogout(autoLogout.isSelected()));

        rememberFilters.addActionListener(e ->
                PreferencesUtil.setRememberFilters(rememberFilters.isSelected()));

        confirmBeforePay.addActionListener(e ->
                PreferencesUtil.setConfirmBeforePayment(confirmBeforePay.isSelected()));

        showTooltips.addActionListener(e ->
                PreferencesUtil.setShowTooltips(showTooltips.isSelected()));

        compactMode.addActionListener(e ->
                PreferencesUtil.setCompactMode(compactMode.isSelected()));

        card.add(label("Theme Mode")); card.add(themeBox);
        card.add(label("Language")); card.add(languageBox);
        card.add(label("Date Format")); card.add(dateFormatBox);
        card.add(label("Currency")); card.add(currencyBox);

        card.add(autoLogout);
        card.add(rememberFilters);
        card.add(confirmBeforePay);
        card.add(showTooltips);
        card.add(compactMode);

        card.add(new JLabel());
        card.add(new JLabel());

        return card;
    }

    /* =================================================
       NOTIFICATIONS
       ================================================= */

    private JPanel notificationTab() {

        JPanel card = cardPanel(8);

        notifEnable = check("Enable notifications");
        emailNotif = check("Email notifications");
        smsNotif = check("SMS alerts");
        promoNotif = check("Promotional messages");
        soundNotif = check("Sound alerts");

        notifEnable.addActionListener(e ->
                PreferencesUtil.setNotifications(notifEnable.isSelected()));

        emailNotif.addActionListener(e ->
                PreferencesUtil.setEmailNotifications(emailNotif.isSelected()));

        smsNotif.addActionListener(e ->
                PreferencesUtil.setSMSNotifications(smsNotif.isSelected()));

        promoNotif.addActionListener(e ->
                PreferencesUtil.setPromoNotifications(promoNotif.isSelected()));

        soundNotif.addActionListener(e ->
                PreferencesUtil.setSoundNotifications(soundNotif.isSelected()));

        card.add(notifEnable);
        card.add(emailNotif);
        card.add(smsNotif);
        card.add(promoNotif);
        card.add(soundNotif);

        card.add(new JLabel());
        card.add(new JLabel());
        card.add(new JLabel());

        return card;
    }

    /* =================================================
       SECURITY
       ================================================= */

    private JPanel securityTab() {

        JPanel card = cardPanel(10);

        oldPass = password("Current Password");
        newPass = password("New Password");
        confirmPass = password("Confirm New Password");

        twoFactor = check("Enable Two-Factor Authentication (2FA)");
        deviceLock = check("Restrict login to trusted devices");
        hideBalance = check("Hide wallet balance by default");
        loginAlerts = check("Login alerts on new device");

        twoFactor.addActionListener(e ->
                PreferencesUtil.setTwoFactor(twoFactor.isSelected()));

        deviceLock.addActionListener(e ->
                PreferencesUtil.setDeviceLock(deviceLock.isSelected()));

        hideBalance.addActionListener(e ->
                PreferencesUtil.setHideBalance(hideBalance.isSelected()));

        loginAlerts.addActionListener(e ->
                PreferencesUtil.setLoginAlerts(loginAlerts.isSelected()));

        JButton change = primaryBtn("Change Password", UIConfig.SUCCESS);
        change.addActionListener(e -> changePassword());

        card.add(oldPass);
        card.add(newPass);
        card.add(confirmPass);
        card.add(twoFactor);
        card.add(deviceLock);
        card.add(hideBalance);
        card.add(loginAlerts);
        card.add(new JLabel());
        card.add(new JLabel());
        card.add(change);

        return card;
    }

    /* =================================================
       REALTIME THEME APPLY
       ================================================= */

    private void applyTheme() {

        String theme = themeBox.getSelectedItem().toString();

        PreferencesUtil.setTheme(theme);

        SwingUtilities.updateComponentTreeUI(
                SwingUtilities.getWindowAncestor(this));
    }

    /* =================================================
       PASSWORD CHANGE
       ================================================= */

    private void changePassword() {

        String oldP = new String(oldPass.getPassword());
        String newP = new String(newPass.getPassword());
        String conf = new String(confirmPass.getPassword());

        if (oldP.isEmpty() || newP.isEmpty() || conf.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all password fields!");
            return;
        }

        if (!newP.equals(conf)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match!");
            return;
        }

        boolean ok = new UserDAO().changePassword(Session.userId, oldP, newP);

        if (ok) {

            JOptionPane.showMessageDialog(this, "Password updated successfully ✅");

            oldPass.setText("");
            newPass.setText("");
            confirmPass.setText("");

        } else {
            JOptionPane.showMessageDialog(this, "Incorrect current password ❌");
        }
    }

    /* =================================================
       UI HELPERS
       ================================================= */

    private JPanel cardPanel(int rows) {
        JPanel p = new JPanel(new GridLayout(rows,2,12,12));
        UIConfig.styleCard(p);
        return p;
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(UIConfig.FONT_SMALL);
        return l;
    }

    private JComboBox<String> combo(String... items) {
        JComboBox<String> b = new JComboBox<>(items);
        b.setFont(UIConfig.FONT_NORMAL);
        return b;
    }

    private JCheckBox check(String text) {
        JCheckBox c = new JCheckBox(text);
        c.setOpaque(false);
        c.setFont(UIConfig.FONT_NORMAL);
        return c;
    }

    private JPasswordField password(String title) {
        JPasswordField p = new JPasswordField();
        p.setBorder(BorderFactory.createTitledBorder(title));
        return p;
    }

    private JButton primaryBtn(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }
}

