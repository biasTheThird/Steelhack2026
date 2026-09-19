package dataAggregregation;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import src.Pixel;

public class DefinePixels {

    private BufferedImage img;
    private int height;
    private int width;
    public static ArrayList<Pixel> pixels = new ArrayList<>();

    public DefinePixels(BufferedImage img) {
        if (img == null) {
            System.err.println("Error: null image passed to DefinePixels.");
            return; // bail out before touching width/height/pixels
        }

        pixels.clear();
        this.img = img;
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

    public ArrayList<Pixel> getPixels() {
        return pixels;
    }
}