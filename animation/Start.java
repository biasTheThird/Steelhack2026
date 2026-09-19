package animation;

import src.*;
import static src.Util.*;

public class Start {

    public static void start() {
        while(!isCompleted()) {
            updatePixels();
            g.updateVisual();
        }
    }

    static boolean isCompleted() {
        double distSum = 0;
        for(Pixel p : pixels) distSum += Math.hypot(p.xPos - p.xTarg, p.yPos - p.yTarg);
        return distSum <= maxAveDist * pixels.size();
    }

    static void updatePixels() {

        for(Pixel p : pixels) {

            double xForce = (p.xPos - p.xTarg) * targAttraction;
            double yForce = (p.yPos - p.yTarg) * targAttraction;

            for(Pixel p2 : pixels) {
                double dist = Math.hypot((p.yPos - p2.yPos), (p.xPos - p2.xPos)) / repulsion;
                xForce += (p.xPos - p2.xPos) / dist;
                yForce += (p.yPos - p2.yPos) / dist;
            }


            p.xPos += p.xVel * dt;
            p.yPos += p.yVel * dt;
            p.xVel += xForce * dt;
            p.yVel += yForce * dt;

            //boundary checks
            if(p.xPos < 0) p.xPos = 0;
            if(p.yPos < 0) p.yPos = 0;
            if(p.xPos > picWidth) p.xPos = picWidth;
            if(p.yPos > picHeight) p.yPos = picHeight;
        }
    }
}
