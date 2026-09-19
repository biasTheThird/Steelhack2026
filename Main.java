import dataAggregregation.*;
import src.*;

import static src.Util.*;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        g = new Grapher();
        g = g.setWinDims(new Point(400, 400));
        g.setTitle("Now You See Me");

        DefinePixels definer = new DefinePixels(homerPath);
        g.updateVisual();
    }

    public void fillHomer() {
        DefinePixels definer = new DefinePixels(homerPath);
        List<Pixel> pixelList = definer.getPixels();
        for(int y = 0; y < definer.getHeight(); y++) {
            for(int x = 0; x < definer.getHeight(); x++) {

            }
        }

    }
}
