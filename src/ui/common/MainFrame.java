package ui.common;

import config.UIConfig;

import ui.accounts.AccountsDashboard;
import ui.admin.*;
import ui.auth.LoginFrame;
import ui.booking.*;
import ui.clerk.BookingClerkDashboard;
import ui.complaints.ComplaintPanel;
import ui.dashboard.UserDashboard;
import ui.manager.ManagerDashboard;
import ui.settings.SettingsPanel;
import ui.tickets.*;
import ui.wallet.WalletPanel;

import util.PreferencesUtil;
import util.Refreshable;
import util.Session;
import util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Stack;

public class MainFrame extends JFrame {

    /* ================= SCREEN CONSTANTS ================= */

    public static final String SCREEN_USER = "USER_DASHBOARD";

    public static final String SCREEN_SEARCH = "SEARCH_BUSES";
    public static final String SCREEN_BUS_LIST = "BUS_LIST";
    public static final String SCREEN_BUS_DETAILS = "BUS_DETAILS";
    public static final String SCREEN_SEATS = "SEAT_SELECTION";
    public static final String SCREEN_PASSENGER = "PASSENGER_DETAILS";
    public static final String SCREEN_PAYMENT = "PAYMENT";
    public static final String SCREEN_SUMMARY = "BOOKING_SUMMARY";

    public static final String SCREEN_TICKET_PREVIEW = "TICKET_PREVIEW";
    public static final String SCREEN_MY_TICKETS = "MY_TICKETS";
    public static final String SCREEN_CANCEL = "CANCEL";

    public static final String SCREEN_WALLET = "WALLET";
    public static final String SCREEN_COMPLAINT = "COMPLAINT";
    public static final String SCREEN_SETTINGS = "SETTINGS";

    public static final String SCREEN_ADMIN = "ADMIN";
    public static final String SCREEN_MANAGER = "MANAGER";
    public static final String SCREEN_CLERK = "CLERK";

    /* ================= CORE ================= */

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private final HashMap<String, JPanel> screenCache = new HashMap<>();
    private final Stack<String> navigationHistory = new Stack<>();

    private String currentScreen;

    private final String username;
    private final String role;

    private HeaderPanel headerPanel;

    /* ================= CONSTRUCTOR ================= */

    public MainFrame(String username, String role) {

        this.username = username;
        this.role = role;

        initFrame();
        initLayout();
        preloadScreens();
        initDefaultScreen();

        setVisible(true);
    }

    /* ================= FRAME ================= */

    private void initFrame() {

        setTitle("BusYatra");
        setMinimumSize(new Dimension(1200, 720));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    /* ================= LAYOUT ================= */

   private void initLayout() {

    // HEADER
    headerPanel = new HeaderPanel(this::logout);
    headerPanel.setBackground(Color.WHITE);
    headerPanel.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(230,230,230)));
    add(headerPanel, BorderLayout.NORTH);

    // SIDEBAR (MODERN)
    if (!"CLERK".equalsIgnoreCase(role)) {
        SidebarPanel sidebar = new SidebarPanel(this, role);

        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0,0,0,1,new Color(230,230,230)));
        sidebar.setPreferredSize(new Dimension(220, 0));

        add(sidebar, BorderLayout.WEST);
    }

    // MAIN WRAPPER (CARD STYLE)
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setBackground(new Color(245, 247, 250));
    wrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    // CONTENT CARD (IMPORTANT 🔥)
    JPanel contentCard = new JPanel(new BorderLayout());
    contentCard.setBackground(Color.WHITE);
    contentCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    contentPanel.setOpaque(false);

    contentCard.add(contentPanel, BorderLayout.CENTER);
    wrapper.add(contentCard, BorderLayout.CENTER);

    add(wrapper, BorderLayout.CENTER);
}

    /* ================= PRELOAD ================= */

    private void preloadScreens(){

        getScreen(SCREEN_SEARCH);
        getScreen(SCREEN_BUS_LIST);
        getScreen(SCREEN_BUS_DETAILS);
        getScreen(SCREEN_SEATS);
        getScreen(SCREEN_PASSENGER);
        getScreen(SCREEN_PAYMENT);
        getScreen(SCREEN_SUMMARY);
        getScreen(SCREEN_TICKET_PREVIEW);
    }

    /* ================= LOGOUT ================= */

    private void logout(){

        SessionManager.clear();
        Session.clear();

        dispose();

        new LoginFrame();
    }

    /* ================= DEFAULT ================= */

    private void initDefaultScreen(){

        navigationHistory.clear();

        if(role == null){
            throw new RuntimeException("ROLE IS NULL");
        }

        switch(role.trim().toUpperCase()){

            case "CLERK":
            case "BOOKING_CLERK":
                showScreen(SCREEN_CLERK,false);
                break;

            case "ADMIN":
                showScreen(SCREEN_ADMIN,false);
                break;

            case "MANAGER":
                showScreen(SCREEN_MANAGER,false);
                break;

            case "USER":
                showScreen(SCREEN_USER,false);
                break;

            default:
                throw new RuntimeException("UNKNOWN ROLE: "+role);
        }
    }

    /* ================= FACTORY ================= */

    private JPanel createScreen(String key){

        switch(key){

            case SCREEN_USER: return new UserDashboard(this);
            case SCREEN_SEARCH: return new SearchBusPanel(this);
            case SCREEN_BUS_LIST: return new BusListPanel(this);
            case SCREEN_BUS_DETAILS: return new BusDetailsPanel(this);
            case SCREEN_SEATS: return new SeatSelectionPanel(this);
            case SCREEN_PASSENGER: return new PassengerDetailsPanel(this);
            case SCREEN_PAYMENT: return new PaymentPanel(this);
            case SCREEN_SUMMARY: return new BookingSummaryPanel(this);

            case SCREEN_TICKET_PREVIEW: return new TicketPreviewPanel(this);
            case SCREEN_MY_TICKETS: return new MyTicketsPanel(this);
            case SCREEN_CANCEL: return new CancelTicketPanel();

            case SCREEN_WALLET: return new WalletPanel();
            case SCREEN_COMPLAINT: return new ComplaintPanel();
            case SCREEN_SETTINGS: return new SettingsPanel();

            case SCREEN_CLERK: return new BookingClerkDashboard(this);
            case SCREEN_MANAGER: return new ManagerDashboard();
            case SCREEN_ADMIN: return new AdminDashboard();

            case "MANAGE_USERS": return new ManageUsersPanel();
            case "MANAGE_BUSES": return new ManageBusesPanel();
            case "MANAGE_ROUTES": return new ManageRoutesPanel();
            case "MANAGE_SCHEDULES": return new ManageSchedulesPanel();
            case "ADMIN_COMPLAINTS": return new AdminComplaintsPanel();
            case "ADMIN_NEWS": return new AdminNewsPanel();
            case "REPORTS": return new ReportsPanel();
            case "ACCOUNTS": return new AccountsDashboard();

            default: return createErrorPanel(key);
        }
    }

    private JPanel createErrorPanel(String key){

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));

        JLabel label = new JLabel("Screen not found: "+key, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));

        panel.add(label,BorderLayout.CENTER);

        return panel;
    }

    /* ================= GET ================= */

    public JPanel getScreen(String key){

        JPanel screen = screenCache.get(key);

        if(screen == null){
            screen = createScreen(key);
            screenCache.put(key,screen);
            contentPanel.add(screen,key);
        }

        return screen;
    }

    /* ================= NAVIGATION ================= */

    public void showScreen(String key){
        showScreen(key,true);
    }

    private void showScreen(String key, boolean trackHistory){

        if(key == null || key.equals(currentScreen)) return;

        if(trackHistory && currentScreen != null){
            navigationHistory.push(currentScreen);
        }

        currentScreen = key;

        JPanel screen = getScreen(key);

        cardLayout.show(contentPanel,key);

        if(headerPanel != null){
            headerPanel.setPageTitle(formatTitle(key));
        }

        if(screen instanceof Refreshable){
            ((Refreshable) screen).refreshData();
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /* ================= BACK ================= */

    public void goBack(){

        if(!navigationHistory.isEmpty()){
            showScreen(navigationHistory.pop(),false);
        }
    }

    /* ================= REFRESH ================= */

    public void refreshCurrent(){

        if(currentScreen == null) return;

        JPanel panel = screenCache.get(currentScreen);

        if(panel instanceof Refreshable){
            ((Refreshable) panel).refreshData();
        }
    }

    /* ================= TITLE FORMAT ================= */

    private String formatTitle(String key){

        switch(key){

            case SCREEN_USER: return "Dashboard";
            case SCREEN_SEARCH: return "Search Buses";
            case SCREEN_BUS_LIST: return "Available Buses";
            case SCREEN_BUS_DETAILS: return "Bus Details";
            case SCREEN_SEATS: return "Select Seats";
            case SCREEN_PASSENGER: return "Passenger Details";
            case SCREEN_PAYMENT: return "Payment";
            case SCREEN_SUMMARY: return "Booking Summary";

            case SCREEN_TICKET_PREVIEW: return "Ticket Preview";
            case SCREEN_MY_TICKETS: return "My Tickets";
            case SCREEN_CANCEL: return "Cancel Ticket";

            case SCREEN_WALLET: return "Wallet";
            case SCREEN_COMPLAINT: return "Complaints";
            case SCREEN_SETTINGS: return "Settings";

            case SCREEN_CLERK: return "Booking Counter";
            case SCREEN_MANAGER: return "Manager Dashboard";
            case SCREEN_ADMIN: return "Admin Dashboard";

            default:
                return key.replace("_"," ");
        }
    }
}

