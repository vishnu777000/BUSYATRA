package ui.auth;

import config.UIConfig;
import dao.UserDAO;
import ui.common.MainFrame;
import util.IconUtil;
import util.Session;
import util.SessionManager;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passField;
    private JCheckBox showPass;
    private JButton loginBtn;
    private JLabel statusLbl;
    private volatile boolean loginInProgress = false;
    private final UserDAO userDAO = new UserDAO();

    public LoginFrame(){

        setTitle("BusYatra - Login");
        setIconImage(IconUtil.load("buslogo.png", 32, 32).getImage());
        setSize(900,580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIConfig.BACKGROUND);

        add(topBanner(),BorderLayout.NORTH);
        add(centerCard(),BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> emailField.requestFocus());
        getRootPane().setDefaultButton(loginBtn);

        setVisible(true);
        warmupLoginPath();
    }

    private JPanel topBanner(){

        JPanel banner = new JPanel(){
            protected void paintComponent(Graphics g){

                Graphics2D g2=(Graphics2D)g;

                GradientPaint gp=new GradientPaint(
                        0,0,UIConfig.PRIMARY,
                        getWidth(),getHeight(),UIConfig.PRIMARY.darker()
                );

                g2.setPaint(gp);
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        };

        banner.setPreferredSize(new Dimension(900,160));
        banner.setLayout(new BoxLayout(banner,BoxLayout.Y_AXIS));

        JLabel logo = new JLabel(IconUtil.load("buslogo.png",70,70));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("BusYatra");
        title.setFont(new Font("Segoe UI",Font.BOLD,34));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Book - Track - Travel Smart");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(Color.WHITE);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        banner.add(Box.createVerticalStrut(15));
        banner.add(logo);
        banner.add(Box.createVerticalStrut(8));
        banner.add(title);
        banner.add(sub);

        return banner;
    }

    private JPanel centerCard(){

        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(UIConfig.BACKGROUND);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(380,400));

        UIConfig.styleCard(card);

        card.add(cardHeader());
        card.add(Box.createVerticalStrut(20));
        card.add(cardForm());
        card.add(Box.createVerticalStrut(15));
        card.add(cardFooter());

        bg.add(card);

        return bg;
    }

    private JPanel cardHeader(){

        JPanel p=new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));

        JLabel h=new JLabel("Login");
        h.setFont(UIConfig.FONT_TITLE);
        h.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub=new JLabel("Welcome back, please login");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(UIConfig.TEXT_LIGHT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(h);
        p.add(Box.createVerticalStrut(5));
        p.add(sub);

        return p;
    }

    private JPanel cardForm(){

        JPanel form=new JPanel();
        form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
        form.setOpaque(false);

        emailField = field("Email Address");
        passField = passwordField("Password");

        passField.addActionListener(e -> doLogin());

        showPass = new JCheckBox("Show password");
        showPass.setOpaque(false);

        showPass.addActionListener(e -> {
            passField.setEchoChar(showPass.isSelected() ? (char)0 : '\u2022');
        });

        loginBtn = btn("Login",true);
        loginBtn.addActionListener(e -> doLogin());

        form.add(emailField);
        form.add(Box.createVerticalStrut(12));
        form.add(passField);
        form.add(Box.createVerticalStrut(6));
        form.add(showPass);
        form.add(Box.createVerticalStrut(15));
        form.add(loginBtn);
        form.add(Box.createVerticalStrut(8));

        statusLbl = new JLabel(" ");
        statusLbl.setForeground(UIConfig.TEXT_LIGHT);
        form.add(statusLbl);

        return form;
    }

    private JPanel cardFooter(){

        JPanel footer=new JPanel();
        footer.setLayout(new BoxLayout(footer,BoxLayout.Y_AXIS));
        footer.setOpaque(false);

        JButton forgotBtn=new JButton("Forgot Password?");
        forgotBtn.setContentAreaFilled(false);
        forgotBtn.setBorderPainted(false);
        forgotBtn.setForeground(UIConfig.PRIMARY);
        forgotBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        forgotBtn.addActionListener(e -> {
            dispose();
            new ForgotPasswordFrame();
        });

        JButton registerBtn = btn("Create New Account",false);
        UIConfig.successBtn(registerBtn);

        registerBtn.addActionListener(e -> {
            dispose();
            new RegisterFrame();
        });

        footer.add(forgotBtn);
        footer.add(Box.createVerticalStrut(10));
        footer.add(registerBtn);

        return footer;
    }

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

    private JButton btn(String text, boolean primary){
        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);

        if(primary) UIConfig.primaryBtn(b);

        return b;
    }

    private void doLogin(){
        if (loginInProgress) return;

        String email=emailField.getText().trim();
        String pass=new String(passField.getPassword()).trim();

        if(email.isEmpty()||pass.isEmpty()){
            JOptionPane.showMessageDialog(this,"Enter email & password");
            return;
        }

        loginInProgress = true;
        setLoginLoading(true);
        statusLbl.setText("Signing in...");

        SwingWorker<String[], Void> worker = new SwingWorker<>() {
            private String errorMessage = "Invalid credentials";

            @Override
            protected String[] doInBackground() {
                String[] user = userDAO.loginUser(email, pass);
                if (user == null) {
                    errorMessage = userDAO.getLastError();
                }
                return user;
            }

            @Override
            protected void done() {
                loginInProgress = false;
                setLoginLoading(false);
                try {
                    String[] user = get();
                    if(user != null){
                        Session.userId=Integer.parseInt(user[0]);
                        Session.username=user[1];
                        Session.userEmail=user[2];
                        Session.role=user[3].trim().toUpperCase();

                        SessionManager.saveLogin(Session.userId,Session.role);
                        SessionManager.saveUserMeta(Session.username, Session.userEmail);
                        statusLbl.setText("Opening dashboard...");
                        Timer openTimer = new Timer(80, evt -> {
                            dispose();
                            new MainFrame(Session.username,Session.role);
                        });
                        openTimer.setRepeats(false);
                        openTimer.start();

                    }else{
                        statusLbl.setText("Sign in failed");
                        JOptionPane.showMessageDialog(
                                LoginFrame.this,
                                "Login failed.\nReason: " + errorMessage,
                                "Login Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }

                }catch(Exception e){
                    statusLbl.setText("Sign in failed");
                    JOptionPane.showMessageDialog(
                            LoginFrame.this,
                            "Database error.\nReason: " + e.getMessage(),
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    private void warmupLoginPath() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                userDAO.warmLoginMetadata();
                return null;
            }
        };
        worker.execute();
    }

    private void setLoginLoading(boolean loading) {
        if (loginBtn == null) return;

        loginBtn.setEnabled(!loading);
        loginBtn.setText(loading ? "Signing In..." : "Login");
        loginBtn.setCursor(loading
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        emailField.setEnabled(!loading);
        passField.setEnabled(!loading);
        showPass.setEnabled(!loading);
    }
}
