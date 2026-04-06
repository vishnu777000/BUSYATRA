package config;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.HierarchyEvent;

public class UIConfig {

    public static final Color PRIMARY = new Color(214, 45, 62);
    public static final Color PRIMARY_HOVER = new Color(186, 33, 48);
    public static final Color PRIMARY_PRESSED = new Color(161, 27, 41);

    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color SECONDARY = new Color(108, 117, 125);
    public static final Color DANGER = new Color(220, 53, 69);
    public static final Color INFO = new Color(2, 132, 199);
    public static final Color WARNING = new Color(245, 158, 11);

    public static final Color BACKGROUND = new Color(244, 246, 250);
    public static final Color CARD = Color.WHITE;

    public static final Color TEXT = new Color(40, 40, 40);
    public static final Color TEXT_LIGHT = new Color(120, 120, 120);
    public static final Color BORDER = new Color(228, 232, 238);

    public static final Color SEAT_AVAILABLE = new Color(22, 163, 74);
    public static final Color SEAT_BOOKED = new Color(220, 53, 69);
    public static final Color SEAT_LOCKED = new Color(245, 158, 11);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    public static void styleCard(JPanel p) {
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
    }

    public static void styleSidebar(JPanel p) {
        p.setBackground(Color.WHITE);
    }

    public static void styleSidebarButton(JButton b) {
        b.setFont(FONT_NORMAL);
        b.setForeground(TEXT);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(200, 45));

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(new Color(245, 245, 245));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(Color.WHITE);
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(new Color(236, 239, 243));
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(new Color(245, 245, 245));
            }
        });
    }

    private static void baseBtn(JButton b, Color base, Color hover) {
        b.setFont(FONT_NORMAL);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(140, 36));
        b.setBackground(base);
        b.setForeground(Color.WHITE);

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(hover);
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(base);
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(PRIMARY_PRESSED);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(hover);
            }
        });
    }

    public static void primaryBtn(JButton b) {
        baseBtn(b, PRIMARY, PRIMARY_HOVER);
    }

    public static void successBtn(JButton b) {
        baseBtn(b, SUCCESS, SUCCESS.darker());
    }

    public static void dangerBtn(JButton b) {
        baseBtn(b, DANGER, PRIMARY_HOVER);
    }

    public static void infoBtn(JButton b) {
        baseBtn(b, INFO, INFO.darker());
    }

    public static void secondaryBtn(JButton b) {
        b.setFont(FONT_NORMAL);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(140, 36));
        b.setBackground(Color.WHITE);
        b.setForeground(TEXT);
        b.setBorder(BorderFactory.createLineBorder(BORDER));

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(new Color(245, 245, 245));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(Color.WHITE);
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(new Color(236, 239, 243));
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(new Color(245, 245, 245));
            }
        });
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setFont(FONT_NORMAL);
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(184, 199, 219));
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(255, 232, 236));
        table.setSelectionForeground(TEXT);
        table.setFocusable(true);
        table.setCellSelectionEnabled(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int column
            ) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    setBackground(tbl.getSelectionBackground());
                    setForeground(tbl.getSelectionForeground());
                } else {
                    setForeground(TEXT);
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(252, 253, 255));
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);

        JTableHeader h = table.getTableHeader();
        TableCellRenderer headerRenderer = createTableHeaderRenderer();
        h.setFont(new Font("Segoe UI", Font.BOLD, 13));
        h.setOpaque(true);
        h.setBackground(new Color(255, 240, 243));
        h.setForeground(new Color(128, 24, 38));
        h.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(219, 83, 102)));
        h.setReorderingAllowed(false);
        h.setResizingAllowed(true);
        h.setPreferredSize(new Dimension(0, 34));
        h.setDefaultRenderer(headerRenderer);
        synchronizeTableHeaders(table, headerRenderer);

        Runnable installHeader = () -> configureTableScrollPane(table);
        table.addHierarchyListener(e -> {
            long flags = e.getChangeFlags();
            if ((flags & HierarchyEvent.PARENT_CHANGED) != 0L
                    || (flags & HierarchyEvent.SHOWING_CHANGED) != 0L) {
                SwingUtilities.invokeLater(installHeader);
            }
        });
        SwingUtilities.invokeLater(installHeader);
    }

    public static void styleField(JTextField f) {
        f.setFont(FONT_NORMAL);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    public static void styleCombo(JComboBox<?> c) {
        c.setFont(FONT_NORMAL);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    public static void styleScroll(JScrollPane sp) {
        sp.setBorder(BorderFactory.createLineBorder(new Color(214, 224, 238), 1));
        sp.setBackground(Color.WHITE);
        sp.getViewport().setBackground(Color.WHITE);
    }

    public static JPanel dashboardCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        styleCard(card);

        JLabel t = new JLabel(title);
        t.setFont(FONT_SUBTITLE);
        t.setForeground(TEXT_LIGHT);

        JLabel v = new JLabel(value, SwingConstants.CENTER);
        v.setFont(new Font("Segoe UI", Font.BOLD, 22));
        v.setForeground(PRIMARY);

        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        return card;
    }

    public static JLabel statusLabel(String text, Color c) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setOpaque(true);
        l.setBackground(c);
        l.setForeground(Color.WHITE);
        l.setFont(FONT_SMALL);
        l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return l;
    }

    private static void configureTableScrollPane(JTable table) {
        if (table == null) return;

        Container parent = table.getParent();
        if (!(parent instanceof JViewport)) return;

        Container grandParent = parent.getParent();
        if (!(grandParent instanceof JScrollPane)) return;

        JScrollPane scrollPane = (JScrollPane) grandParent;
        JTableHeader header = table.getTableHeader();
        if (header == null) return;

        TableCellRenderer headerRenderer = header.getDefaultRenderer();
        if (headerRenderer == null) {
            headerRenderer = createTableHeaderRenderer();
            header.setDefaultRenderer(headerRenderer);
        }
        synchronizeTableHeaders(table, headerRenderer);
        styleScroll(scrollPane);
        scrollPane.setColumnHeaderView(header);

        JViewport columnHeader = scrollPane.getColumnHeader();
        if (columnHeader != null) {
            columnHeader.setBackground(header.getBackground());
            columnHeader.setOpaque(true);
            columnHeader.setPreferredSize(new Dimension(0, 34));
        }

        JPanel corner = new JPanel();
        corner.setBackground(header.getBackground());
        corner.setBorder(header.getBorder());
        scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, corner);
        header.revalidate();
        header.repaint();
    }

    private static String resolveHeaderText(JTable table, Object value, int viewColumn) {
        if (value != null) {
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) {
                return text;
            }
        }

        if (table == null || table.getModel() == null) {
            return "";
        }

        if (viewColumn >= 0 && viewColumn < table.getColumnCount()) {
            int modelColumn = table.convertColumnIndexToModel(viewColumn);
            if (modelColumn >= 0 && modelColumn < table.getModel().getColumnCount()) {
                String text = table.getModel().getColumnName(modelColumn);
                return text == null ? "" : text;
            }
        }

        return "";
    }

    private static TableCellRenderer createTableHeaderRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
            ) {
                super.getTableCellRendererComponent(table, resolveHeaderText(table, value, column), false, false, row, column);
                setHorizontalAlignment(SwingConstants.LEFT);
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                setForeground(new Color(128, 24, 38));
                setBackground(new Color(255, 240, 243));
                setOpaque(true);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(219, 83, 102)),
                        BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
                return this;
            }
        };
    }

    private static void synchronizeTableHeaders(JTable table, TableCellRenderer headerRenderer) {
        if (table == null || table.getModel() == null) return;

        int columnCount = Math.min(table.getColumnModel().getColumnCount(), table.getModel().getColumnCount());
        for (int i = 0; i < columnCount; i++) {
            table.getColumnModel().getColumn(i).setHeaderValue(table.getModel().getColumnName(i));
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
    }
}
