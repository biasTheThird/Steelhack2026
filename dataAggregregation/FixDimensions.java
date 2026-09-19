package src;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class FixDimensions {

    private static final int MAX_DIMENSION = 400;

    /**
     * Loads the image at <b>path</b> and resizes it so its larger dimension
     * (width or height) equals MAX_DIMENSION, preserving aspect ratio.
     * Returns null if the file can't be found or read.
     */
    public static BufferedImage resize(String path) {
        BufferedImage img;
        try {
            img = ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("Error: Could not find or read '" + path + "'.");
            e.printStackTrace();
            return null;
        }

        if (img == null) {
            System.err.println("Error: The image file format is not supported: '" + path + "'.");
            return null;
        }

        int origWidth = img.getWidth();
        int origHeight = img.getHeight();

        double scale = (double) MAX_DIMENSION / Math.max(origWidth, origHeight);

        int newWidth = (int) Math.round(origWidth * scale);
        int newHeight = (int) Math.round(origHeight * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(img, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resized;
    }
}