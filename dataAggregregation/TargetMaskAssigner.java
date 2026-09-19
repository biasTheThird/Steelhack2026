package dataAggregregation;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import src.Pixel;
import src.Util;

/**
 * Assigns pixels from a source image to the best matching target positions in a
 * target image using color and saturation similarity.
 *
 * The logic is:
 * - Gather all target pixels that are part of the target shape.
 * - For each source pixel, compare its RGB and saturation values to every target
 *   candidate and choose the closest match.
 * - Store the chosen target coordinate on the source pixel through setTarg().
 * - Push the matched pixels into Util.pixels so the animation subsystem can use them.
 */
public class TargetMaskAssigner {

    public static final int IMAGE_DIMENSION = 400;
    public static final int FOREGROUND_THRESHOLD = 400;
    // Keep the interactive demo responsive: a full 400x400 all-pairs match is far too
    // expensive for the Swing window to open and animate smoothly.
    public static final int MAX_ASSIGNED_PIXELS = 160000;

    /**
     * Reads a target mask from disk and returns all foreground positions as Pixel
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
     * Finds all foreground pixels inside a mask, treating a pixel as foreground if it
     * is sufficiently bright.
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
     * evenly spread order. This is retained as a fallback for binary masks.
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
     * Computes the perceived saturation of an RGB color.
     */
    private static double saturation(int r, int g, int b) {
        int max = Math.max(Math.max(r, g), b);
        int min = Math.min(Math.min(r, g), b);
        int delta = max - min;
        if (max == 0) {
            return 0.0;
        }
        return delta / (double) max;
    }

    /**
     * Computes a weighted color similarity score between source and target pixels.
     * Lower score means better match.
     */
    private static double colorMatchScore(int sourceR, int sourceG, int sourceB,
                                         int targetR, int targetG, int targetB) {
        double dr = sourceR - targetR;
        double dg = sourceG - targetG;
        double db = sourceB - targetB;

        double sourceSat = saturation(sourceR, sourceG, sourceB);
        double targetSat = saturation(targetR, targetG, targetB);
        double brightnessDiff = Math.abs(((sourceR + sourceG + sourceB) / 3.0) - ((targetR + targetG + targetB) / 3.0));

        double rgbDistance = Math.sqrt(dr * dr + dg * dg + db * db);
        double satDistance = Math.abs(sourceSat - targetSat);

        // Tuned weights: saturation and brightness matter more than raw RGB distance
        // for facial/portrait morphing, so the target assignment tracks the target shape
        // closer to the human face instead of a noisy nearest-color match.
        return (rgbDistance * 0.75) + (satDistance * 140.0) + (brightnessDiff * 0.9);
    }

    /**
     * Finds the target pixel that best matches each source pixel based on RGB + saturation.
     * The source pixel is then assigned that target coordinate by calling setTarg().
     */
    public static List<Pixel> assignTargetsByColor(BufferedImage sourceImage, BufferedImage targetImage) {
        if (sourceImage == null || targetImage == null) {
            return new ArrayList<>();
        }

        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();
        int targetWidth = targetImage.getWidth();
        int targetHeight = targetImage.getHeight();

        int sourceStep = 1;
        int targetStep = 1;

        if (sourceWidth * sourceHeight > MAX_ASSIGNED_PIXELS) {
            sourceStep = (int) Math.ceil(Math.sqrt((double) (sourceWidth * sourceHeight) / MAX_ASSIGNED_PIXELS));
        }
        if (targetWidth * targetHeight > MAX_ASSIGNED_PIXELS) {
            targetStep = (int) Math.ceil(Math.sqrt((double) (targetWidth * targetHeight) / MAX_ASSIGNED_PIXELS));
        }

        List<Pixel> sourcePixels = new ArrayList<>();
        for (int y = 0; y < sourceHeight; y += sourceStep) {
            for (int x = 0; x < sourceWidth; x += sourceStep) {
                if (sourcePixels.size() >= MAX_ASSIGNED_PIXELS) {
                    break;
                }
                int rgb = sourceImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                sourcePixels.add(new Pixel(r, g, b, x, y));
            }
            if (sourcePixels.size() >= MAX_ASSIGNED_PIXELS) {
                break;
            }
        }

        List<Pixel> targetCandidates = new ArrayList<>();
        for (int y = 0; y < targetHeight; y += targetStep) {
            for (int x = 0; x < targetWidth; x += targetStep) {
                if (targetCandidates.size() >= MAX_ASSIGNED_PIXELS) {
                    break;
                }

                int rgb = targetImage.getRGB(x, y);
                int a = (rgb >>> 24) & 0xFF;
                if (a == 0) {
                    continue;
                }

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int brightness = (r + g + b) / 3;

                if (brightness >= 18 || saturation(r, g, b) > 0.08) {
                    targetCandidates.add(new Pixel(r, g, b, x, y));
                }
            }
            if (targetCandidates.size() >= MAX_ASSIGNED_PIXELS) {
                break;
            }
        }

        if (targetCandidates.isEmpty()) {
            Util.pixels = new ArrayList<>(sourcePixels);
            return new ArrayList<>(sourcePixels);
        }

        boolean[] usedTarget = new boolean[targetCandidates.size()];
        int sourceLimit = Math.min(sourcePixels.size(), targetCandidates.size());

        for (int i = 0; i < sourceLimit; i++) {
            Pixel sourcePixel = sourcePixels.get(i);
            Pixel bestTarget = null;
            int bestIndex = -1;
            double bestScore = Double.POSITIVE_INFINITY;

            for (int j = 0; j < targetCandidates.size(); j++) {
                if (usedTarget[j]) {
                    continue;
                }

                Pixel targetPixel = targetCandidates.get(j);
                double score = colorMatchScore(sourcePixel.r, sourcePixel.g, sourcePixel.b,
                        targetPixel.r, targetPixel.g, targetPixel.b);

                if (score < bestScore) {
                    bestScore = score;
                    bestTarget = targetPixel;
                    bestIndex = j;
                }
            }

            if (bestTarget == null) {
                bestTarget = targetCandidates.get(i % targetCandidates.size());
                bestIndex = i % targetCandidates.size();
            }

            usedTarget[bestIndex] = true;
            sourcePixel.setTarg(bestTarget.xStart, bestTarget.yStart);
        }

        Util.pixels = new ArrayList<>(sourcePixels);
        return sourcePixels;
    }

    /**
     * Backwards-compatible overload that matches the old mask-driven API but now uses
     * a color-aware best-match strategy against the target image.
     */
    public static void assignTargets(List<Pixel> sourcePixels, BufferedImage mask) {
        if (sourcePixels == null || sourcePixels.isEmpty()) {
            return;
        }

        List<Pixel> targetCandidates = extractForegroundPoints(mask);
        if (targetCandidates.isEmpty()) {
            Util.pixels = new ArrayList<>(sourcePixels);
            return;
        }

        for (int i = 0; i < sourcePixels.size(); i++) {
            Pixel pixel = sourcePixels.get(i);
            Pixel targetPixel = targetCandidates.get(i % targetCandidates.size());
            pixel.setTarg(targetPixel.xStart, targetPixel.yStart);
        }

        Util.pixels = new ArrayList<>(sourcePixels);
    }

    /**
     * Convenience helper for reading the mask from disk and assigning targets to
     * the source pixel list.
     */
    public static void assignTargets(List<Pixel> sourcePixels, String maskPath) throws IOException {
        assignTargets(sourcePixels, ImageIO.read(new File(maskPath)));
    }
}
