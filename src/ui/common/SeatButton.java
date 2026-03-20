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

    private static final Color AVAILABLE_BG = new Color(240, 240, 240);
    private static final Color AVAILABLE_HOVER = new Color(220, 220, 220);

    private static final Color SELECTED_BG = new Color(0, 150, 136);
    private static final Color SELECTED_HOVER = new Color(0, 121, 107);

    private static final Color BOOKED_BG = new Color(220, 53, 69);

    public SeatButton(String seatNo) {
        super(seatNo);

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);

        setFont(new Font("Segoe UI", Font.BOLD, 12));
        setPreferredSize(new Dimension(58, 42));
        setMargin(new Insets(0, 0, 0, 0));
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        installHoverEffect();
        setState(State.AVAILABLE);
    }

    public void setState(State newState) {
        this.state = newState;
        if (newState == State.BOOKED) {
            setForeground(Color.WHITE);
        } else if (newState == State.SELECTED) {
            setForeground(Color.WHITE);
        } else {
            setForeground(new Color(33, 37, 41));
        }
        repaint();
    }

    public State getState() {
        return state;
    }

    public void toggle() {
        if (state == State.AVAILABLE) {
            setState(State.SELECTED);
        } else if (state == State.SELECTED) {
            setState(State.AVAILABLE);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackgroundColor());
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
                repaint();
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

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (!enabled) {
            state = State.BOOKED;
            setCursor(Cursor.getDefaultCursor());
        }
    }
}
