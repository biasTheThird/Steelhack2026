package morph;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import game.Config;

/**
 * Pairs every source pixel with the target pixel whose colour it matches best,
 * then hands back the pairing as a {@link MorphData}.
 *
 * This is the same greedy nearest-colour assignment the original
 * TargetMaskAssigner did, and the same scoring weights, with three changes that
 * do not alter the result:
 *
 *   1. Primitive arrays instead of Pixel objects, so the inner loop stays in cache.
 *   2. Claimed targets are swap-removed from the candidate list instead of being
 *      skipped with a boolean check, which halves the total comparisons.
 *   3. The scan for each source pixel is split across cores.
 *
 * The one visible difference: when two targets score identically, swap-removal
 * can pick the other one. Ties are rare and the outcome looks the same.
 *
 * Cost is roughly n^2 / 2 scored pairs. At 40000 pixels a side that is about
 * 800 million comparisons, which is a few seconds spread over several cores.
 * Config.MAX_PIXELS is the dial if that feels too long.
 */
public final class MorphBuilder {

    /** Called with 0..100 as the assignment proceeds. */
    public interface Progress {
        void at(int percent);
    }

    /** Below this many remaining candidates, threading costs more than it saves. */
    private static final int PARALLEL_THRESHOLD = 8000;

    private MorphBuilder() {
    }

    public static MorphData build(BufferedImage source, BufferedImage target, Progress progress) {
        int stride = strideFor(Config.CANVAS * Config.CANVAS, Config.MAX_PIXELS);

        Field targets = sample(target, stride, true);
        Field sources = sample(source, stride, false);

        if (targets.n == 0 || sources.n == 0) {
            // Nothing usable to morph into. Park every pixel where it started so the
            // round still renders the source image instead of collapsing to a corner.
            return identity(sources);
        }

        if (sources.n > targets.n) {
            sources = thin(sources, targets.n);
        }

        int[] alive = new int[targets.n];
        for (int i = 0; i < targets.n; i++) {
            alive[i] = i;
        }
        int aliveCount = targets.n;

        short[] xTarget = new short[sources.n];
        short[] yTarget = new short[sources.n];

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = threads > 1 ? Executors.newFixedThreadPool(threads) : null;
        int lastReported = -1;

        try {
            for (int i = 0; i < sources.n; i++) {
                int slot = (pool != null && aliveCount >= PARALLEL_THRESHOLD)
                        ? bestParallel(sources, i, targets, alive, aliveCount, pool, threads)
                        : bestSerial(sources, i, targets, alive, 0, aliveCount).slot;

                int chosen = alive[slot];
                xTarget[i] = targets.x[chosen];
                yTarget[i] = targets.y[chosen];

                aliveCount--;
                alive[slot] = alive[aliveCount];

                if (progress != null) {
                    int percent = (int) ((i + 1) * 100L / sources.n);
                    if (percent != lastReported) {
                        lastReported = percent;
                        progress.at(percent);
                    }
                }
            }
        } finally {
            if (pool != null) {
                pool.shutdownNow();
            }
        }

        return new MorphData(sources.n,
                toBytes(sources.r), toBytes(sources.g), toBytes(sources.b),
                sources.x, sources.y, xTarget, yTarget);
    }

    /* ----- candidate search ----- */

    private static final class Hit {
        final int slot;
        final double score;

        Hit(int slot, double score) {
            this.slot = slot;
            this.score = score;
        }
    }

    private static Hit bestSerial(Field s, int i, Field t, int[] alive, int from, int to) {
        int sr = s.r[i];
        int sg = s.g[i];
        int sb = s.b[i];
        double ssat = s.sat[i];
        double sbri = s.bri[i];

        int bestSlot = from;
        double bestScore = Double.POSITIVE_INFINITY;

        for (int slot = from; slot < to; slot++) {
            int j = alive[slot];

            double dr = sr - t.r[j];
            double dg = sg - t.g[j];
            double db = sb - t.b[j];

            double score = Math.sqrt(dr * dr + dg * dg + db * db) * 0.75
                    + Math.abs(ssat - t.sat[j]) * 140.0
                    + Math.abs(sbri - t.bri[j]) * 0.9;

            if (score < bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }

        return new Hit(bestSlot, bestScore);
    }

    private static int bestParallel(final Field s, final int i, final Field t,
                                    final int[] alive, int aliveCount,
                                    ExecutorService pool, int threads) {
        int chunk = (aliveCount + threads - 1) / threads;
        List<Callable<Hit>> tasks = new ArrayList<>(threads);

        for (int start = 0; start < aliveCount; start += chunk) {
            final int from = start;
            final int to = Math.min(aliveCount, start + chunk);
            tasks.add(new Callable<Hit>() {
                @Override
                public Hit call() {
                    return bestSerial(s, i, t, alive, from, to);
                }
            });
        }

        try {
            List<Future<Hit>> results = pool.invokeAll(tasks);
            Hit best = null;
            for (Future<Hit> future : results) {
                Hit hit = future.get();
                // Lowest score wins; on a tie prefer the lower slot so the result is
                // the same no matter how the work was split.
                if (best == null || hit.score < best.score
                        || (hit.score == best.score && hit.slot < best.slot)) {
                    best = hit;
                }
            }
            return best == null ? 0 : best.slot;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return bestSerial(s, i, t, alive, 0, aliveCount).slot;
        } catch (ExecutionException e) {
            return bestSerial(s, i, t, alive, 0, aliveCount).slot;
        }
    }

    /* ----- pixel extraction ----- */

    private static final class Field {
        int n;
        int[] r;
        int[] g;
        int[] b;
        short[] x;
        short[] y;
        double[] sat;
        double[] bri;
    }

    private static int strideFor(int pixelCount, int cap) {
        if (pixelCount <= cap) {
            return 1;
        }
        return (int) Math.ceil(Math.sqrt((double) pixelCount / cap));
    }

    /**
     * Walks the image on a grid and keeps the pixels that are actually part of the
     * picture. Fully transparent padding is always dropped. Target images also drop
     * near-black, unsaturated pixels so the morph forms the subject rather than the
     * background.
     */
    private static Field sample(BufferedImage image, int stride, boolean dropBackground) {
        int width = image.getWidth();
        int height = image.getHeight();
        int capacity = ((width + stride - 1) / stride) * ((height + stride - 1) / stride);

        int[] r = new int[capacity];
        int[] g = new int[capacity];
        int[] b = new int[capacity];
        short[] x = new short[capacity];
        short[] y = new short[capacity];
        double[] sat = new double[capacity];
        double[] bri = new double[capacity];

        int n = 0;
        for (int py = 0; py < height; py += stride) {
            for (int px = 0; px < width; px += stride) {
                int argb = image.getRGB(px, py);
                if (((argb >>> 24) & 0xFF) == 0) {
                    continue;
                }

                int pr = (argb >> 16) & 0xFF;
                int pg = (argb >> 8) & 0xFF;
                int pb = argb & 0xFF;
                double brightness = (pr + pg + pb) / 3.0;
                double saturation = saturation(pr, pg, pb);

                if (dropBackground && brightness < 18 && saturation <= 0.08) {
                    continue;
                }

                r[n] = pr;
                g[n] = pg;
                b[n] = pb;
                x[n] = (short) px;
                y[n] = (short) py;
                bri[n] = brightness;
                sat[n] = saturation;
                n++;
            }
        }

        Field field = new Field();
        field.n = n;
        field.r = trim(r, n);
        field.g = trim(g, n);
        field.b = trim(b, n);
        field.x = trim(x, n);
        field.y = trim(y, n);
        field.sat = trim(sat, n);
        field.bri = trim(bri, n);
        return field;
    }

    /** Evenly drops pixels until the field has at most {@code limit} of them. */
    private static Field thin(Field field, int limit) {
        if (field.n <= limit || limit <= 0) {
            return field;
        }

        Field out = new Field();
        out.n = limit;
        out.r = new int[limit];
        out.g = new int[limit];
        out.b = new int[limit];
        out.x = new short[limit];
        out.y = new short[limit];
        out.sat = new double[limit];
        out.bri = new double[limit];

        double step = (double) field.n / limit;
        for (int i = 0; i < limit; i++) {
            int from = Math.min(field.n - 1, (int) (i * step));
            out.r[i] = field.r[from];
            out.g[i] = field.g[from];
            out.b[i] = field.b[from];
            out.x[i] = field.x[from];
            out.y[i] = field.y[from];
            out.sat[i] = field.sat[from];
            out.bri[i] = field.bri[from];
        }
        return out;
    }

    private static MorphData identity(Field field) {
        short[] xTarget = new short[field.n];
        short[] yTarget = new short[field.n];
        System.arraycopy(field.x, 0, xTarget, 0, field.n);
        System.arraycopy(field.y, 0, yTarget, 0, field.n);
        return new MorphData(field.n, toBytes(field.r), toBytes(field.g), toBytes(field.b),
                field.x, field.y, xTarget, yTarget);
    }

    private static double saturation(int r, int g, int b) {
        int max = Math.max(Math.max(r, g), b);
        int min = Math.min(Math.min(r, g), b);
        if (max == 0) {
            return 0.0;
        }
        return (max - min) / (double) max;
    }

    private static byte[] toBytes(int[] values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }

    private static int[] trim(int[] values, int n) {
        if (values.length == n) {
            return values;
        }
        int[] out = new int[n];
        System.arraycopy(values, 0, out, 0, n);
        return out;
    }

    private static short[] trim(short[] values, int n) {
        if (values.length == n) {
            return values;
        }
        short[] out = new short[n];
        System.arraycopy(values, 0, out, 0, n);
        return out;
    }

    private static double[] trim(double[] values, int n) {
        if (values.length == n) {
            return values;
        }
        double[] out = new double[n];
        System.arraycopy(values, 0, out, 0, n);
        return out;
    }
}
