package dataAggregregation;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import src.Pixel;

public class TargetMaskAssignerSmokeTest {
    public static void main(String[] args) {
        BufferedImage mask = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                mask.setRGB(x, y, 0xFF000000);
            }
        }

        int[][] foreground = {
                {1, 2},
                {4, 5},
                {7, 8}
        };

        for (int[] point : foreground) {
            mask.setRGB(point[0], point[1], 0xFFFFFFFF);
        }

        List<Pixel> sourcePixels = new ArrayList<>();
        sourcePixels.add(new Pixel(10, 20, 30, 100, 200));
        sourcePixels.add(new Pixel(40, 50, 60, 150, 250));
        sourcePixels.add(new Pixel(70, 80, 90, 200, 300));

        TargetMaskAssigner.assignTargets(sourcePixels, mask);

        for (int i = 0; i < foreground.length; i++) {
            Pixel pixel = sourcePixels.get(i);
            int expectedX = foreground[i][0];
            int expectedY = foreground[i][1];

            if (Math.abs(pixel.xTarg - expectedX) > 1e-9 || Math.abs(pixel.yTarg - expectedY) > 1e-9) {
                throw new AssertionError(
                        "Pixel " + i + " expected target (" + expectedX + ", " + expectedY + ") but got (" +
                                pixel.xTarg + ", " + pixel.yTarg + ")"
                );
            }
        }

        System.out.println("TargetMaskAssigner smoke test passed.");
    }
}
