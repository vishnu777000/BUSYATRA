package ui.admin;

import config.UIConfig;
import dao.BannerDAO;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

public class AdminBannerPanel extends JPanel implements Refreshable {

    private JTable table;
    private DefaultTableModel model;
    private JLabel previewLabel;

    public AdminBannerPanel() {

        setLayout(new BorderLayout(20,20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        add(header(), BorderLayout.NORTH);
        add(centerPanel(), BorderLayout.CENTER);
        add(actionPanel(), BorderLayout.SOUTH);

        loadBanners();
    }

    /* ================= HEADER ================= */

    private JPanel header(){

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        left.setOpaque(false);

        JLabel icon = new JLabel(
                new ImageIcon(getClass().getResource("/resources/icons/extra/report.png"))
        );

        JLabel title = new JLabel("Banner Management");
        title.setFont(UIConfig.FONT_TITLE);

        left.add(icon);
        left.add(title);

        panel.add(left,BorderLayout.WEST);
        return panel;
    }

    /* ================= CENTER ================= */

    private JPanel centerPanel(){

        JPanel panel = new JPanel(new GridLayout(1,2,20,20));
        panel.setOpaque(false);

        panel.add(tableSection());
        panel.add(previewSection());

        return panel;
    }

    /* ================= TABLE ================= */

    private JPanel tableSection(){

        model = new DefaultTableModel(
                new Object[]{"ID","Title","Image Path","Active"},0){

            public boolean isCellEditable(int r,int c){
                return false;
            }
        };

        table = new JTable(model);
        UIConfig.styleTable(table);

        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getSelectionModel().addListSelectionListener(e -> showPreview());

        JScrollPane sp = new JScrollPane(table);

        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);
        card.add(sp,BorderLayout.CENTER);

        return card;
    }

    /* ================= PREVIEW ================= */

    private JPanel previewSection(){

        JPanel panel = new JPanel(new BorderLayout());
        UIConfig.styleCard(panel);

        JLabel title = new JLabel("Banner Preview",SwingConstants.CENTER);
        title.setFont(UIConfig.FONT_SUBTITLE);

        previewLabel = new JLabel("",SwingConstants.CENTER);
        previewLabel.setForeground(UIConfig.TEXT_LIGHT);

        setDefaultPreview();

        panel.add(title,BorderLayout.NORTH);
        panel.add(previewLabel,BorderLayout.CENTER);

        return panel;
    }

    private void setDefaultPreview(){
        previewLabel.setText("Select a banner to preview");
        previewLabel.setIcon(null);
    }

    private void showPreview(){

        int row = table.getSelectedRow();
        if(row == -1){
            setDefaultPreview();
            return;
        }

        String path = model.getValueAt(row,2).toString();

        try{
            ImageIcon icon = new ImageIcon(path);

            Image img = icon.getImage().getScaledInstance(
                    420,250,Image.SCALE_SMOOTH);

            previewLabel.setIcon(new ImageIcon(img));
            previewLabel.setText("");

        }catch(Exception e){
            setDefaultPreview();
        }
    }

    /* ================= ACTION BUTTONS ================= */

    private JPanel actionPanel(){

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,0));
        panel.setOpaque(false);

        JButton uploadBtn = createBtn("Upload",
                "/resources/icons/actions/upload.png");

        JButton activeBtn = createBtn("Set Active",
                "/resources/icons/actions/refresh.png");

        JButton deleteBtn = createBtn("Delete",
                "/resources/icons/extra/logout.png");

        UIConfig.primaryBtn(uploadBtn);
        UIConfig.successBtn(activeBtn);
        UIConfig.dangerBtn(deleteBtn);

        uploadBtn.addActionListener(e -> uploadBanner());
        activeBtn.addActionListener(e -> setActive());
        deleteBtn.addActionListener(e -> deleteBanner());

        panel.add(uploadBtn);
        panel.add(activeBtn);
        panel.add(deleteBtn);

        return panel;
    }

    private JButton createBtn(String text, String iconPath){

        JButton b = new JButton(text,
                new ImageIcon(getClass().getResource(iconPath)));

        b.setPreferredSize(new Dimension(150,36));
        return b;
    }

    /* ================= LOAD DATA ================= */

    private void loadBanners(){

        SwingUtilities.invokeLater(() -> {

            model.setRowCount(0);

            try{
                List<String[]> list = new BannerDAO().getAllBanners();

                for(String[] row : list){
                    model.addRow(row);
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        });
    }

    /* ================= ACTIONS ================= */

    private void uploadBanner(){

        JFileChooser chooser = new JFileChooser();

        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Images", "jpg","png","jpeg"));

        int res = chooser.showOpenDialog(this);
        if(res != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        String path = file.getAbsolutePath().replace("\\","/");

        String title = JOptionPane.showInputDialog(this,"Enter Banner Title:");

        if(title == null || title.trim().isEmpty()){
            JOptionPane.showMessageDialog(this,"Title required");
            return;
        }

        boolean ok = new BannerDAO().addBanner(title, path);

        JOptionPane.showMessageDialog(this,
                ok ? "Uploaded successfully ✅" : "Upload failed ❌");

        if(ok) loadBanners();
    }

    private void setActive(){

        int row = table.getSelectedRow();
        if(row == -1){
            JOptionPane.showMessageDialog(this,"Select a banner");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row,0).toString());

        boolean ok = new BannerDAO().setActiveBanner(id);

        JOptionPane.showMessageDialog(this,
                ok ? "Activated ✅" : "Failed ❌");

        if(ok) loadBanners();
    }

    private void deleteBanner(){

        int row = table.getSelectedRow();
        if(row == -1){
            JOptionPane.showMessageDialog(this,"Select a banner");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,"Delete banner?","Confirm",
                JOptionPane.YES_NO_OPTION);

        if(confirm != JOptionPane.YES_OPTION) return;

        int id = Integer.parseInt(model.getValueAt(row,0).toString());

        boolean ok = new BannerDAO().deleteBanner(id);

        JOptionPane.showMessageDialog(this,
                ok ? "Deleted ✅" : "Failed ❌");

        if(ok) loadBanners();
    }

    @Override
    public void refreshData(){
        loadBanners();
    }
}