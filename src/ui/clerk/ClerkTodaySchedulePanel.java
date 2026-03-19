package ui.clerk;

import config.UIConfig;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
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

        table.setRowHeight(30);
        table.setFont(UIConfig.FONT_NORMAL);
        table.setSelectionBackground(
                new Color(232,245,233));

        // ===== HEADER STYLE =====
        JTableHeader header=
                table.getTableHeader();
        header.setFont(
                new Font("Segoe UI",
                        Font.BOLD,14));
        header.setBackground(
                UIConfig.PRIMARY);
        header.setForeground(
                Color.WHITE);

        JScrollPane scroll=
                new JScrollPane(table);
        scroll.setBorder(
                BorderFactory
                        .createLineBorder(
                                new Color(210,210,210)));

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
