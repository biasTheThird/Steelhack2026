package dataAggregregation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import src.Pixel;

public class DefinePixels {

    private BufferedImage img;
    public static int height;
    public static int width;
    public static List<Pixel> pixels = new ArrayList<>();

    public DefinePixels(String path) {
        try {
            img = ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("Error: Could not find or read '" + path + "'.");
            e.printStackTrace();
        }

        if (img == null) {
            System.err.println("Error: The image file format is not supported, or file was not found.");
            return; // bail out before touching width/height/pixels
        }

        height = img.getHeight();
        width = img.getWidth();
        extractPixels();
    }

    private void extractPixels() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Pixel's constructor is (r, g, b, xStart, yStart) - order matters here
                pixels.add(new Pixel(r, g, b, x, y));
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public BufferedImage getImg() {
        return img;
    }

    public List<Pixel> getPixels() {
        return pixels;
    }
}