package animation;

public class Point {

    //rectangular coordinates
    private double x, y;



    /* ----- Constructors ----- */

    /**
     * Instantiates a point at the origin
     */
    public Point() {
        this(0, 0);
    }

    /**
     * Instantiates a point in rectangular at (<b>x</b>, <b>y</b>)
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Instantiates a point by copying <b>p</b>'s values
     */
    public Point(Point p) {
        this(p.getX(), p.getY());
    }





    /* ----- Point Operations ----- */

    /**
     * @return a copy of this point
     */
    public Point copy() {
        return new Point(x, y);
    }

    /**
     * Takes the sum of this point and <b>p</b>
     */
    public Point add(Point p) {
        return new Point(
                getX() + p.getX(),
                getY() + p.getY()
        );
    }

    /**
     * Takes the sum of this point and negative <b>p</b>
     */
    public Point subtract(Point p) {
        return new Point(
                getX() - p.getX(),
                getY() - p.getY()
        );
    }

    /**
     * Takes the product of this point and <b>scalar</b> by multiplying
     * the x and y components of this point by <b>scalar</b>
     * @param scalar value to scale point <b>p</b> by
     */
    public Point scale(double scalar) {
        return new Point(
                getX() * scalar,
                getY() * scalar
        );
    }

    /**
     * @return the distance between this point and <b>p2</b>
     */
    public double distance(Point p) {
        return Math.hypot(getX() - p.getX(), getY() - p.getY());
    }

    /**
     * Computes the distance between the origin and this point
     * @return sqrt(p<sub>x</sub><sup>2</sup> + p<sub>y</sub><sup>2</sup>)
     */
    public double magnitude() {
        return Math.hypot(getX(), getY());
    }

    /**
     * Computes the dot product of this point and {@link Point} {@code p}
     */
    public double dotProd(Point p) {
        return (x * p.x) + (y * p.y);
    }

    /**
     * @return the point <b>p</b> as a string: (x, y)
     */
    @Override
    public String toString() {
        return "(" + getX() + ", " + getY() + ")";
    }





    /* ----- Static Methods ----- */

    /**
     * Clamps <b>theta</b> between -&pi; and &pi;
     */
    public static double clampTheta(double theta) {
        if(theta > Math.PI) return clampTheta(theta - Math.PI*2);
        if(theta < -Math.PI) return clampTheta(theta + Math.PI*2);
        return theta;
    }

    /**
     * Takes the sum of <b>p1</b> and <b>p2</b>
     */
    public static Point add(Point p1, Point p2) {
        return new Point(p1.getX() + p2.getX(), p1.getY() + p2.getY());
    }

    /**
     * Takes the sum of <b>p1</b> and negative <b>p2</b>
     */
    public static Point subtract(Point p1, Point p2) {
        return new Point(p1.getX() - p2.getX(), p1.getY() - p2.getY());
    }

    /**
     * Takes the product of <b>scalar</b> and <b>p</b> by multiplying the x and y components of <b>p</b> by <b>scalar</b>
     * @param p point to scale
     * @param scalar value to scale point <b>p</b> by
     */
    public static Point scale(Point p, double scalar) {
        return new Point(p.getX() * scalar, p.getY() * scalar);
    }

    /**
     * @return the distance between <b>p1</b> and <b>p2</b>
     */
    public static double distance(Point p1, Point p2) {
        return Math.hypot(p1.getX() - p2.getX(), p1.getY() - p2.getY());
    }

    /**
     * Computes the distance between the origin and the point
     * @return sqrt(p<sub>x</sub><sup>2</sup> + p<sub>y</sub><sup>2</sup>)
     */
    public static double magnitude(Point p) {
        return Math.hypot(p.getX(), p.getY());
    }

    /**
     * Computes the dot product between {@link Point} {@code p1} and {@link Point} {@code p2}
     */
    public static double dotProd(Point p1, Point p2) {
        return (p1.x * p2.x) + (p1.y * p2.y);
    }

    /**
     * @return the point <b>p</b> as a string: (x, y)
     */
    public static String toString(Point p) {
        return "(" + p.getX() + ", " + p.getY() + ")";
    }





    /* ----- Rectangular and Polar Conversions ----- */

    /**
     * Computes the distance between the origin and the point
     * @return sqrt(<i><b>x</b></i><sup>2</sup>&nbsp;+<i><b>y</b></i><sup>2</sup>)
     */
    public static double radiusFromRectangular(double x, double y) {
        return Math.hypot(x, y);
    }

    /**
     * Computes the angle between the positive x-axis and the point. Positive theta is counterclockwise from the x-axis
     * @return positive or negative arctan(<b>y</b>/<b>x</b>) depending on the quadrant of (<b>y</b>,<b>x</b>)
     * <ul>
     *     <li>positive when (<b>x</b>, <b>y</b>) is in quadrant 1 or 2
     *     <li>negative when (<b>x</b>,<b>y</b>) is in quadrant 3 or 4
     * </ul>
     */
    public static double thetaFromRectangular(double x, double y) {
        return Math.atan2(y, x);
    }

    /**
     * Computes the x coordinate of the polar coordinates (<b>radius</b>, <b>theta</b>)
     * @return <b>radius</b> cos(<b>theta</b>)
     */
    public static double xFromPolar(double radius, double theta) {
        return radius * Math.cos(theta);
    }

    /**
     * Computes the y coordinate of the polar coordinates (<b>radius</b>, <b>theta</b>)
     * @return <b>radius</b> sin(<b>theta</b>)
     */
    public static double yFromPolar(double radius, double theta) {
        return radius * Math.sin(theta);
    }





    /* ----- Getters and Setters ----- */

    public double getX() {
        return x;
    }
    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }
    public void setY(double y) {
        this.y = y;
    }
    
    
    /**
     * Radius is always >= 0. If a negative number is input, theta will increase by &pi;, then be clamped
     */
    public double getRadius() {
        return radiusFromRectangular(getX(), getY());
    }
    /**
     * Radius is always >= 0. If a negative number is input, theta will increase by &pi;
     */
    public void setRadius(double radius) {
        double scalar = radius / magnitude();
        x *= scalar;
        y *= scalar;
    }


    /**
     * Theta is clamped between -&pi; and &pi;
     */
    public double getTheta() {
        return thetaFromRectangular(getX(), getY());
    }
    /**
     * Theta is clamped between -&pi; and &pi;, then x and y are updated
     */
    public void setTheta(double theta) {
        theta = clampTheta(theta);

        x = xFromPolar(getRadius(), theta);
        y = yFromPolar(getRadius(), theta);
    }
}
