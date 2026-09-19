import dataAggregregation.DefinePixels;
import dataAggregregation.TargetMaskAssigner;
import src.Pixel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        BufferedImage sourceImage = ImageIO.read(new File("./SourceImages/homer.jpg"));
        DefinePixels sourceLoader = new DefinePixels(sourceImage);
        List<Pixel> sourcePixels = sourceLoader.getPixels();

        BufferedImage targetMask = ImageIO.read(new File("./TargetImage/6aaede0902432_download-modified.jpg"));
        TargetMaskAssigner.assignTargets(sourcePixels, targetMask);

        for (int i = 0; i < Math.min(5, sourcePixels.size()); i++) {
            Pixel p = sourcePixels.get(i);
            System.out.println("pixel " + i + " -> targ = (" + p.xTarg + ", " + p.yTarg + ")");
        }
    }
}
