package src;

public class Pixel {

    public final int r, g, b;
    public final Point startPos;

    public Point pos, targ, vel;

    public Pixel(int r, int g, int b, Point startPos) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.startPos = startPos;
    }
    public Pixel(int r, int g, int b, int xStart, int yStart) {
        this(r, g, b, new Point(xStart, yStart));
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

    public void setTarg(Point targ) {
        this.targ = targ;
    }

    public Point getVel() {
        return vel;
    }

    public void setVel(Point vel) {
        this.vel = vel;
    }
}
