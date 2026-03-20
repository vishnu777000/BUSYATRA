package ui.clerk;

import config.UIConfig;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ClerkTodaySchedulePanel extends JPanel {

    public ClerkTodaySchedulePanel(){

        setLayout(new BorderLayout(10,10));
        setBackground(UIConfig.CARD);
        setBorder(BorderFactory
                .createEmptyBorder(12,12,12,12));

        JLabel title =
                new JLabel("Today's Schedule");
        title.setFont(UIConfig.FONT_TITLE);

        String[] cols={
                "Route",
                "Bus No",
                "Departure",
                "Seats",
                "Status"
        };

        DefaultTableModel model=
                new DefaultTableModel(cols,0);

        JTable table=
                new JTable(model);
        UIConfig.styleTable(table);

        JScrollPane scroll=
                new JScrollPane(table);
        UIConfig.styleScroll(scroll);

        add(title,
                BorderLayout.NORTH);
        add(scroll,
                BorderLayout.CENTER);

        // ===== SAMPLE DATA =====
        model.addRow(new Object[]{
                "Hyd → Blr",
                "AP09 AB1234",
                "22:30",
                "12",
                "ON TIME"
        });

        model.addRow(new Object[]{
                "Hyd → Vizag",
                "TS10 XY5678",
                "21:00",
                "0",
                "FULL"
        });
    }
}
