package animation;

import javax.swing.*;
import src.*;

import static src.Util.*;

public class Start {

    public static void start(boolean pixelsRepel) {
        SwingUtilities.invokeLater(() -> {
            Timer timer = new Timer(16, e -> {
                if (isCompleted()) {
                    ((Timer) e.getSource()).stop();
                    return;
                }

                updatePixels();
                if (g != null) {
                    g.updateVisual();
                }
            });
            timer.setInitialDelay(0);
            timer.start();
        });
    }

    static boolean isCompleted() {
        if (pixels == null || pixels.isEmpty()) {
            return true;
        }

        double distSum = 0;
        for (Pixel p : pixels) {
            distSum += Math.hypot(p.xPos - p.xTarg, p.yPos - p.yTarg);
        }
        return distSum <= maxAveDist * pixels.size();
    }

    static void updatePixels() {
        if (pixels == null || pixels.isEmpty()) {
            return;
        }

        for (Pixel p : pixels) {
            double dx = p.xTarg - p.xPos;
            double dy = p.yTarg - p.yPos;

            double xForce = dx * targAttraction;
            double yForce = dy * targAttraction;

            p.xVel += xForce * dt;
            p.yVel += yForce * dt;

            p.xPos += p.xVel * dt;
            p.yPos += p.yVel * dt;

            p.xVel *= 0.92;
            p.yVel *= 0.92;

            if (p.xPos < 0) p.xPos = 0;
            if (p.yPos < 0) p.yPos = 0;
            if (p.xPos > picWidth) p.xPos = picWidth;
            if (p.yPos > picHeight) p.yPos = picHeight;
        }
    }
}
