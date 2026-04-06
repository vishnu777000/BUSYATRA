package ui.clerk;

import config.UIConfig;
import dao.ClerkDashboardDAO;
import ui.common.DashboardCard;
import util.Refreshable;

import javax.swing.*;
import java.awt.*;

public class ClerkKPIPanel extends JPanel implements Refreshable {

    private DashboardCard ticketsCard;
    private DashboardCard revenueCard;
    private DashboardCard busesCard;
    private DashboardCard seatsCard;

    public ClerkKPIPanel() {

        setLayout(new GridLayout(1, 4, 16, 16));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ticketsCard = card("Tickets Sold", "0", UIConfig.PRIMARY);
        revenueCard = card("Revenue", "INR 0.00", UIConfig.SUCCESS);
        busesCard = card("Buses Today", "0", UIConfig.INFO);
        seatsCard = card("Available Seats", "0", UIConfig.SECONDARY);

        add(ticketsCard);
        add(revenueCard);
        add(busesCard);
        add(seatsCard);
    }

    private DashboardCard card(String title, String value, Color accent) {

        DashboardCard card = new DashboardCard(title, value);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                )
        );
        return card;
    }

    @Override
    public void refreshData() {

        SwingWorker<Void, Void> worker = new SwingWorker<>() {

            ClerkDashboardDAO.DashboardSnapshot snapshot = new ClerkDashboardDAO.DashboardSnapshot();

            @Override
            protected Void doInBackground() {
                try {
                    snapshot = new ClerkDashboardDAO().getTodaySnapshot();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                ticketsCard.setValue(String.valueOf(snapshot.tickets));
                revenueCard.setValue("INR " + String.format("%.2f", snapshot.revenue));
                busesCard.setValue(String.valueOf(snapshot.buses));
                seatsCard.setValue(String.valueOf(snapshot.availableSeats));
            }
        };

        worker.execute();
    }
}
