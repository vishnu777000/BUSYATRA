package ui.auth;

import config.UIConfig;
import dao.UserDAO;
import ui.common.MainFrame;
import util.IconUtil;
import util.Session;
import util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;

public class LoginFrame extends JFrame {

private JTextField emailField;
private JPasswordField passField;
private JCheckBox showPass;

public LoginFrame(){

    setTitle("BusYatra - Login");
    setSize(900,580);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLayout(new BorderLayout());
    getContentPane().setBackground(UIConfig.BACKGROUND);

    add(topBanner(),BorderLayout.NORTH);
    add(centerCard(),BorderLayout.CENTER);

    SwingUtilities.invokeLater(() -> emailField.requestFocus());

    setVisible(true);
}

/* ================= TOP BANNER ================= */

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

    JLabel logo = new JLabel(IconUtil.load("bus.png",70,70));
    logo.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel title = new JLabel("BusYatra");
    title.setFont(new Font("Segoe UI",Font.BOLD,34));
    title.setForeground(Color.WHITE);
    title.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel sub = new JLabel("Book • Track • Travel Smart");
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

/* ================= CENTER CARD ================= */

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

/* ================= HEADER ================= */

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

/* ================= FORM ================= */

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
        passField.setEchoChar(showPass.isSelected() ? (char)0 : '•');
    });

    JButton loginBtn = btn("Login",true);
    loginBtn.addActionListener(e -> doLogin());

    form.add(emailField);
    form.add(Box.createVerticalStrut(12));
    form.add(passField);
    form.add(Box.createVerticalStrut(6));
    form.add(showPass);
    form.add(Box.createVerticalStrut(15));
    form.add(loginBtn);

    return form;
}

/* ================= FOOTER ================= */

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

/* ================= HELPERS ================= */

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

/* ================= LOGIN ================= */

private void doLogin(){

    try{

        String email=emailField.getText().trim();
        String pass=new String(passField.getPassword()).trim();

        if(email.isEmpty()||pass.isEmpty()){
            JOptionPane.showMessageDialog(this,"Enter email & password");
            return;
        }

        ResultSet rs=new UserDAO().loginUser(email,pass);

        if(rs!=null && rs.next()){

            Session.userId=rs.getInt("id");
            Session.username=rs.getString("name");
            Session.role=rs.getString("role").trim().toUpperCase();
            Session.userEmail=rs.getString("email");

            SessionManager.saveLogin(Session.userId,Session.role);

            dispose();
            new MainFrame(Session.username,Session.role);

        }else{
            JOptionPane.showMessageDialog(
                    this,
                    "Invalid credentials ❌",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }

    }catch(Exception e){

        e.printStackTrace();

        JOptionPane.showMessageDialog(this,"Database error ❌");
    }
}

}
