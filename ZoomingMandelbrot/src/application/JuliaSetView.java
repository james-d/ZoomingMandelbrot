package application;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

public class JuliaSetView extends FractalView {

    private final double cx;
    private final double cy;

    private static final Bounds BOUNDS = new BoundingBox(-2, -2, 4, 4);

    public JuliaSetView(int width, int height, int maxIterations,
            double cx, double cy) {
        super(width, height, BOUNDS, maxIterations);
        this.cx = cx;
        this.cy = cy;
    }

    public double getCx() {
        return cx;
    }

    public double getCy() {
        return cy;
    }

    @Override
    protected int computeIterationCount(double x, double y, int maxIterations) {
        int iterations = 0;
        double x2 = x * x;
        double y2 = y * y;
        while (x2 + y2 < 4 && iterations < maxIterations) {
            y = 2 * x * y + cy;
            x = x2 - y2 + cx;
            x2 = x * x;
            y2 = y * y;
            iterations++;
        }
        return iterations;
    }

}
