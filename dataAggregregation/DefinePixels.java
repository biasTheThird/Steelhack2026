package dataAggregregation;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DefinePixels {
    BufferedImage img = ImageIO.read(new File("homer.jpg"));
    int height = img.getHeight();
    int width = img.getWidth();

}
