package application;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

public class Model {
    public static final double ZOOM_FACTOR = 4.0;
    public static final int VIEW_WIDTH = 400;
    public static final int VIEW_HEIGHT = 400;

    public static final int ANIMATION_FRAMES = 15;

    private final ObjectProperty<MandelbrotView> currentMandelbrot = new SimpleObjectProperty<>();
    private final ObjectProperty<JuliaSetView> currentJuliaSet = new SimpleObjectProperty<>();

    private final ExecutorService exec = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    private final BlockingQueue<MandelbrotView> viewQueue = new ArrayBlockingQueue<>(10);

    private final IntegerProperty framesPendingRendering = new SimpleIntegerProperty();

    private final ReadOnlyBooleanWrapper zoomingInProgress = new ReadOnlyBooleanWrapper();

    private final IntegerProperty frameCount = new SimpleIntegerProperty();

    private BooleanProperty trackingJuliaSet = new SimpleBooleanProperty();
    private BooleanProperty reverseZoomAction = new SimpleBooleanProperty();
    private BooleanProperty guessIteration = new SimpleBooleanProperty();

    public Model() {
        zoomingInProgress.bind(framesPendingRendering.greaterThan(0));
    }

    public final ReadOnlyBooleanProperty zoomingInProgressProperty() {
        return zoomingInProgress.getReadOnlyProperty();
    }

    public final boolean isZoomingInProgress() {
        return zoomingInProgressProperty().get();
    }

    public final ObjectProperty<MandelbrotView> currentMandelbrotProperty() {
        return this.currentMandelbrot;
    }

    public final MandelbrotView getCurrentMandelbrot() {
        return this.currentMandelbrotProperty().get();
    }

    public final void setCurrentMandelbrot(final MandelbrotView currentMandelbrot) {
        this.currentMandelbrotProperty().set(currentMandelbrot);
    }

    public final ObjectProperty<JuliaSetView> currentJuliaSetProperty() {
        return this.currentJuliaSet;
    }

    public final JuliaSetView getCurrentJuliaSet() {
        return this.currentJuliaSetProperty().get();
    }

    public final void setCurrentJuliaSet(final JuliaSetView currentJuliaSet) {
        this.currentJuliaSetProperty().set(currentJuliaSet);
    }

    public final IntegerProperty framesPendingRenderingProperty() {
        return this.framesPendingRendering;
    }

    public final int getFramesPendingRendering() {
        return this.framesPendingRenderingProperty().get();
    }

    public final void setFramesPendingRendering(final int framesPendingRendering) {
        this.framesPendingRenderingProperty().set(framesPendingRendering);
    }

    public final BooleanProperty trackingJuliaSetProperty() {
        return this.trackingJuliaSet;
    }

    public final boolean isTrackingJuliaSet() {
        return this.trackingJuliaSetProperty().get();
    }

    public final void setTrackingJuliaSet(final boolean trackingJuliaSet) {
        this.trackingJuliaSetProperty().set(trackingJuliaSet);
    }

    public final BooleanProperty reverseZoomActionProperty() {
        return this.reverseZoomAction;
    }

    public final boolean isReverseZoomAction() {
        return this.reverseZoomActionProperty().get();
    }

    public final void setReverseZoomAction(final boolean reverseZoomAction) {
        this.reverseZoomActionProperty().set(reverseZoomAction);
    }

    public final BooleanProperty guessIterationProperty() {
        return this.guessIteration;
    }

    public final boolean isGuessIteration() {
        return this.guessIterationProperty().get();
    }

    public final void setGuessIteration(final boolean guessIteration) {
        this.guessIterationProperty().set(guessIteration);
    }

    public final IntegerProperty frameCountProperty() {
        return this.frameCount;
    }

    public final int getFrameCount() {
        return this.frameCountProperty().get();
    }

    public final void setFrameCount(final int framesPerSecond) {
        this.frameCountProperty().set(framesPerSecond);
    }

    public BlockingQueue<MandelbrotView> getViewQueue() {
        return viewQueue;
    }

    /**
     * 
     * @return The rendering progress during a zoom, as a double between 0.0 and 1.0
     */
    public double getRenderProgress() {
        return 1.0 - getFramesPendingRendering() / (ANIMATION_FRAMES - 1.0);
    }

    public void shutdown() {
        exec.shutdown();
    }

    public void reset() {
        setFramesPendingRendering(1);
        exec.execute(() -> {
            try {
                viewQueue.put(createMandelbrotView(-0.5, 0, 3, 3, 50));
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void startZoom(double mouseX, double mouseY, double zoomFactor,
            Optional<Integer> maxIterations) {

        final Bounds bounds = currentMandelbrot.get().getBounds();

        final double currentX = bounds.getMinX() + bounds.getWidth() / 2;
        final double currentY = bounds.getMinY() + bounds.getHeight() / 2;

        final double deltaX = bounds.getWidth() * (mouseX / VIEW_WIDTH - 0.5); 
        final double deltaY = bounds.getHeight() * (1 - mouseY / VIEW_HEIGHT - 0.5); 

        final double currentWidth = bounds.getWidth();
        final double currentHeight = bounds.getHeight();

        final double deltaWidth = (1 - zoomFactor) * bounds.getWidth() / zoomFactor; // targetWidth - currentWidth ;
        final double deltaHeight = (1 - zoomFactor) * bounds.getHeight() / zoomFactor; // targetHeight - currentHeight ;

        final double frameDeltaX = deltaX / ANIMATION_FRAMES;
        final double frameDeltaY = deltaY / ANIMATION_FRAMES;
        final double frameDeltaWidth = deltaWidth / ANIMATION_FRAMES;
        final double frameDeltaHeight = deltaHeight / ANIMATION_FRAMES;

        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() {

                for (int i = 1; i <= ANIMATION_FRAMES; i++) {
                    try {

                        double width = currentWidth + i * frameDeltaWidth;
                        int iterationLevel = maxIterations.orElse(estimateIterationLevel(1 / width));

                        viewQueue.put(createMandelbrotView(
                                currentX + i * frameDeltaX, 
                                currentY + i * frameDeltaY,
                                currentWidth + i * frameDeltaWidth,
                                currentHeight + i * frameDeltaHeight,
                                iterationLevel));
                    } catch (InterruptedException exc) {
                        Thread.currentThread().interrupt();
                    }
                    updateProgress(i, ANIMATION_FRAMES);
                }
                return null;
            }
        };

        framesPendingRendering.set(ANIMATION_FRAMES);

        exec.execute(task);
    }

    private MandelbrotView createMandelbrotView(double centerX, double centerY,
            double width, double height, int maxIterations) {

        Bounds bounds = new BoundingBox(centerX - width / 2, centerY - height / 2, width, height);
        MandelbrotView mandelbrot = new MandelbrotView(VIEW_WIDTH, VIEW_HEIGHT,
                bounds, maxIterations, exec);
        mandelbrot.compute();

        return mandelbrot;
    }

    private int estimateIterationLevel(double scale) {
        return (int) (Math.sqrt(2 * Math.sqrt(Math.abs(1 - Math.sqrt(6.66 * scale)))) * 66.6);
    }

    public void computeJuliaSet(double cx, double cy, int iterationLevel,
            Runnable whenFinished) {
        exec.execute(() -> {
            JuliaSetView juliaSet = new JuliaSetView(VIEW_WIDTH, VIEW_HEIGHT,
                    iterationLevel, exec, cx, cy);
            juliaSet.compute();
            setCurrentJuliaSet(juliaSet);
            whenFinished.run();
        });
    }

}
