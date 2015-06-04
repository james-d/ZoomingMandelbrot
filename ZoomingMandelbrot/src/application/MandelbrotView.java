package application;

import java.util.concurrent.Executor;

import javafx.geometry.Bounds;

public class MandelbrotView extends FractalView {

    public MandelbrotView(int width, int height, Bounds bounds,
            int maxIterations, Executor exec) {
        super(width, height, bounds, maxIterations, exec);
    }

    @Override
    protected int computeIterationCount(double cx, double cy, int maxIterations) {
        double x = cx;
        double y = cy;
        double x2 = x * x;
        double y2 = y * y;

        int iterations = 0;

        while (x2 + y2 < 4 && iterations < maxIterations) {
            x2 = x * x;
            y2 = y * y;
            y = 2 * x * y + cy;
            x = x2 - y2 + cx;
            iterations++;
        }

        return iterations;
    }

}
