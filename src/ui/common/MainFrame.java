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

import util.IconUtil;
import util.PreferencesUtil;
import util.Refreshable;
import util.Session;
import util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Stack;

public class MainFrame extends JFrame {

    

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

    

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private final HashMap<String, JPanel> screenCache = new HashMap<>();
    private final HashMap<String, Long> lastRefreshAtNanos = new HashMap<>();
    private final Stack<String> navigationHistory = new Stack<>();

    private String currentScreen;

    private final String role;
    private static final long REFRESH_THROTTLE_NS = 500_000_000L; 
    private static final boolean PERF_LOG = true;

    private HeaderPanel headerPanel;

    

    public MainFrame(String username, String role) {

        this.role = normalizeRole(role);

        initFrame();
        initLayout();
        initDefaultScreen();

        setVisible(true);
    }

    

    private void initFrame() {

        setTitle("BusYatra");
        setIconImage(IconUtil.load("buslogo.png", 32, 32).getImage());
        setMinimumSize(new Dimension(1200, 720));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    

   private void initLayout() {

    
    headerPanel = new HeaderPanel(this, this::logout);
    headerPanel.setBackground(Color.WHITE);
    headerPanel.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(230,230,230)));
    add(headerPanel, BorderLayout.NORTH);

    
    if (!"CLERK".equalsIgnoreCase(role) && !"BOOKING_CLERK".equalsIgnoreCase(role)) {
        SidebarPanel sidebar = new SidebarPanel(this, role);
        sidebar.setBorder(BorderFactory.createMatteBorder(0,0,0,1,new Color(232,236,242)));
        sidebar.setPreferredSize(new Dimension(252, 0));

        add(sidebar, BorderLayout.WEST);
    }

    
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setBackground(new Color(245, 247, 250));
    wrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    
    JPanel contentCard = new JPanel(new BorderLayout());
    contentCard.setBackground(Color.WHITE);
    contentCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    contentPanel.setOpaque(false);

    contentCard.add(contentPanel, BorderLayout.CENTER);
    wrapper.add(contentCard, BorderLayout.CENTER);

    add(wrapper, BorderLayout.CENTER);
}

    

    private String normalizeRole(String inputRole) {
        if (inputRole == null || inputRole.isBlank()) return "USER";
        return inputRole.trim().toUpperCase();
    }

    

    private void logout(){

        SessionManager.clear();
        Session.clear();

        dispose();

        new LoginFrame();
    }

    

    private void initDefaultScreen(){

        navigationHistory.clear();

        switch(role){

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
                showScreen(SCREEN_USER,false);
        }
    }

    

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
            case "ADMIN_ROUTE_MAP": return new ManageRoutesPanel();
            case "MANAGE_SCHEDULES": return new ManageSchedulesPanel();
            case "ADMIN_COMPLAINTS": return new AdminComplaintsPanel();
            case "ADMIN_NEWS": return new AdminNewsPanel();
            case "ADMIN_BANNERS": return new AdminBannerPanel();
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

    

    public JPanel getScreen(String key){

        JPanel screen = screenCache.get(key);

        if(screen == null){
            long start = System.nanoTime();
            screen = createScreen(key);
            screenCache.put(key,screen);
            contentPanel.add(screen,key);
            logPerf("createScreen(" + key + ")", start);
        }

        return screen;
    }

    

    public void showScreen(String key){
        showScreen(key,true);
    }

    private void showScreen(String key, boolean trackHistory){

        if(key == null || key.equals(currentScreen)) return;
        long start = System.nanoTime();
        boolean newlyCreated = !screenCache.containsKey(key);

        if(trackHistory && currentScreen != null){
            navigationHistory.push(currentScreen);
        }

        currentScreen = key;

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        JPanel screen = getScreen(key);

        cardLayout.show(contentPanel,key);

        if(headerPanel != null){
            headerPanel.setPageTitle(formatTitle(key));
        }

        contentPanel.revalidate();
        contentPanel.repaint();
        setCursor(Cursor.getDefaultCursor());
        logPerf("showScreen(" + key + ")", start);

        
        if(screen instanceof Refreshable
                && shouldRefresh(key)
                && (!newlyCreated || ((Refreshable) screen).refreshOnFirstShow())){
            SwingUtilities.invokeLater(() -> {
                long refreshStart = System.nanoTime();
                ((Refreshable) screen).refreshData();
                logPerf("refreshData(" + key + ")", refreshStart);
            });
        }
    }

    

    public void goBack(){

        if(!navigationHistory.isEmpty()){
            showScreen(navigationHistory.pop(),false);
        }
    }

    

    public void refreshCurrent(){

        if(currentScreen == null) return;

        JPanel panel = screenCache.get(currentScreen);

        if(panel instanceof Refreshable){
            ((Refreshable) panel).refreshData();
        }
    }

    

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
            case "ADMIN_ROUTE_MAP": return "Route Map Management";
            case "ADMIN_BANNERS": return "Banner Management";

            default:
                return key.replace("_"," ");
        }
    }

    private boolean shouldRefresh(String key) {
        long now = System.nanoTime();
        Long last = lastRefreshAtNanos.get(key);
        if (last != null && now - last < REFRESH_THROTTLE_NS) {
            return false;
        }
        lastRefreshAtNanos.put(key, now);
        return true;
    }

    private void logPerf(String action, long startNanos) {
        if (!PERF_LOG) return;
        long ms = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
        if (ms >= 120) {
            System.out.println("[Perf] " + action + " took " + ms + "ms");
        }
    }
}

