import dataAggregregation.DefinePixels;
import src.*;
import static src.Util.g;

import java.util.List;

public class Main {
    
    public static void main(String[] args) {
        g = new Grapher();
        DefinePixels definer = new DefinePixels("homer.jpg");
        g.setWinDims(new Point(definer.getHeight(), definer.getWidth()));
    }

    public void fillHomer(Grapher g) {
        DefinePixels definer = new DefinePixels("homer.jpg");
        List<Pixel> pixelList = definer.getPixels();
        for(int y = 0; y< definer.getHeight(); y++) {
            for(int x = 0; x< definer.getHeight(); x++) {

            }
        }

    }
}
