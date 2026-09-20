package morph;

import game.Config;

/**
 * Runs one morph. The original animation kept its pixels in a static list, which
 * meant one morph per process; this holds its own state so a client can throw the
 * old round away and start a new one.
 *
 * Same spring model as before: each pixel accelerates toward its target in
 * proportion to how far away it is, with velocity bled off every step.
 */
public final class MorphAnimator {

    private final int count;
    private final int[] colour;
    private final double[] xPos;
    private final double[] yPos;
    private final double[] xVel;
    private final double[] yVel;
    private final double[] xTarget;
    private final double[] yTarget;

    private final double dt;
    private boolean settled;

    public MorphAnimator(MorphData data) {
        this(data, Config.MORPH_TIME_SCALE);
    }

    public MorphAnimator(MorphData data, double timeScale) {
        this.count = data.count;
        this.dt = Config.DT * timeScale;

        colour = new int[count];
        xPos = new double[count];
        yPos = new double[count];
        xVel = new double[count];
        yVel = new double[count];
        xTarget = new double[count];
        yTarget = new double[count];

        for (int i = 0; i < count; i++) {
            colour[i] = (data.red(i) << 16) | (data.green(i) << 8) | data.blue(i);
            xPos[i] = data.xStart[i];
            yPos[i] = data.yStart[i];
            xTarget[i] = data.xTarget[i];
            yTarget[i] = data.yTarget[i];
        }
    }

    public int pixelCount() {
        return count;
    }

    public boolean isSettled() {
        return settled;
    }

    /** How far along the morph is, 0 at the source image and 1 once it has arrived. */
    public double progress() {
        if (count == 0) {
            return 1.0;
        }
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += Math.hypot(xPos[i] - xTarget[i], yPos[i] - yTarget[i]);
        }
        double average = sum / count;
        double span = Config.CANVAS * 0.5;
        return Math.max(0.0, Math.min(1.0, 1.0 - (average / span)));
    }

    public void step() {
        if (settled || count == 0) {
            return;
        }

        double distanceSum = 0;
        double limit = Config.CANVAS;

        for (int i = 0; i < count; i++) {
            double dx = xTarget[i] - xPos[i];
            double dy = yTarget[i] - yPos[i];

            xVel[i] += dx * Config.TARGET_ATTRACTION * dt;
            yVel[i] += dy * Config.TARGET_ATTRACTION * dt;

            xPos[i] += xVel[i] * dt;
            yPos[i] += yVel[i] * dt;

            xVel[i] *= Config.DAMPING;
            yVel[i] *= Config.DAMPING;

            if (xPos[i] < 0) xPos[i] = 0;
            if (yPos[i] < 0) yPos[i] = 0;
            if (xPos[i] > limit) xPos[i] = limit;
            if (yPos[i] > limit) yPos[i] = limit;

            distanceSum += Math.abs(dx) + Math.abs(dy);
        }

        if (distanceSum <= Config.MAX_AVERAGE_DISTANCE * count) {
            settled = true;
        }
    }

    /** Snaps every pixel onto its target, used when a round ends early. */
    public void finish() {
        for (int i = 0; i < count; i++) {
            xPos[i] = xTarget[i];
            yPos[i] = yTarget[i];
            xVel[i] = 0;
            yVel[i] = 0;
        }
        settled = true;
    }

    /**
     * Paints the current frame into a raw pixel buffer.
     *
     * @param buffer     row-major ARGB-ignored int buffer of size width * height
     * @param width      buffer width
     * @param height     buffer height
     * @param background fill colour, format 0xRRGGBB
     * @param dotSize    1 draws single pixels, 2 draws a 3x3 block, and so on
     */
    public void render(int[] buffer, int width, int height, int background, int dotSize) {
        java.util.Arrays.fill(buffer, background);

        double scaleX = (double) width / Config.CANVAS;
        double scaleY = (double) height / Config.CANVAS;
        int spread = dotSize - 1;

        for (int i = 0; i < count; i++) {
            int px = (int) (xPos[i] * scaleX);
            int py = (int) (yPos[i] * scaleY);
            int rgb = colour[i];

            for (int ox = -spread; ox <= spread; ox++) {
                int x = px + ox;
                if (x < 0 || x >= width) {
                    continue;
                }
                for (int oy = -spread; oy <= spread; oy++) {
                    int y = py + oy;
                    if (y < 0 || y >= height) {
                        continue;
                    }
                    buffer[x + y * width] = rgb;
                }
            }
        }
    }
}
