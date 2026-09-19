import dataAggregregation.*;
import src.*;
import src.FixDimensions;

import static src.Util.*;

import java.awt.image.BufferedImage;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        g = new Grapher();
        g = g.setWinDims(new Point(400, 400));
        g.setTitle("Now You See Me");



        DefinePixels definer = new DefinePixels(FixDimensions.resize(homerPath));
        g.updateVisual();
    }


}
