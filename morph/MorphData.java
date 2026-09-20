package morph;

import java.io.Serializable;

/**
 * One round's worth of pixel assignment, flattened into primitive arrays so it
 * can cross the network cheaply. Around 40000 pixels works out to roughly
 * 450 KB, which is fine on a LAN and survives a home connection.
 *
 * Colours are stored as bytes and must be read back with {@code & 0xFF}.
 * Coordinates are stored as shorts because the field is only 400 units wide.
 */
public final class MorphData implements Serializable {

    private static final long serialVersionUID = 1L;

    public final int count;
    public final byte[] r;
    public final byte[] g;
    public final byte[] b;
    public final short[] xStart;
    public final short[] yStart;
    public final short[] xTarget;
    public final short[] yTarget;

    public MorphData(int count, byte[] r, byte[] g, byte[] b,
                     short[] xStart, short[] yStart,
                     short[] xTarget, short[] yTarget) {
        this.count = count;
        this.r = r;
        this.g = g;
        this.b = b;
        this.xStart = xStart;
        this.yStart = yStart;
        this.xTarget = xTarget;
        this.yTarget = yTarget;
    }

    public int red(int i) {
        return r[i] & 0xFF;
    }

    public int green(int i) {
        return g[i] & 0xFF;
    }

    public int blue(int i) {
        return b[i] & 0xFF;
    }
}
