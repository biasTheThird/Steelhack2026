package ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.JPanel;
import javax.swing.Timer;

import game.Config;
import morph.MorphAnimator;
import morph.MorphData;

/**
 * Draws the morph.
 *
 * The original code wrote straight into a Canvas with a BufferStrategy, which is
 * fast but fights with Swing as soon as there are other components around it.
 * This keeps the same idea - write ints into a BufferedImage's backing array -
 * and then blits that image in paintComponent, which behaves inside a layout.
 */
public final class MorphCanvasPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final BufferedImage frame;
    private final int[] buffer;
    private final Timer ticker;

    private MorphAnimator animator;
    private int dotSize = 2;

    public MorphCanvasPanel() {
        setOpaque(true);
        setBackground(Theme.TRAY);
        setPreferredSize(new Dimension(Config.CANVAS, Config.CANVAS));
        setMinimumSize(new Dimension(280, 280));

        frame = new BufferedImage(Config.CANVAS, Config.CANVAS, BufferedImage.TYPE_INT_RGB);
        buffer = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
        java.util.Arrays.fill(buffer, Theme.CANVAS_BACKGROUND);

        ticker = new Timer(16, e -> {
            if (animator == null) {
                return;
            }
            animator.step();
            animator.render(buffer, Config.CANVAS, Config.CANVAS, Theme.CANVAS_BACKGROUND, dotSize);
            repaint();
            if (animator.isSettled()) {
                ((Timer) e.getSource()).stop();
            }
        });
        ticker.setCoalesce(true);
    }

    /** Starts a fresh morph. Any previous one is discarded. */
    public void play(MorphData data) {
        animator = new MorphAnimator(data);

        // Sparse pixel sets leave gaps in the picture, so grow the dot to cover them.
        //dotSize = data.count < 18000 ? 2 : 1;

        animator.render(buffer, Config.CANVAS, Config.CANVAS, Theme.CANVAS_BACKGROUND, dotSize);
        repaint();

        Timer delay = new Timer(500, e -> ticker.start());
        delay.setRepeats(false);
        delay.start();
    }

    /** Snaps to the finished image, for when a round ends before the morph settles. */
    public void settle() {
        if (animator != null) {
            animator.finish();
            animator.render(buffer, Config.CANVAS, Config.CANVAS, Theme.CANVAS_BACKGROUND, dotSize);
            repaint();
        }
        ticker.stop();
    }

    public void clear() {
        ticker.stop();
        animator = null;
        java.util.Arrays.fill(buffer, Theme.CANVAS_BACKGROUND);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        int side = Math.min(getWidth(), getHeight());
        if (side <= 0) {
            return;
        }
        int x = (getWidth() - side) / 2;
        int y = (getHeight() - side) / 2;

        Graphics2D gfx = (Graphics2D) graphics;
        gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gfx.drawImage(frame, x, y, side, side, null);
    }
}
