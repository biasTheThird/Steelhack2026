package morph;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import game.Config;

/**
 * Puts every image on the same footing: scaled to fit inside a CANVAS x CANVAS
 * square, centred, with the leftover margin fully transparent.
 *
 * The transparent margin matters. Both the assignment and the renderer treat an
 * alpha of zero as "not part of the picture", so a portrait does not drag a
 * rectangle of black pixels around with it.
 */
public final class ImageField {

    private ImageField() {
    }

    public static BufferedImage load(File file) throws IOException {
        BufferedImage raw = ImageIO.read(file);
        if (raw == null) {
            throw new IOException("Unsupported or unreadable image: " + file.getPath());
        }
        return normalise(raw);
    }

    public static BufferedImage load(byte[] bytes) throws IOException {
        BufferedImage raw = ImageIO.read(new ByteArrayInputStream(bytes));
        if (raw == null) {
            throw new IOException("Unsupported or unreadable image data");
        }
        return normalise(raw);
    }

    public static BufferedImage normalise(BufferedImage raw) {
        int side = Config.CANVAS;
        double scale = Math.min(
                (double) side / raw.getWidth(),
                (double) side / raw.getHeight()
        );

        int width = Math.max(1, (int) Math.round(raw.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(raw.getHeight() * scale));
        int offsetX = (side - width) / 2;
        int offsetY = (side - height) / 2;

        BufferedImage field = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gfx = field.createGraphics();
        gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gfx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gfx.drawImage(raw, offsetX, offsetY, width, height, null);
        gfx.dispose();

        return field;
    }

    /** Small preview used for the drawer's three choices. */
    public static byte[] thumbnail(BufferedImage image, int side) throws IOException {
        double scale = Math.min((double) side / image.getWidth(), (double) side / image.getHeight());
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));

        BufferedImage thumb = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gfx = thumb.createGraphics();
        gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gfx.drawImage(image, 0, 0, width, height, null);
        gfx.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(thumb, "png", out);
        return out.toByteArray();
    }
}
