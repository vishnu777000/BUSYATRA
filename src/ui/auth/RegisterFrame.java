package ui.auth;

import config.UIConfig;
import dao.UserDAO;
import util.IconUtil;

import javax.swing.*;
import java.awt.*;

public class RegisterFrame extends JFrame {

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passField;
    private JButton registerBtn;

    public RegisterFrame() {

        setTitle("BusYatra - Register");
        setSize(600, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        getContentPane().setBackground(UIConfig.BACKGROUND);
        setLayout(new BorderLayout());

        add(topBanner(), BorderLayout.NORTH);
        add(centerCard(), BorderLayout.CENTER);

        setVisible(true);
    }

    private JPanel topBanner() {

        JPanel banner = new JPanel() {
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g;

                GradientPaint gp = new GradientPaint(
                        0, 0, UIConfig.PRIMARY,
                        getWidth(), getHeight(), UIConfig.PRIMARY.darker()
                );

                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        banner.setPreferredSize(new Dimension(600, 140));
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel(IconUtil.load("user.png", 60, 60));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Join BusYatra and travel smarter");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(Color.WHITE);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        banner.add(Box.createVerticalStrut(12));
        banner.add(icon);
        banner.add(Box.createVerticalStrut(6));
        banner.add(title);
        banner.add(sub);

        return banner;
    }

    private JPanel centerCard() {

        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(UIConfig.BACKGROUND);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 400));

        UIConfig.styleCard(card);

        card.add(cardHeader());
        card.add(Box.createVerticalStrut(20));
        card.add(cardForm());
        card.add(Box.createVerticalStrut(15));
        card.add(cardFooter());

        bg.add(card);

        return bg;
    }

    private JPanel cardHeader() {

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel h = new JLabel("Register");
        h.setFont(UIConfig.FONT_TITLE);
        h.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Create your BusYatra account");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(UIConfig.TEXT_LIGHT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(h);
        p.add(Box.createVerticalStrut(5));
        p.add(sub);

        return p;
    }

    private JPanel cardForm() {

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        nameField = field("Full Name");
        emailField = field("Email Address");
        passField = passwordField("Password");

        passField.addActionListener(e -> registerUser());

        registerBtn = btn("Create Account");
        UIConfig.primaryBtn(registerBtn);

        registerBtn.addActionListener(e -> registerUser());

        form.add(nameField);
        form.add(Box.createVerticalStrut(12));
        form.add(emailField);
        form.add(Box.createVerticalStrut(12));
        form.add(passField);
        form.add(Box.createVerticalStrut(18));
        form.add(registerBtn);

        return form;
    }

    private JPanel cardFooter() {

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);

        JButton back = new JButton("<- Back to Login");
        UIConfig.secondaryBtn(back);

        back.setPreferredSize(new Dimension(200, 36));

        back.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });

        footer.add(back);

        return footer;
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
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    private void registerUser() {

        String n = nameField.getText().trim();
        String e = emailField.getText().trim();
        String p = new String(passField.getPassword()).trim();

        if (n.isEmpty() || e.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields");
            return;
        }

        if (!e.contains("@")) {
            JOptionPane.showMessageDialog(this, "Invalid email");
            return;
        }

        if (p.length() < 4) {
            JOptionPane.showMessageDialog(this, "Password too short");
            return;
        }

        setRegisterLoading(true);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private final UserDAO userDAO = new UserDAO();
            private String errorMessage = "Registration failed.";

            @Override
            protected Boolean doInBackground() {
                boolean ok = userDAO.registerUser(n, e, p);
                if (!ok) {
                    errorMessage = userDAO.getLastError();
                }
                return ok;
            }

            @Override
            protected void done() {
                setRegisterLoading(false);
                try {
                    boolean ok = get();
                    if (ok) {
                        JOptionPane.showMessageDialog(RegisterFrame.this, "Registered successfully.");
                        dispose();
                        new LoginFrame();
                    } else {
                        JOptionPane.showMessageDialog(
                                RegisterFrame.this,
                                "Registration failed.\nReason: " + errorMessage,
                                "Registration Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            RegisterFrame.this,
                            "Registration failed.\nReason: " + ex.getMessage(),
                            "Registration Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    private void setRegisterLoading(boolean loading) {
        if (registerBtn == null) return;

        registerBtn.setEnabled(!loading);
        registerBtn.setText(loading ? "Creating Account..." : "Create Account");
        registerBtn.setCursor(loading
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}