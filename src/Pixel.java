package src;

public class Pixel {

    public final int r, g, b;
    public final Point startPos;

    private Point pos, targ, vel;

    private int totalFrames;
    private int framesElapsed;

    public Pixel(int r, int g, int b, Point startPos) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.startPos = startPos;
        this.pos = startPos.copy();
    }

    public Pixel(int r, int g, int b, int xStart, int yStart) {
        this(r, g, b, new Point(xStart, yStart));
    }

    /**
     * Sets this pixel's destination and precomputes a constant-velocity
     * path from its current position to <b>targ</b>, to be covered over
     * <b>totalFrames</b> calls to {@link #update()}.
     */
    public void setTarg(Point targ, int totalFrames) {
        this.targ = targ;
        this.totalFrames = totalFrames;
        this.framesElapsed = 0;
        this.vel = targ.subtract(pos).scale(1.0 / totalFrames);
    }

    /**
     * Advances this pixel one frame along its precomputed path.
     * Snaps exactly to <b>targ</b> once <b>totalFrames</b> have elapsed,
     * so floating point drift never leaves the pixel short of/past target.
     * No-op if {@link #setTarg} hasn't been called yet.
     */
    public void update() {
        if (targ == null) return;

        if (framesElapsed >= totalFrames) {
            pos = targ.copy();
            return;
        }

        pos = pos.add(vel);
        framesElapsed++;
    }

    /**
     * @return true once this pixel has reached its target position
     */
    public boolean hasArrived() {
        return targ != null && framesElapsed >= totalFrames;
    }

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }

    public Point getStartPos() {
        return startPos;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }

    public Point getTarg() {
        return targ;
    }

    public Point getVel() {
        return vel;
    }
}