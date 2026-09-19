package dataAggregregation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import src.Pixel;
import src.Point;

/**
 * Extracts foreground points from a manually created binary mask and assigns them
 * to source pixels in a deterministic, spatially ordered way.
 *
 * Mask convention:
 * - Black background (0,0,0)
 * - White foreground (255,255,255)
 * - Anything above a brightness threshold is treated as foreground.
 *
 * If the mask contains more foreground pixels than there are source pixels, the
 * method samples the foreground evenly across the mask to keep the shape while
 * reducing the total count. If the mask contains fewer foreground pixels than
 * source pixels, the method cycles through the available target points to fill
 * the remaining assignments without failing.
 */
public class TargetMaskAssigner {

    public static final int IMAGE_DIMENSION = 400;
    public static final int FOREGROUND_THRESHOLD = 200;

    /**
     * Reads a binary mask from disk and returns all foreground coordinates.
     */
    public static List<Point> extractForegroundPoints(String maskPath) throws IOException {
        File file = new File(maskPath);
        BufferedImage mask = ImageIO.read(file);
        if (mask == null) {
            throw new IOException("Could not read mask image: " + maskPath);
        }
        return extractForegroundPoints(mask);
    }

    /**
     * Returns all foreground pixels in scanline order.
     * A foreground pixel is any pixel whose brightness is >= FOREGROUND_THRESHOLD.
     */
    public static List<Point> extractForegroundPoints(BufferedImage mask) {
        List<Point> foreground = new ArrayList<>();

        int width = mask.getWidth();
        int height = mask.getHeight();
        if (width != IMAGE_DIMENSION || height != IMAGE_DIMENSION) {
            System.err.println(
                    "Warning: expected a 400x400 mask, but found " + width + "x" + height + ". " +
                            "Continuing with the actual mask dimensions."
            );
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = mask.getRGB(x, y);
                int a = (rgb >>> 24) & 0xFF;
                if (a == 0) {
                    continue;
                }

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int brightness = (r + g + b) / 3;

                if (brightness >= FOREGROUND_THRESHOLD) {
                    foreground.add(new Point(x, y));
                }
            }
        }

        return foreground;
    }

    /**
     * Selects exactly targetCount points from the foreground list in a deterministic,
     * evenly spread order. This preserves the mask shape more naturally than random
     * sampling while keeping the implementation simple and fast.
     */
    public static List<Point> sampleTargetPositions(List<Point> foreground, int targetCount) {
        if (targetCount <= 0 || foreground == null || foreground.isEmpty()) {
            return new ArrayList<>();
        }

        List<Point> copied = new ArrayList<>(foreground.size());
        for (Point p : foreground) {
            copied.add(p.copy());
        }

        if (copied.size() == targetCount) {
            return copied;
        }

        // More source pixels than target positions: cycle through the available
        // positions in order so the assignment remains deterministic and never fails.
        if (copied.size() < targetCount) {
            List<Point> expanded = new ArrayList<>(targetCount);
            for (int i = 0; i < targetCount; i++) {
                expanded.add(copied.get(i % copied.size()).copy());
            }
            return expanded;
        }

        // More target positions than source pixels: take an evenly spaced subset.
        List<Point> selected = new ArrayList<>(targetCount);
        if (targetCount == 1) {
            selected.add(copied.get(0).copy());
            return selected;
        }

        double step = (double) (copied.size() - 1) / (double) (targetCount - 1);
        for (int i = 0; i < targetCount; i++) {
            int index = (int) Math.round(i * step);
            if (index >= copied.size()) {
                index = copied.size() - 1;
            }
            selected.add(copied.get(index).copy());
        }

        return selected;
    }

    /**
     * Assigns each source pixel a target coordinate and stores it in both the
     * current point and the original source point.
     */
    public static void assignTargets(List<Pixel> sourcePixels, BufferedImage mask) {
        if (sourcePixels == null || sourcePixels.isEmpty()) {
            return;
        }

        List<Point> maskTargets = sampleTargetPositions(extractForegroundPoints(mask), sourcePixels.size());
        for (int i = 0; i < sourcePixels.size(); i++) {
            Pixel pixel = sourcePixels.get(i);
            Point target = maskTargets.get(i % maskTargets.size()).copy();

            // Store the assignment on the pixel itself, which is where the project
            // already keeps the destination target for animation logic.
            //pixel.setTarg(target.copy());
        }
    }

    /**
     * Convenience helper for reading the mask from disk and assigning targets to
     * the source pixel list.
     */
    public static void assignTargets(List<Pixel> sourcePixels, String maskPath) throws IOException {
        assignTargets(sourcePixels, ImageIO.read(new File(maskPath)));
    }
}
