package ui.admin;

import config.UIConfig;
import dao.UserDAO;
import util.Refreshable;
import util.DBConnectionUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class ManageUsersPanel extends JPanel implements Refreshable {

    private JTable table;
    private DefaultTableModel model;
    private JLabel loadStatusLabel;
    private JTextField searchField;
    private JLabel pageInfoLabel;
    private JButton prevBtn;
    private JButton nextBtn;

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passField;
    private JComboBox<String> roleBox;
    private JButton addBtn;
    private JButton updateBtn;
    private JButton blockBtn;
    private JButton unblockBtn;

    private int selectedUserId = -1;
    private List<String[]> allUsers = new ArrayList<>();
    private List<String[]> filteredUsers = new ArrayList<>();
    private int currentPage = 1;
    private static final int PAGE_SIZE = 25;
    private final UserDAO userDAO = new UserDAO();

    public ManageUsersPanel() {

        setLayout(new BorderLayout(20,20));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        add(header(),BorderLayout.NORTH);
        add(tableCard(),BorderLayout.CENTER);
        add(formCard(),BorderLayout.EAST);

        loadUsers();
    }

    private JPanel header(){

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("User Management");
        title.setFont(UIConfig.FONT_TITLE);

        JLabel sub = new JLabel("Manage users and permissions");
        sub.setForeground(UIConfig.TEXT_LIGHT);

        JPanel left = new JPanel(new GridLayout(2,1));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        searchField = new JTextField();
        UIConfig.styleField(searchField);
        searchField.setPreferredSize(new Dimension(260, 34));
        searchField.setToolTipText("Search by name, email, role or status");
        searchField.addActionListener(e -> applyFilter());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        pageInfoLabel = new JLabel("Page 1/1");
        pageInfoLabel.setForeground(UIConfig.TEXT_LIGHT);

        prevBtn = btn("Prev");
        nextBtn = btn("Next");
        UIConfig.secondaryBtn(prevBtn);
        UIConfig.secondaryBtn(nextBtn);
        prevBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                refreshTablePage();
            }
        });
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPages()) {
                currentPage++;
                refreshTablePage();
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(new JLabel("Search:"));
        right.add(searchField);
        right.add(prevBtn);
        right.add(nextBtn);
        right.add(pageInfoLabel);

        panel.add(left,BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JPanel tableCard(){

        model = new DefaultTableModel(
                new Object[]{"ID","Name","Email","Role","Status"},0){
            public boolean isCellEditable(int r,int c){ return false; }
        };

        table = new JTable(model);
        UIConfig.styleTable(table);
        table.setRowHeight(32);

        table.getSelectionModel().addListSelectionListener(e -> fillForm());

        JScrollPane sp = new JScrollPane(table);
        UIConfig.styleScroll(sp);
        sp.setColumnHeaderView(table.getTableHeader());

        loadStatusLabel = new JLabel(" ");
        loadStatusLabel.setFont(UIConfig.FONT_SMALL);
        loadStatusLabel.setForeground(UIConfig.TEXT_LIGHT);

        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);
        card.add(sp,BorderLayout.CENTER);
        card.add(loadStatusLabel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel formCard(){

        JPanel card = new JPanel(new BorderLayout(12,12));
        UIConfig.styleCard(card);
        card.setPreferredSize(new Dimension(360,0));

        JLabel h = new JLabel("User Details");
        h.setFont(UIConfig.FONT_SUBTITLE);

        nameField = field("Name");
        emailField = field("Email");

        passField = new JPasswordField();
        UIConfig.styleField(passField);
        passField.setBorder(BorderFactory.createTitledBorder("Password"));

        roleBox = new JComboBox<>(new String[]{"USER","ADMIN","MANAGER","CLERK"});
        UIConfig.styleCombo(roleBox);

        JPanel fields = new JPanel(new GridLayout(4,1,10,10));
        fields.setOpaque(false);
        fields.add(nameField);
        fields.add(emailField);
        fields.add(passField);
        fields.add(roleBox);

        addBtn = btn("Add");
        updateBtn = btn("Update");
        blockBtn = btn("Block");
        unblockBtn = btn("Unblock");

        UIConfig.primaryBtn(addBtn);
        UIConfig.successBtn(updateBtn);
        UIConfig.dangerBtn(blockBtn);
        UIConfig.infoBtn(unblockBtn);

        addBtn.addActionListener(e -> addUser());
        updateBtn.addActionListener(e -> updateUser());
        blockBtn.addActionListener(e -> setStatus("BLOCKED"));
        unblockBtn.addActionListener(e -> setStatus("ACTIVE"));

        JPanel btns = new JPanel(new GridLayout(4,1,10,10));
        btns.setOpaque(false);
        btns.add(addBtn);
        btns.add(updateBtn);
        btns.add(blockBtn);
        btns.add(unblockBtn);

        card.add(h,BorderLayout.NORTH);
        card.add(fields,BorderLayout.CENTER);
        card.add(btns,BorderLayout.SOUTH);

        return card;
    }

    private JTextField field(String title){
        JTextField f = new JTextField();
        UIConfig.styleField(f);
        f.setBorder(BorderFactory.createTitledBorder(title));
        return f;
    }

    private JButton btn(String t){
        JButton b = new JButton(t);
        b.setPreferredSize(new Dimension(140,36));
        return b;
    }

    private void loadUsers(){

        model.setRowCount(0);
        allUsers = new ArrayList<>();
        filteredUsers = new ArrayList<>();
        setLoadStatus("Loading users...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() {
                try (Connection ignored = DBConnectionUtil.getConnection()) {
                    
                } catch (Exception e) {
                    throw new RuntimeException(DBConnectionUtil.userMessage(e), e);
                }
                return userDAO.getAllUsers();
            }

            @Override
            protected void done() {
                try {
                    List<String[]> users = get();
                    String daoError = userDAO.getLastError();
                    if ((users == null || users.isEmpty())
                            && daoError != null
                            && !daoError.isBlank()
                            && !"Unknown registration/login error".equals(daoError)) {
                        setLoadStatus(daoError);
                        refreshTablePage();
                        return;
                    }
                    allUsers = users == null ? new ArrayList<>() : new ArrayList<>(users);
                    applyFilter();
                    setLoadStatus(allUsers.isEmpty()
                            ? "No users found"
                            : "Loaded " + allUsers.size() + " users");
                } catch (Exception e) {
                    String message = DBConnectionUtil.userMessage(e);
                    setLoadStatus(message);
                    JOptionPane.showMessageDialog(ManageUsersPanel.this, message, "Manage Users", JOptionPane.ERROR_MESSAGE);
                }
                setActionBusy(false, loadStatusLabel == null ? " " : loadStatusLabel.getText());
            }
        };

        worker.execute();
    }

    private void applyFilter() {
        String q = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        filteredUsers = new ArrayList<>();
        for (String[] user : allUsers) {
            if (user == null || user.length < 5) continue;
            if (q.isBlank()
                    || user[1].toLowerCase().contains(q)
                    || user[2].toLowerCase().contains(q)
                    || user[3].toLowerCase().contains(q)
                    || user[4].toLowerCase().contains(q)
                    || user[0].toLowerCase().contains(q)) {
                filteredUsers.add(user);
            }
        }
        currentPage = 1;
        refreshTablePage();
    }

    private int totalPages() {
        int size = filteredUsers == null ? 0 : filteredUsers.size();
        int pages = (int) Math.ceil(size / (double) PAGE_SIZE);
        return Math.max(1, pages);
    }

    private void refreshTablePage() {
        model.setRowCount(0);
        int pages = totalPages();
        if (currentPage > pages) currentPage = pages;
        if (currentPage < 1) currentPage = 1;

        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredUsers.size());
        for (int i = start; i < end; i++) {
            String[] user = filteredUsers.get(i);
            model.addRow(new Object[]{
                    Integer.parseInt(user[0]),
                    user[1],
                    user[2],
                    user[3],
                    user[4]
            });
        }

        pageInfoLabel.setText("Page " + currentPage + "/" + pages + "  (" + filteredUsers.size() + " users)");
        prevBtn.setEnabled(currentPage > 1);
        nextBtn.setEnabled(currentPage < pages);
    }

    private void setLoadStatus(String message) {
        if (loadStatusLabel != null) {
            loadStatusLabel.setText(message == null || message.isBlank() ? " " : message);
        }
    }

    private void setActionBusy(boolean busy, String message) {
        setLoadStatus(message);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        if (searchField != null) searchField.setEnabled(!busy);
        if (table != null) table.setEnabled(!busy);
        if (prevBtn != null) prevBtn.setEnabled(!busy && currentPage > 1);
        if (nextBtn != null) nextBtn.setEnabled(!busy && currentPage < totalPages());
        if (addBtn != null) addBtn.setEnabled(!busy);
        if (updateBtn != null) updateBtn.setEnabled(!busy);
        if (blockBtn != null) blockBtn.setEnabled(!busy);
        if (unblockBtn != null) unblockBtn.setEnabled(!busy);
    }

    private void runUserMutation(String busyMessage, String successMessage, UserMutation mutation, boolean clearForm) {
        setActionBusy(true, busyMessage);

        SwingWorker<UserMutationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected UserMutationResult doInBackground() {
                UserDAO actionDao = new UserDAO();
                boolean ok = mutation.run(actionDao);
                return new UserMutationResult(ok, actionDao.getLastError());
            }

            @Override
            protected void done() {
                try {
                    UserMutationResult result = get();
                    if (!result.success) {
                        setActionBusy(false, result.message);
                        JOptionPane.showMessageDialog(ManageUsersPanel.this, result.message);
                        return;
                    }

                    if (clearForm) {
                        clear();
                    }
                    loadUsers();
                    JOptionPane.showMessageDialog(ManageUsersPanel.this, successMessage);
                } catch (Exception e) {
                    setActionBusy(false, "Action failed");
                    JOptionPane.showMessageDialog(ManageUsersPanel.this, "Action failed");
                }
            }
        };

        worker.execute();
    }

    private void fillForm(){

        int row = table.getSelectedRow();
        if(row==-1) return;

        selectedUserId = Integer.parseInt(model.getValueAt(row,0).toString());

        nameField.setText(model.getValueAt(row,1).toString());
        emailField.setText(model.getValueAt(row,2).toString());
        roleBox.setSelectedItem(model.getValueAt(row,3).toString());
    }

    private void addUser(){

        try{
            String name = nameField.getText();
            String email = emailField.getText();
            String pass = new String(passField.getPassword());
            String role = roleBox.getSelectedItem().toString();

            runUserMutation(
                    "Adding user...",
                    "User added",
                    dao -> dao.addUser(name, email, pass, role),
                    true
            );
        }catch(Exception e){
            JOptionPane.showMessageDialog(this,"Error");
        }
    }

    private void updateUser(){

        if(selectedUserId == -1){
            JOptionPane.showMessageDialog(this,"Select user");
            return;
        }

        try{
            String role = roleBox.getSelectedItem().toString();
            runUserMutation(
                    "Updating user role...",
                    "Role updated",
                    dao -> dao.updateUserRole(selectedUserId, role),
                    false
            );
        }catch(Exception e){
            JOptionPane.showMessageDialog(this,"Error");
        }
    }

    private void setStatus(String status){

        if(selectedUserId == -1){
            JOptionPane.showMessageDialog(this,"Select user");
            return;
        }

        runUserMutation(
                "Updating user status...",
                "User updated",
                dao -> dao.updateUserStatus(selectedUserId, status),
                false
        );
    }

    private void clear(){

        selectedUserId = -1;
        nameField.setText("");
        emailField.setText("");
        passField.setText("");
        roleBox.setSelectedIndex(0);
    }

    @Override
    public void refreshData(){
        clear();
        loadUsers();
    }

    @Override
    public boolean refreshOnFirstShow() {
        return false;
    }

    @FunctionalInterface
    private interface UserMutation {
        boolean run(UserDAO dao);
    }

    private static class UserMutationResult {
        private final boolean success;
        private final String message;

        private UserMutationResult(boolean success, String message) {
            this.success = success;
            this.message = message == null || message.isBlank() || "Unknown registration/login error".equals(message)
                    ? "Action failed"
                    : message;
        }
    }
}
