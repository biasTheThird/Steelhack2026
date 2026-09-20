import animation.*;
import dataAggregregation.DefinePixels;
import dataAggregregation.FixDimensions;
import dataAggregregation.TargetMaskAssigner;
import src.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static src.Util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        BufferedImage sourceImage = FixDimensions.resize("./SourceImages/ritiii.jpg");
        if (sourceImage == null) {
            throw new IllegalStateException("Could not load source image: ./SourceImages/homer.jpg");
        }

        DefinePixels sourceLoader = new DefinePixels(sourceImage);
        List<Pixel> sourcePixels = sourceLoader.getPixels();

        BufferedImage targetImage = FixDimensions.resize("./TargetImage/Cat.jpg");
        if (targetImage == null) {
            throw new IllegalStateException("Could not load target image: ./TargetImage/ritiii.jpg");
        }

        List<Pixel> matchedPixels = TargetMaskAssigner.assignTargetsByColor(sourceImage, targetImage);
        pixels = new ArrayList<>(matchedPixels);

        System.out.println("Assigned " + pixels.size() + " source pixels to target positions.");
        System.out.println("Target image: ./TargetImage/ritiii.jpg");
        if (!matchedPixels.isEmpty()) {
            System.out.println("Sample target: " + matchedPixels.get(0).xTarg + ", " + matchedPixels.get(0).yTarg);
        }

        g = new Grapher();
        new Animate().start(true);
    }
}
