package ui;

import java.awt.Color;
import java.awt.Font;

/**
 * A darkroom: the room is dark so the picture is the only bright thing in it.
 *
 * Everything outside the morph field sits in a narrow band of cool slate. The
 * teal is the one saturated colour and it is spent on things that are true right
 * now - your own turn, a correct guess, the progress bar filling. Amber appears
 * only when the clock is nearly out, so it means one thing.
 */
public final class Theme {

    public static final Color TRAY = new Color(0x0B0E11);      // around the picture
    public static final Color INK = new Color(0x12171C);       // panels
    public static final Color EDGE = new Color(0x1E262E);      // hairlines
    public static final Color PAPER = new Color(0xE8E4DB);     // primary text
    public static final Color GRAPHITE = new Color(0x7C8896);  // secondary text
    public static final Color DEVELOPER = new Color(0x3FB9A0); // accent
    public static final Color FLARE = new Color(0xE4A33C);     // clock running out

    public static final int CANVAS_BACKGROUND = 0x0B0E11;

    public static final Font BLANKS = new Font("Georgia", Font.PLAIN, 30);
    public static final Font TITLE = new Font("Georgia", Font.PLAIN, 17);
    public static final Font CLOCK = new Font("Georgia", Font.PLAIN, 34);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BODY_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);

    private Theme() {
    }
}
