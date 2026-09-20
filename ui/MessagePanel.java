package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Stands in for the picture when there isn't one yet: waiting for players,
 * waiting for the drawer, or matching pixels.
 *
 * The bar is the only moving thing on this screen, which is the point - the wait
 * for a morph to build is long enough that people need to see it is working.
 */
public final class MessagePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel line = new JLabel("", SwingConstants.CENTER);
    private final JLabel detail = new JLabel("", SwingConstants.CENTER);
    private final Bar bar = new Bar();

    public MessagePanel() {
        setBackground(Theme.TRAY);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(24, 40, 24, 40));

        line.setFont(Theme.TITLE);
        line.setForeground(Theme.PAPER);
        line.setAlignmentX(CENTER_ALIGNMENT);

        detail.setFont(Theme.SMALL);
        detail.setForeground(Theme.GRAPHITE);
        detail.setAlignmentX(CENTER_ALIGNMENT);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(Box.createVerticalGlue());
        stack.add(line);
        stack.add(Box.createVerticalStrut(10));
        stack.add(detail);
        stack.add(Box.createVerticalStrut(18));
        stack.add(bar);
        stack.add(Box.createVerticalGlue());

        add(stack, BorderLayout.CENTER);
    }

    public void message(String text, String subtext) {
        line.setText(text == null ? "" : text);
        detail.setText(subtext == null ? "" : subtext);
        bar.setVisible(false);
        repaint();
    }

    public void progress(int percent, String note) {
        line.setText("Matching pixels to the target");
        detail.setText(note == null ? "" : note);
        bar.setVisible(true);
        bar.set(percent);
    }

    /** A hairline that fills left to right. Nothing else on this screen moves. */
    private static final class Bar extends JPanel {

        private static final long serialVersionUID = 1L;

        private int percent;

        Bar() {
            setOpaque(false);
            setAlignmentX(CENTER_ALIGNMENT);
            setPreferredSize(new Dimension(260, 4));
            setMaximumSize(new Dimension(260, 4));
            setVisible(false);
        }

        void set(int percent) {
            this.percent = Math.max(0, Math.min(100, percent));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D gfx = (Graphics2D) graphics;
            gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            gfx.setColor(Theme.EDGE);
            gfx.fillRect(0, 0, getWidth(), getHeight());

            gfx.setColor(Theme.DEVELOPER);
            gfx.fillRect(0, 0, (int) (getWidth() * (percent / 100.0)), getHeight());
        }
    }
}
