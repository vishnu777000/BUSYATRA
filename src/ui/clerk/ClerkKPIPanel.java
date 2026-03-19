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
        revenueCard = card("Revenue", "₹ 0", UIConfig.SUCCESS);
        busesCard = card("Buses Today", "0", UIConfig.INFO);
        seatsCard = card("Available Seats", "0", UIConfig.SECONDARY);

        add(ticketsCard);
        add(revenueCard);
        add(busesCard);
        add(seatsCard);

        refreshData();
    }

    /* ================= KPI CARD ================= */

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

    /* ================= LOAD DATA ================= */

    @Override
    public void refreshData() {

        SwingWorker<Void, Void> worker = new SwingWorker<>() {

            int tickets = 0;
            double revenue = 0;
            int buses = 0;
            int seats = 0;

            @Override
            protected Void doInBackground() {

                try {

                    ClerkDashboardDAO dao = new ClerkDashboardDAO();

                    tickets = dao.getTodayTickets();
                    revenue = dao.getTodayRevenue();
                    buses = dao.getTodayBuses();
                    seats = dao.getAvailableSeats();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void done() {

                ticketsCard.setValue(String.valueOf(tickets));
                revenueCard.setValue("₹ " + revenue);
                busesCard.setValue(String.valueOf(buses));
                seatsCard.setValue(String.valueOf(seats));
            }
        };

        worker.execute();
    }
}