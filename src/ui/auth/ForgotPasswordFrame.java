package ui.auth;

import config.UIConfig;
import dao.OTPDAO;
import dao.UserDAO;
import util.EmailUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class ForgotPasswordFrame extends JFrame {

    private JTextField emailField;
    private JTextField otpField;
    private JPasswordField newPassField;

    private JButton sendOtpBtn;
    private JButton verifyBtn;
    private JLabel statusLabel;

    public ForgotPasswordFrame() {

        setTitle("Forgot Password - BusYatra");
        setSize(520, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        getContentPane().setBackground(UIConfig.BACKGROUND);
        setLayout(new GridBagLayout());

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        UIConfig.styleCard(card);
        card.setPreferredSize(new Dimension(420, 420));

        card.add(header());
        card.add(Box.createVerticalStrut(15));
        card.add(form());
        card.add(Box.createVerticalStrut(15));
        card.add(footer());

        add(card);
        setVisible(true);
    }

    private JPanel header() {

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel title = new JLabel("Forgot Password");
        title.setFont(UIConfig.FONT_TITLE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Reset your password using OTP");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(UIConfig.TEXT_LIGHT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(title);
        p.add(Box.createVerticalStrut(5));
        p.add(sub);

        return p;
    }

    private JPanel form() {

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        emailField = field("Email Address");
        otpField = field("Enter OTP");
        newPassField = passwordField("New Password");

        sendOtpBtn = btn("Send OTP");
        verifyBtn = btn("Verify & Reset Password");
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        UIConfig.infoBtn(sendOtpBtn);
        UIConfig.primaryBtn(verifyBtn);

        verifyBtn.setEnabled(false);

        sendOtpBtn.addActionListener(e -> sendOTP());
        verifyBtn.addActionListener(e -> verifyOTPAndReset());
        enableOtpFields(false);

        form.add(emailField);
        form.add(Box.createVerticalStrut(12));
        form.add(sendOtpBtn);
        form.add(Box.createVerticalStrut(18));
        form.add(otpField);
        form.add(Box.createVerticalStrut(12));
        form.add(newPassField);
        form.add(Box.createVerticalStrut(12));
        form.add(statusLabel);
        form.add(Box.createVerticalStrut(12));
        form.add(verifyBtn);

        return form;
    }

    private JPanel footer() {

        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        p.setOpaque(false);

        JButton back = new JButton("<- Back to Login");
        UIConfig.secondaryBtn(back);

        back.setPreferredSize(new Dimension(200, 36));

        back.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });

        p.add(back);
        return p;
    }

    private JTextField field(String title) {

        JTextField f = new JTextField();
        UIConfig.styleField(f);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        f.setBorder(BorderFactory.createTitledBorder(title));
        return f;
    }

    private JPasswordField passwordField(String title) {

        JPasswordField f = new JPasswordField();
        UIConfig.styleField(f);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        f.setBorder(BorderFactory.createTitledBorder(title));
        return f;
    }

    private JButton btn(String text) {

        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return b;
    }

    private void setBusy(boolean busy, String message) {
        if (sendOtpBtn != null) {
            sendOtpBtn.setEnabled(!busy);
        }
        if (verifyBtn != null && otpField != null && newPassField != null) {
            verifyBtn.setEnabled(!busy && otpField.isEnabled() && newPassField.isEnabled());
        }
        if (emailField != null) {
            emailField.setEnabled(!busy);
        }
        if (otpField != null) {
            otpField.setEnabled(!busy && otpField.isEditable());
        }
        if (newPassField != null) {
            newPassField.setEnabled(!busy && newPassField.isEditable());
        }
        if (statusLabel != null) {
            statusLabel.setText(message == null ? " " : message);
        }
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void enableOtpFields(boolean enabled) {
        if (otpField != null) {
            otpField.setEditable(enabled);
            otpField.setEnabled(enabled);
        }
        if (newPassField != null) {
            newPassField.setEditable(enabled);
            newPassField.setEnabled(enabled);
        }
        if (verifyBtn != null) {
            verifyBtn.setEnabled(enabled);
        }
    }

    private void sendOTP() {

        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter email");
            return;
        }

        String otp = String.format("%06d", new Random().nextInt(999999));
        setBusy(true, "Sending OTP...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                if (!EmailUtil.sendOTP(email, otp)) {
                    return false;
                }
                return new OTPDAO().saveOTP(email, otp);
            }

            @Override
            protected void done() {
                try {
                    boolean sent = Boolean.TRUE.equals(get());
                    if (sent) {
                        enableOtpFields(true);
                        setBusy(false, "OTP sent");
                        JOptionPane.showMessageDialog(ForgotPasswordFrame.this, "OTP sent");
                    } else {
                        setBusy(false, "Failed to send OTP");
                        JOptionPane.showMessageDialog(ForgotPasswordFrame.this, "Failed to send OTP");
                    }
                } catch (Exception e) {
                    setBusy(false, "Failed to send OTP");
                    JOptionPane.showMessageDialog(ForgotPasswordFrame.this, "Failed to send OTP");
                }
            }
        };

        worker.execute();
    }

    private void verifyOTPAndReset() {

        String email = emailField.getText().trim();
        String otp = otpField.getText().trim();
        String pass = new String(newPassField.getPassword()).trim();

        if (email.isEmpty() || otp.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields");
            return;
        }

        setBusy(true, "Verifying OTP...");

        SwingWorker<ResetResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ResetResult doInBackground() {
                OTPDAO otpDAO = new OTPDAO();
                UserDAO userDAO = new UserDAO();

                if (!otpDAO.isValidOTP(email, otp)) {
                    return ResetResult.fail("Invalid or expired OTP");
                }
                if (!userDAO.updatePasswordByEmail(email, pass)) {
                    return ResetResult.fail(userDAO.getLastError());
                }
                otpDAO.markUsed(email, otp);
                return ResetResult.success();
            }

            @Override
            protected void done() {
                try {
                    ResetResult result = get();
                    if (!result.success) {
                        setBusy(false, result.message);
                        JOptionPane.showMessageDialog(ForgotPasswordFrame.this, result.message);
                        return;
                    }

                    setBusy(false, "Password updated");
                    JOptionPane.showMessageDialog(ForgotPasswordFrame.this, "Password updated");

                    dispose();
                    new LoginFrame();
                } catch (Exception e) {
                    setBusy(false, "Reset failed");
                    JOptionPane.showMessageDialog(ForgotPasswordFrame.this, "Reset failed");
                }
            }
        };

        worker.execute();
    }

    private static class ResetResult {
        private final boolean success;
        private final String message;

        private ResetResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        private static ResetResult success() {
            return new ResetResult(true, "Password updated");
        }

        private static ResetResult fail(String message) {
            return new ResetResult(false, message == null || message.isBlank() ? "Reset failed" : message);
        }
    }
}
