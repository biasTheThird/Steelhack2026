import dataAggregregation.DefinePixels;
import src.*;
import static src.Util.g;

import java.util.List;

public class Main {
    
    public static void main(String[] args) {
        g = new Grapher();
        g.setWinDims(new Point(400, 400));
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
