package ui.booking;

import config.UIConfig;
import dao.SearchDAO;
import ui.common.MainFrame;
import util.BookingContext;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BusListPanel extends JPanel implements util.Refreshable {

private final MainFrame frame;
private JPanel resultsPanel;

public BusListPanel(MainFrame frame){

    this.frame = frame;

    setLayout(new BorderLayout(20,20));
    setBackground(UIConfig.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    add(header(),BorderLayout.NORTH);
    add(results(),BorderLayout.CENTER);
}

/* ================= HEADER ================= */

private JComponent header(){

    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JButton back = new JButton("← Back");
    UIConfig.secondaryBtn(back);
    back.setPreferredSize(new Dimension(120,35));

    back.addActionListener(e -> frame.goBack());

    JLabel title = new JLabel("Available Buses",SwingConstants.CENTER);
    title.setFont(UIConfig.FONT_TITLE);

    panel.add(back,BorderLayout.WEST);
    panel.add(title,BorderLayout.CENTER);

    return panel;
}

/* ================= RESULTS ================= */

private JComponent results(){

    resultsPanel = new JPanel();
    resultsPanel.setLayout(new BoxLayout(resultsPanel,BoxLayout.Y_AXIS));
    resultsPanel.setOpaque(false);

    JScrollPane scroll = new JScrollPane(resultsPanel);
    scroll.setBorder(null);
    scroll.getVerticalScrollBar().setUnitIncrement(16);

    return scroll;
}

/* ================= LOAD ================= */

@Override
public void refreshData(){

    resultsPanel.removeAll();

    try{

        List<String[]> list =
                new SearchDAO().searchSchedules(
                        BookingContext.fromStop,
                        BookingContext.toStop,
                        BookingContext.journeyDate
                );

        if(list.isEmpty()){

            JLabel no = new JLabel("No buses available 😕");
            no.setFont(UIConfig.FONT_SUBTITLE);
            no.setAlignmentX(Component.CENTER_ALIGNMENT);

            resultsPanel.add(Box.createVerticalStrut(50));
            resultsPanel.add(no);
            return;
        }

        for(String[] r : list){

            int scheduleId = Integer.parseInt(r[0]);
            int routeId    = Integer.parseInt(r[1]);

            String operator = r[2];
            String busType  = r[3];
            String dep      = r[4];
            String arr      = r[5];
            double fare     = Double.parseDouble(r[6]);
            int seats       = Integer.parseInt(r[7]);

            BusCardPanel card = new BusCardPanel(
                    scheduleId,
                    operator,
                    busType,
                    BookingContext.fromStop + " → " + BookingContext.toStop,
                    dep,
                    arr,
                    seats,
                    fare,
                    () -> {

                        BookingContext.scheduleId = scheduleId;
                        BookingContext.routeId = routeId;
                        BookingContext.farePerSeat = fare;

                        frame.showScreen(MainFrame.SCREEN_BUS_DETAILS);
                    }
            );

            resultsPanel.add(card);
            resultsPanel.add(Box.createVerticalStrut(15));
        }

    }catch(Exception e){
        e.printStackTrace();
    }

    resultsPanel.revalidate();
    resultsPanel.repaint();
}

}
