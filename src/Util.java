package src;

import animation.Grapher;

import java.util.ArrayList;

import static java.awt.Color.HSBtoRGB;

public class Util {

    //this project related stuff
    public static Grapher g;
    public static final int picWidth = 400;
    public static final int picHeight = 400;
    public static ArrayList<Pixel> pixels = new ArrayList<>();
    //animation
    public static final double maxAveDist = 0.25;
    public static final double repulsion = 0.1;
    public static final double targAttraction = 1;
    public static final double dt = 0.01;


    //pre-made colors, used in the Grapher Class
    public static final int RED = rgb(180,40,30);
    public static final int ORANGE = rgb(210, 110, 30);
    public static final int YELLOW = rgb(230, 190, 40);
    public static final int GREEN = rgb(75,180,20);
    public static final int BLUE = rgb(20, 120, 200);
    public static final int PURPLE = rgb(150, 20, 180);
    public static final int WHITE = rgb(200, 200, 200);
    public static final int BLACK = rgb(16, 16, 16);

    /**
     * Transfers a value from one range to another
     * @param lowerIn lower bound of the input range
     * @param upperIn upper bound of the input range
     * @param lowerOut lower bound of the output range
     * @param upperOut upper bound of the output range
     * @param data the value from the input range to be mapped onto the output range
     */
    public static double map(double lowerIn, double upperIn, double lowerOut, double upperOut, double data) {
        return lowerOut + ((data - lowerIn) * (upperOut - lowerOut) / (upperIn - lowerIn));
    }

    /**
     * Converts an <b>r</b>-<b>g</b>-<b>b</b> trio into a hexadecimal value
     */
    public static int rgb(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Converts an <b>r</b>-<b>g</b>-<b>b</b> color into an integer value.
     * <b>r</b>, <b>g</b>, and <b>b</b> are cast to {@code int}
     */
    public static int rgb(double r, double g, double b) {
        return rgb((int)r, (int)g, (int)b);
    }

    /**
     * Converts an <b>h</b>-<b>s</b>-<b>b</b> color into an integer value
     */
    public static int hsb(float h, float s, float b) {
        return HSBtoRGB(h, s, b);
    }

    /**
     * Converts an <b>h</b>-<b>s</b>-<b>b</b> color into an integer value.
     * <b>h</b>, <b>s</b>, and <b>b</b> are cast to {@code double}
     */
    public static int hsb(double h, double s, double b) {
        return HSBtoRGB((float)h, (float)s, (float)b);
    }

    /**
     * Computes the factorial of the integer <b>arg</b>
     */
    public static int factorial(int arg) {
        int prod;
        for(prod = 1; arg > 1; arg--) {
            prod *= arg;
        }
        return prod;
    }

    public static int nPr(int n, int r) {
        return (int) (factorial(n) / (double)factorial(n-r));
    }

    public static int nCr(int n, int r) {
        return (int) (nPr(n,r) / (double)factorial(r));
    }
}
