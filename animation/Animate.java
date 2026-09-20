package animation;

import src.*;
import java.util.ArrayList;
import static src.Util.*;

public class Animate {

    // Larger = fewer chunks, more pixels checked per chunk
    final int CHUNK_SIZE = 5;
    final int CHUNKS_X = (int) Math.ceil((double) picWidth / CHUNK_SIZE);
    final int CHUNKS_Y = (int) Math.ceil((double) picHeight / CHUNK_SIZE);
    ArrayList<Pixel>[][] chunks;
    
    public Animate() {
        if(pixels == null ||
                pixels.isEmpty() ||
                g == null
        ) throw new RuntimeException("Something was null");
    }

    public void start(boolean pixelsRepel) {

        if(pixelsRepel) {
            chunks = new ArrayList[CHUNKS_X][CHUNKS_Y];
            for (int x = 0; x < CHUNKS_X; x++) {
                for (int y = 0; y < CHUNKS_Y; y++) {
                    chunks[x][y] = new ArrayList<>();
                }
            }

            while(isNotCompleted()) {
                updateChunks();
                updatePixelsRepel();
                g.updateVisual();
                if(dt < dtMax) dt += dtStep;
            }
        } else {
            while(isNotCompleted()) {
                updatePixels();
                g.updateVisual();
                if(dt < dtMax) dt += dtStep;
            }
        }
    }

    boolean isNotCompleted() {
        double distSum = 0;

        for(Pixel p : pixels) distSum +=
                (p.xPos - p.xTarg) * (p.xPos - p.xTarg) + (p.yPos - p.yTarg) * (p.yPos - p.yTarg);

        return distSum > maxAveDist * maxAveDist * pixels.size();
    }

    void updatePixels() {

        if(pixels == null || pixels.isEmpty()) return;

        for(Pixel p : pixels) {

            p.xPos += p.xVel * dt;
            p.yPos += p.yVel * dt;

            p.xVel *= velDecay;
            p.yVel *= velDecay;
            
            p.xVel += (p.xTarg - p.xPos) * (p.xTarg - p.xPos) * targAttraction * dt;
            p.yVel += (p.yTarg - p.yPos) * (p.yTarg - p.yPos) * targAttraction * dt;

            constrainPosition(p);
        }
    }

    void updatePixelsRepel() {
        if(pixels == null || pixels.isEmpty()) return;

        for(Pixel p : pixels) {

            double xForce = (p.xTarg - p.xPos) * targAttraction;
            double yForce = (p.yTarg - p.yPos) * targAttraction;

            int cx = (int) (p.xPos / CHUNK_SIZE);
            int cy = (int) (p.yPos / CHUNK_SIZE);

            cx = Math.clamp(cx, 0, CHUNKS_X - 1);
            cy = Math.clamp(cy, 0, CHUNKS_Y - 1);

            // Check this chunk and all neighboring chunks.
            for(int x = cx - 1; x <= cx + 1; x++) {
                if(x < 0 || x >= CHUNKS_X) continue;

                for(int y = cy - 1; y <= cy + 1; y++) {
                    if(y < 0 || y >= CHUNKS_Y) continue;

                    for(Pixel p2 : chunks[x][y]) {
                        if(p == p2) continue;

                        double dx = p.xPos - p2.xPos;
                        double dy = p.yPos - p2.yPos;
                        double distSq = dx*dx + dy*dy;

                        if(distSq < 0.000001) continue;

                        double invDist = 1.0 / Math.sqrt(distSq);

                        double targDist = (p.xPos - p.xTarg)*(p.xPos - p.xTarg) + (p.yPos - p.yTarg)*(p.yPos - p.yTarg);
                        targDist /= repulsion;

                        xForce += dx * invDist * targDist;
                        yForce += dy * invDist * targDist;
                    }
                }
            }

            p.xPos += p.xVel * dt;
            p.yPos += p.yVel * dt;

            p.xVel *= velDecay;
            p.yVel *= velDecay;

            p.xVel += xForce * dt;
            p.yVel += yForce * dt;

            constrainPosition(p);
        }
    }

    void updateChunks() {
        // Clear old chunks
        for(int x = 0; x < CHUNKS_X; x++) {
            for(int y = 0; y < CHUNKS_Y; y++) {
                chunks[x][y].clear();
            }
        }

        // Put every pixel into its current chunk
        for(Pixel p : pixels) {
            chunks
                [Math.clamp((int) (p.xPos / CHUNK_SIZE), 0, CHUNKS_X - 1)]
                [Math.clamp((int) (p.yPos / CHUNK_SIZE), 0, CHUNKS_Y - 1)]
                .add(p);
        }
    }

    void constrainPosition(Pixel p) {
        //lower bounds
        if(p.xPos < 0) p.xPos = 0;
        if(p.yPos < 0) p.yPos = 0;
        //upper bounds
        if(p.xPos >= picWidth) p.xPos = picWidth-1;
        if(p.yPos >= picHeight) p.yPos = picHeight-1;
    }
}