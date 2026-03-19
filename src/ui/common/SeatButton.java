package ui.common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SeatButton extends JButton {

    public enum State {
        AVAILABLE, SELECTED, BOOKED
    }

    private State state = State.AVAILABLE;

    /* ================= COLORS ================= */

    private static final Color AVAILABLE_BG = new Color(240, 240, 240);
    private static final Color AVAILABLE_HOVER = new Color(220, 220, 220);

    private static final Color SELECTED_BG = new Color(0, 150, 136);
    private static final Color SELECTED_HOVER = new Color(0, 121, 107);

    private static final Color BOOKED_BG = new Color(200, 200, 200);

    public SeatButton(String seatNo) {
        super(seatNo);

        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false); // 🔥 for rounded UI

        setFont(new Font("Segoe UI", Font.BOLD, 12));
        setPreferredSize(new Dimension(46, 38));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        installHoverEffect();
        setState(State.AVAILABLE);
    }

    /* ================= STATE ================= */

    public void setState(State newState) {
        this.state = newState;
        repaint();
    }

    public State getState() {
        return state;
    }

    /* ================= TOGGLE ================= */

    public void toggle() {
        if (state == State.AVAILABLE) {
            setState(State.SELECTED);
        } else if (state == State.SELECTED) {
            setState(State.AVAILABLE);
        }
    }

    /* ================= PAINT (ROUNDED UI) ================= */

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        Color bg = getBackgroundColor();

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

        super.paintComponent(g2);

        g2.dispose();
    }

    private Color getBackgroundColor() {

        return switch (state) {

            case AVAILABLE -> AVAILABLE_BG;
            case SELECTED -> SELECTED_BG;
            case BOOKED -> BOOKED_BG;
        };
    }

    /* ================= HOVER ================= */

    private void installHoverEffect() {

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                if (state == State.AVAILABLE) {
                    setBackground(AVAILABLE_HOVER);
                } else if (state == State.SELECTED) {
                    setBackground(SELECTED_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                repaint(); // reset properly
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (state != State.BOOKED) {
                    setBackground(getBackgroundColor().darker());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                repaint();
            }
        });
    }

    /* ================= ENABLE/DISABLE ================= */

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (!enabled) {
            state = State.BOOKED;
            setCursor(Cursor.getDefaultCursor());
        }
    }
}