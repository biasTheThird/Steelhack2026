package animation;

import src.Pixel;
import src.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Arrays;

import static dataAggregregation.DefinePixels.pixels;

/**
 * Each instance of this class can open a window that can display data
 */
public class Grapher {

    private BufferedImage image;
    private JFrame frame;
    private Canvas canvas;
    private BufferStrategy bs;

    private int pixelH;
    private int pixelW;
    private int[] pixel;

    //sets default graphing values
    private Point lowerBounds = new Point(0, 0);
    private Point upperBounds = new Point(400, 400);
    private Dimension winDims = new Dimension(400, 400);
    private int backgroundColor = Util.BLACK;
    private int pointSize = 1;


    /**
     * Creates a window to plot data from <b>dataList</b> on. The graph is bounded to [<b>lowerBounds</b>,
     * b>upperBounds</b>], but will stretch to the size of the window. Colors are in hex with the format: 0xrrggbb.
     * Defaults:
     * <ul>
     *     <li> visibility: visible
     *     <li> window dimensions: 512x512 pixels
     *     <li> bounds: x and y to [-10, 10]
     *     <li> background: color black (0x101010)
     *     <li> axis: visible, color gray (0xcccccc), size thin (1)
     *     <li> point color: red (0xb4281e), size normal (2)
     * </ul>
     */
    public Grapher() {
        windowSetup();
        show();
        updateVisual();
    }

    private Grapher(Dimension winDims) {
        this.winDims = winDims;
        this.upperBounds =  new Point(winDims.width, winDims.height);
        windowSetup();
        show();
        updateVisual();
    }

    public String getTitle() {
        return frame.getTitle();
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }

    public int getPointSize() {
        return pointSize;
    }

    public void setPointSize(int pointSize) {
        this.pointSize = pointSize;
        updateVisual();
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        updateVisual();
    }

    public Point getUpperBounds() {
        return upperBounds;
    }

    public void setUpperBounds(Point upperBounds) {
        this.upperBounds = upperBounds;
        updateVisual();
    }

    public Point getLowerBounds() {
        return lowerBounds;
    }

    public void setLowerBounds(Point lowerBounds) {
        this.lowerBounds = lowerBounds;
        updateVisual();
    }

    public void setBounds(Point lowerBounds, Point upperBounds) {
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
        updateVisual();
    }

    public void visible(boolean visible) {
        frame.setVisible(visible);
    }
    public void show() {
        visible(true);
    }
    public void hide() {
        visible(false);
    }

    /**
     * Sets up the window by creating a BufferedImage, JFrame, Canvas, and BufferStrategy
     */
    private void windowSetup() {
        //sets the canvas to a new canvas
        canvas = new Canvas();
        canvas.setIgnoreRepaint(true);
        image = new BufferedImage(winDims.width, winDims.height, BufferedImage.TYPE_INT_RGB);
        canvas.setPreferredSize(winDims);
        canvas.setMaximumSize(winDims);
        canvas.setMinimumSize(winDims);
        pixelW = winDims.width;
        pixelH = winDims.height;
        pixel = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();

        //sets up the JFrame with the title from the GameContainer, sets the frame to the EXIT_ON_CLOSE operation
        frame = new JFrame();
        frame.setIgnoreRepaint(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        //sets the layout to a new BorderLayout, adds the canvas with the BorderLayout to the frame
        frame.setLayout(new BorderLayout());
        frame.add(canvas, BorderLayout.CENTER);
        //packs the frame to the canvas, sets frame to open in the middle of the screen
        frame.pack();
        frame.setLocationRelativeTo(null);
        //sets the frame to non-resizable, sets the frame to visible
        frame.setResizable(false);
        frame.setVisible(false);

        //creates two BufferStrategys for the canvas, used to tell the graphics how to draw the image
        canvas.createBufferStrategy(2);
        //sets the BufferStrategy to the one the canvas has
        bs = canvas.getBufferStrategy();

        setTitle("Untitled Grapher");
        updateVisual();
    }

    /**
     * Sets the pixel dimensions of the window
     * @see #setWinDims(Point)
     */
    public Grapher setWinDims(Dimension windowDims) {
        frame.dispose();
        return new Grapher(windowDims);
    }
    /**
     * Sets the pixel dimensions of the window
     * @see #setWinDims(Dimension)
     */
    public Grapher setWinDims(Point dims) {
        Grapher gOut = setWinDims(new Dimension((int)dims.getX(), (int)dims.getY()));
        gOut.updateVisual();
        return gOut;
    }
    public Point getWinDims() {
        return new Point(winDims.width, winDims.height);
    }

    /**
     * Sets one pixel's color at (<b>x</b>, <b>y</b>). These coordinates do not map
     * @param color the hex color of the axis. format: 0xrrggbb
     */
    void setPixel(double x, double y, int color, int size) {
        setPixelRaw(
                //maps the x and y values from the data bounds to the window bounds
                (int) Util.map(lowerBounds.getX(), upperBounds.getX(), 0, pixelW, x),
                (int) Util.map(upperBounds.getY(), lowerBounds.getY(), pixelH, 0, y),
                color, size
        );
    }

    /**
     * Sets one pixel's color at (<b>x</b>, <b>y</b>). These coordinates do not map
     * @param color the hex color of the axis. format: 0xrrggbb
     */
    void setPixelRaw(int x, int y, int color, int size) {
        for(int i = 1 - size; i <= size - 1; i++) {
            for(int j = 1 - size; j <= size - 1; j++) {
                if(x+i < 0 || x+i >= pixelW || y+j < 0 || y+j >= pixelH) continue;
                pixel[(x+i) + (y+j)*pixelW] = color;
            }
        }
    }

    /**
     * Updates the window's pixel data. Use after making visual changes
     */
    public void updateVisual() {
        //if either bounds range is 0, return
        if(lowerBounds.getX() == upperBounds.getX() || lowerBounds.getY() == upperBounds.getY()) return;
        //sets the background
        Arrays.fill(pixel, backgroundColor);
        //updates each pixel
        for(int i = 0; i < pixels.size(); i++) {
            Pixel p = pixels.get(i);
            setPixel(p.xPos, p.yPos, Util.rgb(p.r, p.g, p.b), pointSize);
        }
        //draws the pixels on the window
        Graphics graphics = bs.getDrawGraphics();
        graphics.drawImage(image, 0, 0, canvas.getWidth(), canvas.getHeight(), null);
        graphics.dispose();
        bs.show();
        Toolkit.getDefaultToolkit().sync();
    }

}
