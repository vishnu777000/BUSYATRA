package ui.booking;

import config.UIConfig;
import dao.RouteDAO;
import dao.StopDAO;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RouteMapPanel extends JPanel {

public RouteMapPanel(int routeId){

    setLayout(new BorderLayout(16,16));
    setBackground(UIConfig.BACKGROUND);

    add(mapSection(routeId), BorderLayout.CENTER);
    add(stopsSection(routeId), BorderLayout.SOUTH);
}

/* ================= MAP SECTION ================= */

private JComponent mapSection(int routeId){

    JPanel card = new JPanel(new BorderLayout());
    UIConfig.styleCard(card);

    JLabel title = new JLabel("Route Map");
    title.setFont(UIConfig.FONT_SUBTITLE);

    JLabel mapLabel = new JLabel("No Map Available", SwingConstants.CENTER);
    mapLabel.setFont(UIConfig.FONT_SMALL);
    mapLabel.setForeground(UIConfig.TEXT_LIGHT);

    try{

        String mapImage = new RouteDAO().getRouteMap(routeId);

        if(mapImage != null){

            ImageIcon icon = new ImageIcon(
                    getClass().getResource("/resources/routes/" + mapImage)
            );

            Image scaled = icon.getImage().getScaledInstance(
                    500, 220, Image.SCALE_SMOOTH
            );

            mapLabel.setIcon(new ImageIcon(scaled));
            mapLabel.setText("");
        }

    }catch(Exception e){
        // fallback already handled
    }

    card.add(title, BorderLayout.NORTH);
    card.add(mapLabel, BorderLayout.CENTER);

    return card;
}

/* ================= STOPS SECTION ================= */

private JComponent stopsSection(int routeId){

    JPanel card = new JPanel(new BorderLayout());
    UIConfig.styleCard(card);

    JLabel title = new JLabel("Stops");
    title.setFont(UIConfig.FONT_SUBTITLE);

    JPanel list = new JPanel();
    list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
    list.setOpaque(false);

    List<String> stops = new StopDAO().getStopsByRoute(routeId);

    if(stops.isEmpty()){

        JLabel empty = new JLabel("No stops available");
        empty.setFont(UIConfig.FONT_SMALL);
        empty.setForeground(UIConfig.TEXT_LIGHT);

        list.add(empty);
    }

    for(String s : stops){

        JLabel stop = new JLabel("• " + s);
        stop.setFont(UIConfig.FONT_NORMAL);

        list.add(stop);
        list.add(Box.createVerticalStrut(5));
    }

    JScrollPane scroll = new JScrollPane(list);
    scroll.setBorder(null);
    scroll.setPreferredSize(new Dimension(0,120));

    card.add(title, BorderLayout.NORTH);
    card.add(scroll, BorderLayout.CENTER);

    return card;
}

}
