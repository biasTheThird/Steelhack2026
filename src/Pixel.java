package src;

public class Pixel {

    public final int r, g, b;
    public final double xStart;
    public final double yStart;

    public double xPos, xTarg, xVel, yPos, yTarg, yVel;

    public Pixel(int r, int g, int b, double xStart, double yStart) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.xStart = xStart;
        this.yStart = yStart;
        xPos = xStart;
        yPos = yStart;
        xVel = 0;
        yVel = 0;
    }

    public void setTarg(double xTarg, double yTarg) {
        this.xTarg = xTarg;
        this.yTarg = yTarg;
    }

    /**
     * Advances this pixel one frame along its precomputed path.
     * Snaps exactly to <b>targ</b> once <b>totalFrames</b> have elapsed,
     * so floating point drift never leaves the pixel short of/past target.
     * No-op if {@link #setTarg} hasn't been called yet.
     */
    public void update() {
        //TODO
    }
}