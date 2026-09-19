package dataAggregregation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import src.Pixel;

/**
 * Extracts foreground coordinates from a manually created binary mask and assigns
 * them to source pixels in a deterministic, spatially ordered way.
 *
 * Mask convention:
 * - Black background (0,0,0)
 * - White foreground (255,255,255)
 * - Anything above a brightness threshold is treated as foreground.
 *
 * If the mask contains more foreground pixels than there are source pixels, the
 * method samples the foreground evenly across the mask to keep the shape while
 * reducing the total count. If the mask contains fewer foreground pixels than
 * source pixels, the method cycles through the available target coordinates to
 * fill the remaining assignments without failing.
 */
public class TargetMaskAssigner {

    public static final int IMAGE_DIMENSION = 400;
    public static final int FOREGROUND_THRESHOLD = 200;

    /**
     * Reads a binary mask from disk and returns all foreground positions as Pixel
     * objects, using only the Pixel coordinate model.
     */
    public static List<Pixel> extractForegroundPoints(String maskPath) throws IOException {
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
    public static List<Pixel> extractForegroundPoints(BufferedImage mask) {
        List<Pixel> foreground = new ArrayList<>();

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
                    foreground.add(new Pixel(255, 255, 255, x, y));
                }
            }
        }

        return foreground;
    }

    /**
     * Selects exactly targetCount foreground pixels from the mask in a deterministic,
     * evenly spread order. This preserves the mask shape more naturally than random
     * sampling while keeping the implementation simple and fast.
     */
    public static List<Pixel> sampleTargetPositions(List<Pixel> foreground, int targetCount) {
        if (targetCount <= 0 || foreground == null || foreground.isEmpty()) {
            return new ArrayList<>();
        }

        List<Pixel> copied = new ArrayList<>(foreground.size());
        for (Pixel p : foreground) {
            copied.add(new Pixel(p.r, p.g, p.b, p.xStart, p.yStart));
        }

        if (copied.size() == targetCount) {
            return copied;
        }

        if (copied.size() < targetCount) {
            List<Pixel> expanded = new ArrayList<>(targetCount);
            for (int i = 0; i < targetCount; i++) {
                Pixel source = copied.get(i % copied.size());
                expanded.add(new Pixel(source.r, source.g, source.b, source.xStart, source.yStart));
            }
            return expanded;
        }

        List<Pixel> selected = new ArrayList<>(targetCount);
        if (targetCount == 1) {
            Pixel first = copied.get(0);
            selected.add(new Pixel(first.r, first.g, first.b, first.xStart, first.yStart));
            return selected;
        }

        double step = (double) (copied.size() - 1) / (double) (targetCount - 1);
        for (int i = 0; i < targetCount; i++) {
            int index = (int) Math.round(i * step);
            if (index >= copied.size()) {
                index = copied.size() - 1;
            }
            Pixel chosen = copied.get(index);
            selected.add(new Pixel(chosen.r, chosen.g, chosen.b, chosen.xStart, chosen.yStart));
        }

        return selected;
    }

    /**
     * Assigns each source pixel a target coordinate and stores it on the pixel
     * itself, matching the repo's existing pixel-based animation model.
     */
    public static void assignTargets(List<Pixel> sourcePixels, BufferedImage mask) {
        if (sourcePixels == null || sourcePixels.isEmpty()) {
            return;
        }

        List<Pixel> maskTargets = sampleTargetPositions(extractForegroundPoints(mask), sourcePixels.size());
        if (maskTargets.isEmpty()) {
            throw new IllegalArgumentException("Target mask contains no foreground pixels.");
        }

        for (int i = 0; i < sourcePixels.size(); i++) {
            Pixel pixel = sourcePixels.get(i);
            Pixel targetPixel = maskTargets.get(i);
            pixel.setTarg(targetPixel.xStart, targetPixel.yStart);
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
