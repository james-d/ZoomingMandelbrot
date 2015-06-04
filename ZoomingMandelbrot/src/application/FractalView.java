package application;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public abstract class FractalView {

    private static final int[] PALETTE = createDefaultPalette();


    protected abstract int computeIterationCount(double x, double y,
            int maxIterations);

    private final int maxIterations;
    private final int width;
    private final int height;
    private final Bounds bounds;
    private final WritableImage image;
    private AtomicInteger minComputedIterations = new AtomicInteger(Integer.MAX_VALUE);
    private AtomicInteger maxComputedIterations = new AtomicInteger(Integer.MIN_VALUE);

    public FractalView(int width, int height, Bounds bounds, int maxIterations) {
        this.width = width;
        this.height = height;
        this.bounds = bounds;
        this.maxIterations = maxIterations;

        this.image = new WritableImage(width, height);
    }

    private static int[] createDefaultPalette() {

        final int numCols = 256;
        int[] palette = new int[numCols];
        for (int i = 0; i < numCols; i++) {
            Color color = Color.hsb(180.0 * i / numCols, 1.0, 1.0);
            int a = 255;
            int r = (int) (255 * color.getRed());
            int g = (int) (255 * color.getGreen());
            int b = (int) (255 * color.getBlue());
            palette[i] = (a << 24) | (r << 16) | (g << 8) | b;

        }
        return palette;
    }

    public Image getImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Bounds getBounds() {
        return bounds;
    }

    public int getIterationLevel() {
        return maxIterations;
    }

    public int getMinComputedIterations() {
        return minComputedIterations.get();
    }

    public int getMaxComputedIterations() {
        return maxComputedIterations.get();
    }

    public void compute(Executor exec, int parallelizationLevel) {

        final int numStrips = parallelizationLevel;
        final int[] boundaries = new int[numStrips + 1];
        for (int i = 0; i <= numStrips; i++) {
            boundaries[i] = i * height / numStrips;
        }
        final CountDownLatch latch = new CountDownLatch(numStrips);
        final PixelWriter pw = image.getPixelWriter();

        for (int strip = 0; strip < numStrips; strip++) {
            final int startRow = boundaries[strip];
            final int endRow = boundaries[strip + 1];
            exec.execute(() -> {
                try {
                    int[] pixels = new int[width * (endRow - startRow)];
                    for (int y = startRow; y < endRow; y++) {
                        final double cy = bounds.getMinY() + (height - y) * bounds.getHeight() / height;
                        for (int x = 0; x < width; x++) {
                            final double cx = bounds.getMinX() + x * bounds.getWidth() / width;
                            pixels[(y - startRow) * width + x] = computePixel(cx, cy);
                        }
                    }
                    pw.setPixels(0, startRow, width, endRow - startRow,
                            PixelFormat.getIntArgbInstance(), pixels, 0, width);
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    private int computePixel(double cx, double cy) {

        int iterations = computeIterationCount(cx, cy, maxIterations);

        maxComputedIterations.updateAndGet(x -> iterations < maxIterations ? Math.max(x, iterations) : x);
        minComputedIterations.updateAndGet(x -> Math.min(x, iterations));

        if (iterations >= maxIterations) {
            return /* black */0xff000000;
        }

        int index = maxIterations >= PALETTE.length 
                ? iterations % PALETTE.length 
                : (PALETTE.length * iterations) / maxIterations;

        return PALETTE[index];
    }

}