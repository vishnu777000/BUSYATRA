package ui.settings;

import config.UIConfig;
import util.IconUtil;
import util.Refreshable;
import util.Session;

import javax.swing.*;
import java.awt.*;

public class ProfilePanel extends JPanel implements Refreshable {

    private JLabel nameValue;
    private JLabel roleValue;
    private JLabel idValue;
    private JLabel emailValue;

    public ProfilePanel() {

        setLayout(new BorderLayout(20,20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(25,30,25,30));

        add(header(),BorderLayout.NORTH);
        add(profileCard(),BorderLayout.CENTER);

        refreshData();
    }

    /* ================= HEADER ================= */

    private JPanel header(){

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("My Profile");
        title.setFont(new Font("Segoe UI",Font.BOLD,28));

        JLabel sub = new JLabel("Account information & role details");
        sub.setFont(UIConfig.FONT_SMALL);
        sub.setForeground(UIConfig.TEXT_LIGHT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));
        left.setOpaque(false);

        left.add(title);
        left.add(sub);

        panel.add(left,BorderLayout.WEST);

        return panel;
    }

    /* ================= PROFILE CARD ================= */

    private JPanel profileCard(){

        JPanel card = createCard();
        card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));

        /* ===== Avatar ===== */

        JLabel avatar = new JLabel(
                IconUtil.load("user.png",70,70)
        );

        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        nameValue = new JLabel("-",SwingConstants.CENTER);
        nameValue.setFont(new Font("Segoe UI",Font.BOLD,22));
        nameValue.setAlignmentX(Component.CENTER_ALIGNMENT);

        roleValue = new JLabel("-",SwingConstants.CENTER);
        roleValue.setFont(UIConfig.FONT_SMALL);
        roleValue.setForeground(UIConfig.TEXT_LIGHT);
        roleValue.setAlignmentX(Component.CENTER_ALIGNMENT);

        emailValue = new JLabel("-",SwingConstants.CENTER);
        emailValue.setFont(UIConfig.FONT_NORMAL);
        emailValue.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(avatar);
        card.add(Box.createVerticalStrut(10));
        card.add(nameValue);
        card.add(roleValue);
        card.add(emailValue);

        card.add(Box.createVerticalStrut(25));

        /* ===== Details ===== */

        JPanel details = new JPanel(new GridLayout(2,2,15,15));
        details.setOpaque(false);

        idValue = value();

        details.add(label("User ID"));
        details.add(idValue);

        details.add(label("Role"));
        details.add(new JLabel(Session.role));

        card.add(details);

        card.add(Box.createVerticalStrut(25));

        /* ===== Buttons ===== */

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER,15,0));
        buttons.setOpaque(false);

        JButton edit = new JButton("Edit Profile");
        UIConfig.primaryBtn(edit);

        JButton change = new JButton("Change Password");
        UIConfig.infoBtn(change);

        JButton logout = new JButton("Logout");
        UIConfig.dangerBtn(logout);

        logout.addActionListener(e -> {

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to logout?",
                    "Logout",
                    JOptionPane.YES_NO_OPTION
            );

            if(confirm == JOptionPane.YES_OPTION){

                Window window = SwingUtilities.getWindowAncestor(this);
                window.dispose();

                new ui.auth.LoginFrame();
            }
        });

        buttons.add(edit);
        buttons.add(change);
        buttons.add(logout);

        card.add(buttons);

        return card;
    }

    /* ================= LABEL HELPERS ================= */

    private JLabel label(String text){

        JLabel l = new JLabel(text);
        l.setFont(UIConfig.FONT_SMALL);
        l.setForeground(UIConfig.TEXT_LIGHT);

        return l;
    }

    private JLabel value(){

        JLabel l = new JLabel("-");
        l.setFont(UIConfig.FONT_NORMAL);
        l.setForeground(UIConfig.TEXT);

        return l;
    }

    /* ================= CARD STYLE ================= */

    private JPanel createCard(){

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,220)),
                BorderFactory.createEmptyBorder(30,40,30,40)
        ));

        return card;
    }

    /* ================= REFRESH ================= */

    @Override
    public void refreshData(){

        nameValue.setText(Session.username);
        roleValue.setText(Session.role);
        emailValue.setText(Session.userEmail);
        idValue.setText(String.valueOf(Session.userId));
    }
}