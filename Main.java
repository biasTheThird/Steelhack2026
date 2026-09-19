import dataAggregregation.*;
import animation.*;
import src.*;
import src.FixDimensions;

import static animation.Start2.start;
import static src.Util.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws Exception {
        g = new Grapher();
        g = g.setWinDims(new Point(picWidth, picHeight));

        BufferedImage sourceImage = FixDimensions.resize("./SourceImages/homer.jpg");
        DefinePixels sourceLoader = new DefinePixels(sourceImage);
        ArrayList<Pixel> sourcePixels = sourceLoader.getPixels();

        BufferedImage targetMask = ImageIO.read(new File("./TargetImage/6aaede0902432_download-modified.jpg"));
        TargetMaskAssigner.assignTargets(sourcePixels, targetMask);

        pixels = sourcePixels;
        start();
    }
}
