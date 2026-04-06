package ui.admin;

import config.UIConfig;
import dao.BannerDAO;
import util.IconUtil;
import util.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class AdminBannerPanel extends JPanel implements Refreshable {

    private final BannerDAO bannerDAO = new BannerDAO();
    private JTable table;
    private DefaultTableModel model;
    private JLabel previewLabel;
    private JLabel statusLabel;
    private JButton uploadBtn;
    private JButton activeBtn;
    private JButton deleteBtn;
    private volatile boolean busy = false;

    public AdminBannerPanel() {

        setLayout(new BorderLayout(20,20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        add(header(), BorderLayout.NORTH);
        add(centerPanel(), BorderLayout.CENTER);
        add(actionPanel(), BorderLayout.SOUTH);

        loadBanners();
    }

    

    private JPanel header(){

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        left.setOpaque(false);

        JLabel icon = new JLabel(
                IconUtil.load("report.png", 20, 20)
        );

        JLabel title = new JLabel("Banner Management");
        title.setFont(UIConfig.FONT_TITLE);

        left.add(icon);
        left.add(title);

        panel.add(left,BorderLayout.WEST);
        return panel;
    }

    

    private JPanel centerPanel(){

        JPanel panel = new JPanel(new GridLayout(1,2,20,20));
        panel.setOpaque(false);

        panel.add(tableSection());
        panel.add(previewSection());

        return panel;
    }

    

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
        UIConfig.styleScroll(sp);
        sp.setColumnHeaderView(table.getTableHeader());

        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);
        card.add(sp,BorderLayout.CENTER);

        return card;
    }

    

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
            ImageIcon icon = resolvePreviewIcon(path);
            if (icon == null || icon.getIconWidth() <= 0) {
                setDefaultPreview();
                return;
            }

            Image img = icon.getImage().getScaledInstance(
                    420,250,Image.SCALE_SMOOTH);

            previewLabel.setIcon(new ImageIcon(img));
            previewLabel.setText("");

        }catch(Exception e){
            setDefaultPreview();
        }
    }

    private ImageIcon resolvePreviewIcon(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return null;
        String path = rawPath.trim().replace("\\", "/");

        if (path.startsWith("/")) {
            URL url = getClass().getResource(path);
            if (url != null) return new ImageIcon(url);
        }

        if (path.startsWith("resources/")) {
            URL url = getClass().getResource("/" + path);
            if (url != null) return new ImageIcon(url);
        }

        URL fromBanners = getClass().getResource("/resources/banners/" + new File(path).getName());
        if (fromBanners != null) return new ImageIcon(fromBanners);

        File file = new File(path);
        if (file.exists()) return new ImageIcon(file.getAbsolutePath());
        return null;
    }

    

    private JPanel actionPanel(){

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,0));
        actions.setOpaque(false);

        uploadBtn = createBtn("Upload", "download.png");

        activeBtn = createBtn("Set Active", "refresh.png");

        deleteBtn = createBtn("Delete", "logout.png");

        UIConfig.primaryBtn(uploadBtn);
        UIConfig.successBtn(activeBtn);
        UIConfig.dangerBtn(deleteBtn);

        uploadBtn.addActionListener(e -> uploadBanner());
        activeBtn.addActionListener(e -> setActive());
        deleteBtn.addActionListener(e -> deleteBanner());

        actions.add(uploadBtn);
        actions.add(activeBtn);
        actions.add(deleteBtn);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UIConfig.FONT_SMALL);
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(actions, BorderLayout.EAST);

        return panel;
    }

    private JButton createBtn(String text, String iconName){
        JButton b = new JButton(text, IconUtil.load(iconName, 16, 16));

        b.setPreferredSize(new Dimension(150,36));
        return b;
    }

    

    private void loadBanners(){
        loadBanners(false);
    }

    private void loadBanners(boolean force){
        if (busy && !force) return;
        setBusy(true, "Loading banners...");
        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                return bannerDAO.getAllBanners();
            }

            @Override
            protected void done() {
                model.setRowCount(0);
                try{
                    List<String[]> list = get();
                    for(String[] row : list){
                        model.addRow(row);
                    }
                    setBusy(false, "Loaded " + model.getRowCount() + " banners");
                }catch(Exception e){
                    setBusy(false, bannerDAO.getLastError());
                }
            }
        };
        worker.execute();
    }

    

    private void uploadBanner(){

        JFileChooser chooser = new JFileChooser();

        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Images", "jpg","png","jpeg"));

        int res = chooser.showOpenDialog(this);
        if(res != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        String title = JOptionPane.showInputDialog(this,"Enter Banner Title:");

        if(title == null || title.trim().isEmpty()){
            JOptionPane.showMessageDialog(this,"Title required");
            return;
        }

        final String path;
        try {
            path = persistBannerImage(file);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Banner image could not be stored.\nReason: " + ex.getMessage(),
                    "Upload Failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        setBusy(true, "Uploading banner...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return bannerDAO.addBanner(title, path);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = Boolean.TRUE.equals(get());
                    JOptionPane.showMessageDialog(
                            AdminBannerPanel.this,
                            ok ? "Uploaded successfully" : bannerDAO.getLastError(),
                            ok ? "Banner Uploaded" : "Upload Failed",
                            ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
                    );
                    if(ok) loadBanners(true);
                    else setBusy(false, bannerDAO.getLastError());
                } catch (Exception ex) {
                    setBusy(false, bannerDAO.getLastError());
                }
            }
        };
        worker.execute();
    }

    private void setActive(){

        int row = table.getSelectedRow();
        if(row == -1){
            JOptionPane.showMessageDialog(this,"Select a banner");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row,0).toString());

        setBusy(true, "Updating active banner...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return bannerDAO.setActiveBanner(id);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = Boolean.TRUE.equals(get());
                    JOptionPane.showMessageDialog(
                            AdminBannerPanel.this,
                            ok ? "Activated" : bannerDAO.getLastError(),
                            ok ? "Banner Updated" : "Update Failed",
                            ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
                    );
                    if(ok) loadBanners(true);
                    else setBusy(false, bannerDAO.getLastError());
                } catch (Exception ex) {
                    setBusy(false, bannerDAO.getLastError());
                }
            }
        };
        worker.execute();
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

        setBusy(true, "Deleting banner...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return bannerDAO.deleteBanner(id);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = Boolean.TRUE.equals(get());
                    JOptionPane.showMessageDialog(
                            AdminBannerPanel.this,
                            ok ? "Deleted" : bannerDAO.getLastError(),
                            ok ? "Banner Deleted" : "Delete Failed",
                            ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
                    );
                    if(ok) loadBanners(true);
                    else setBusy(false, bannerDAO.getLastError());
                } catch (Exception ex) {
                    setBusy(false, bannerDAO.getLastError());
                }
            }
        };
        worker.execute();
    }

    private String persistBannerImage(File file) throws Exception {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("Select a valid image file.");
        }

        String fileName = file.getName();
        int dot = fileName.lastIndexOf('.');
        String ext = dot >= 0 ? fileName.substring(dot).toLowerCase() : ".png";
        String base = dot >= 0 ? fileName.substring(0, dot) : fileName;
        base = base.replaceAll("[^A-Za-z0-9_-]+", "-").replaceAll("-{2,}", "-");
        base = base.replaceAll("^-+", "").replaceAll("-+$", "");
        if (base.isBlank()) {
            base = "banner";
        }

        Path dir = Path.of("src", "resources", "banners");
        Files.createDirectories(dir);

        Path target = dir.resolve(base + "-" + System.currentTimeMillis() + ext);
        Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return "resources/banners/" + target.getFileName().toString();
    }

    @Override
    public void refreshData(){
        loadBanners();
    }

    @Override
    public boolean refreshOnFirstShow() {
        return false;
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        if (statusLabel != null) statusLabel.setText(message == null ? " " : message);
        if (table != null) table.setEnabled(!value);
        if (uploadBtn != null) uploadBtn.setEnabled(!value);
        if (activeBtn != null) activeBtn.setEnabled(!value);
        if (deleteBtn != null) deleteBtn.setEnabled(!value);
        setCursor(Cursor.getPredefinedCursor(value ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }
}

