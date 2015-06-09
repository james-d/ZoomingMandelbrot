package application;

import javafx.geometry.Bounds;

public class MandelbrotView extends FractalView {

    public MandelbrotView(int width, int height, Bounds bounds,
            int maxIterations) {
        super(width, height, bounds, maxIterations);
    }

    @Override
    protected int computeIterationCount(double cx, double cy, int maxIterations) {
        
        // TODO: figure approximate bounds for the fixed point cardioid and period 2 bulb
        // and avoid these moderately expensive tests if possible
        
        // period 2 bulb:
        
        if ((1+cx)*(1+cx)+cy*cy < 0.0625) {
            return maxIterations ;
        }
        
        // fixed point cardiod:
        
        double kx = 1 - 4 * cx ;
        double ky = -4 * cy ;
        double r = Math.sqrt(kx * kx + ky * ky) ;
        double cosTheta = kx / r ;
        if ( r < 2 + 2 * cosTheta) {
            return maxIterations ;
        }
        
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
