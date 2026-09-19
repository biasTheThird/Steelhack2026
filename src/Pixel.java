package src;

public class Pixel {

    int r, g, b;
    double xStart;
    double yStart;

    public double getxCurrent() {
        return xCurrent;
    }

    public void setxCurrent(double xCurrent) {
        this.xCurrent = xCurrent;
    }

    public double getyCurrent() {
        return yCurrent;
    }

    public void setyCurrent(double yCurrent) {
        this.yCurrent = yCurrent;
    }

    public double getxTarg() {
        return xTarg;
    }

    public void setxTarg(double xTarg) {
        this.xTarg = xTarg;
    }

    public double getyTarg() {
        return yTarg;
    }

    public void setyTarg(double yTarg) {
        this.yTarg = yTarg;
    }

    public double getxVel() {
        return xVel;
    }

    public void setxVel(double xVel) {
        this.xVel = xVel;
    }

    public double getyVel() {
        return yVel;
    }

    public void setyVel(double yVel) {
        this.yVel = yVel;
    }

    double xCurrent;
    double yCurrent;
    double xTarg;
    double yTarg;
    double xVel;
    double yVel;

    public Pixel(int r, int g, int b, double x, double y) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.xStart = x;
        this.yStart = y;
    }
}
