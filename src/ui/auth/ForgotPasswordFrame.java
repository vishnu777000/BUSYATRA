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

public ForgotPasswordFrame(){

    setTitle("Forgot Password - BusYatra");
    setSize(520,520);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    getContentPane().setBackground(UIConfig.BACKGROUND);
    setLayout(new GridBagLayout());

    JPanel card = new JPanel();
    card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
    UIConfig.styleCard(card);
    card.setPreferredSize(new Dimension(420,420));

    card.add(header());
    card.add(Box.createVerticalStrut(15));
    card.add(form());
    card.add(Box.createVerticalStrut(15));
    card.add(footer());

    add(card);
    setVisible(true);
}

/* ================= HEADER ================= */

private JPanel header(){

    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
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

/* ================= FORM ================= */

private JPanel form(){

    JPanel form = new JPanel();
    form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
    form.setOpaque(false);

    emailField = field("Email Address");
    otpField = field("Enter OTP");
    newPassField = passwordField("New Password");

    otpField.setEnabled(false);
    newPassField.setEnabled(false);

    sendOtpBtn = btn("Send OTP");
    verifyBtn = btn("Verify & Reset Password");

    UIConfig.infoBtn(sendOtpBtn);
    UIConfig.primaryBtn(verifyBtn);

    verifyBtn.setEnabled(false);

    sendOtpBtn.addActionListener(e -> sendOTP());
    verifyBtn.addActionListener(e -> verifyOTPAndReset());

    form.add(emailField);
    form.add(Box.createVerticalStrut(12));
    form.add(sendOtpBtn);
    form.add(Box.createVerticalStrut(18));
    form.add(otpField);
    form.add(Box.createVerticalStrut(12));
    form.add(newPassField);
    form.add(Box.createVerticalStrut(18));
    form.add(verifyBtn);

    return form;
}

/* ================= FOOTER ================= */

private JPanel footer(){

    JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
    p.setOpaque(false);

    JButton back = new JButton("← Back to Login");
    UIConfig.secondaryBtn(back);

    back.setPreferredSize(new Dimension(200,36));

    back.addActionListener(e -> {
        dispose();
        new LoginFrame();
    });

    p.add(back);
    return p;
}

/* ================= COMPONENT HELPERS ================= */

private JTextField field(String title){

    JTextField f = new JTextField();
    UIConfig.styleField(f);
    f.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));
    f.setBorder(BorderFactory.createTitledBorder(title));
    return f;
}

private JPasswordField passwordField(String title){

    JPasswordField f = new JPasswordField();
    UIConfig.styleField(f);
    f.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));
    f.setBorder(BorderFactory.createTitledBorder(title));
    return f;
}

private JButton btn(String text){

    JButton b = new JButton(text);
    b.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
    return b;
}

/* ================= LOGIC ================= */

private void sendOTP(){

    String email = emailField.getText().trim();

    if(email.isEmpty()){
        JOptionPane.showMessageDialog(this,"Enter email");
        return;
    }

    String otp = String.format("%06d", new Random().nextInt(999999));

    boolean mailSent = EmailUtil.sendOTP(email, otp);

    if(mailSent){

        new OTPDAO().saveOTP(email, otp);

        otpField.setEnabled(true);
        newPassField.setEnabled(true);
        verifyBtn.setEnabled(true);

        JOptionPane.showMessageDialog(this,"OTP sent ✅");

    }else{
        JOptionPane.showMessageDialog(this,"Failed ❌");
    }
}

private void verifyOTPAndReset(){

    String email = emailField.getText().trim();
    String otp = otpField.getText().trim();
    String pass = new String(newPassField.getPassword()).trim();

    if(email.isEmpty() || otp.isEmpty() || pass.isEmpty()){
        JOptionPane.showMessageDialog(this,"Fill all fields");
        return;
    }

    OTPDAO dao = new OTPDAO();

    if(!dao.verifyOTP(email, otp)){
        JOptionPane.showMessageDialog(this,"Invalid OTP ❌");
        return;
    }

    boolean ok = new UserDAO().updatePasswordByEmail(email, pass);

    if(ok){

        dao.markUsed(email, otp);

        JOptionPane.showMessageDialog(this,"Password Updated ✅");

        dispose();
        new LoginFrame();

    }else{
        JOptionPane.showMessageDialog(this,"Failed ❌");
    }
}


}
