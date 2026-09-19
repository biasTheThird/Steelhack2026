package animation;

import src.*;
import static src.Util.*;

import java.util.ArrayList;
import java.util.List;

public class Start2 {

    static int n = 0;

    // Larger = fewer chunks, more pixels checked per chunk
    static final int CHUNK_SIZE = 5;
    static final int CHUNKS_X = (int) Math.ceil((double) picWidth / CHUNK_SIZE);
    static final int CHUNKS_Y = (int) Math.ceil((double) picHeight / CHUNK_SIZE);
    static final List<Pixel>[][] chunks = createChunks();

    @SuppressWarnings("unchecked")
    static List<Pixel>[][] createChunks() {
        List<Pixel>[][] result = new ArrayList[CHUNKS_X][CHUNKS_Y];

        for (int x = 0; x < CHUNKS_X; x++) {
            for (int y = 0; y < CHUNKS_Y; y++) {
                result[x][y] = new ArrayList<>();
            }
        }

        return result;
    }

    public static void start() {
        while(!isCompleted()) {
            System.out.println(n++);
            updateChunks();
            updatePixels();
            g.updateVisual();
        }
    }

    static boolean isCompleted() {
        double distSum = 0;

        for (Pixel p : pixels) {
            distSum += Math.hypot(
                    p.xPos - p.xTarg,
                    p.yPos - p.yTarg
            );
        }

        return distSum <= maxAveDist * pixels.size();
    }

    static void updateChunks() {

        // Clear old chunks
        for (int x = 0; x < CHUNKS_X; x++) {
            for (int y = 0; y < CHUNKS_Y; y++) {
                chunks[x][y].clear();
            }
        }

        // Put every pixel into its current chunk
        for (Pixel p : pixels) {
            int cx = (int) (p.xPos / CHUNK_SIZE);
            int cy = (int) (p.yPos / CHUNK_SIZE);

            // Safety against boundary rounding
            cx = Math.clamp(cx, 0, CHUNKS_X - 1);
            cy = Math.clamp(cy, 0, CHUNKS_Y - 1);

            chunks[cx][cy].add(p);
        }
    }

    static void updatePixels() {

        for (Pixel p : pixels) {

            double xForce = (p.xPos - p.xTarg) * targAttraction;
            double yForce = (p.yPos - p.yTarg) * targAttraction;

            int cx = (int) (p.xPos / CHUNK_SIZE);
            int cy = (int) (p.yPos / CHUNK_SIZE);

            cx = Math.clamp(cx, 0, CHUNKS_X - 1);
            cy = Math.clamp(cy, 0, CHUNKS_Y - 1);

            //Check this chunk and neighboring chunks.
            for (int x = cx - 1; x <= cx + 1; x++) {
                if (x < 0 || x >= CHUNKS_X) continue;

                for (int y = cy - 1; y <= cy + 1; y++) {
                    if (y < 0 || y >= CHUNKS_Y)continue;

                    for (Pixel p2 : chunks[x][y]) {
                        if (p == p2) continue;

                        double dx = p.xPos - p2.xPos;
                        double dy = p.yPos - p2.yPos;

                        double dist = Math.hypot(dx, dy);

                        if (dist < 0.001) continue;

                        dist /= repulsion;

                        xForce += dx / dist;
                        yForce += dy / dist;
                    }
                }
            }

            p.xPos += p.xVel * dt;
            p.yPos += p.yVel * dt;

            p.xVel += xForce * dt;
            p.yVel += yForce * dt;

            //Boundary checks
            if (p.xPos < 0) p.xPos = 0;
            if (p.yPos < 0) p.yPos = 0;
            if (p.xPos > picWidth) p.xPos = picWidth;
            if (p.yPos > picHeight) p.yPos = picHeight;
        }
    }
}