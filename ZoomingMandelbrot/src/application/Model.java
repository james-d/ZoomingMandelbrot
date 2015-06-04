package application;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

public class Model {
    public static final double ZOOM_FACTOR = 4.0;
    public static final int VIEW_WIDTH = 400;
    public static final int VIEW_HEIGHT = 400;

    public static final int ANIMATION_FRAMES = 15;

    private final ObjectProperty<MandelbrotView> currentMandelbrot = new SimpleObjectProperty<>();
    private final AtomicReference<JuliaSetView> currentJuliaSet = new AtomicReference<>();

    private final int PARALLELIZATION_LEVEL = Runtime.getRuntime().availableProcessors() ;
    
    private final ExecutorService exec = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    private final BlockingQueue<MandelbrotView> computationQueue = new ComputationQueue(10);
    private final BlockingQueue<MandelbrotView> viewQueue = new ViewQueue(10);

    private final ReadOnlyIntegerWrapper framesPendingRendering = new ReadOnlyIntegerWrapper();

    private final ReadOnlyBooleanWrapper zoomingInProgress = new ReadOnlyBooleanWrapper();

    private final IntegerProperty frameCount = new SimpleIntegerProperty();

    private BooleanProperty trackingJuliaSet = new SimpleBooleanProperty();
    private BooleanProperty reverseZoomAction = new SimpleBooleanProperty();
    private BooleanProperty guessIteration = new SimpleBooleanProperty();

    /*
     * ============
     * Constructors
     * ============
     */
    
    public Model() {
        zoomingInProgress.bind(framesPendingRendering.greaterThan(0));
        
        Thread computeThread = new Thread(() -> {
            try {
                while (true) {
                    MandelbrotView mandelbrot = computationQueue.take();
                    mandelbrot.compute(exec, PARALLELIZATION_LEVEL);
                    viewQueue.put(mandelbrot);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        computeThread.setDaemon(true);
        exec.execute(computeThread);
    }
    
    /*
     * ==============================
     * Property Accessors and Mutators
     * ==============================
     */

    public final ReadOnlyBooleanProperty zoomingInProgressProperty() {
        return zoomingInProgress.getReadOnlyProperty();
    }

    public final boolean isZoomingInProgress() {
        return zoomingInProgressProperty().get();
    }

    /**
     * The currently displayed MandelbrotView. 
     * <strong>Thread Safety:</strong> Access to this property must be restricted to a single thread.
     * The <code>startZoom(...)</code> accesses this property and must be called on the same thread.
     * @return
     */
    public final ObjectProperty<MandelbrotView> currentMandelbrotProperty() {
        return this.currentMandelbrot;
    }

    /**
     * The currently displayed MandelbrotView. 
     * <strong>Thread Safety:</strong> Access to this property must be restricted to a single thread.
     * The <code>startZoom(...)</code> accesses this property and must be called on the same thread.
     * @return
     */
    public final MandelbrotView getCurrentMandelbrot() {
        return this.currentMandelbrotProperty().get();
    }

    /**
     * Update the currently displayed MandelbrotView. 
     * <strong>Thread Safety:</strong> Access to this property must be restricted to a single thread.
     * The <code>startZoom(...)</code> accesses this property and must be called on the same thread.
     * @return
     */
    public final void setCurrentMandelbrot(final MandelbrotView currentMandelbrot) {
        this.currentMandelbrotProperty().set(currentMandelbrot);
    }

    public final ReadOnlyIntegerProperty framesPendingRenderingProperty() {
        return this.framesPendingRendering.getReadOnlyProperty();
    }

    public final int getFramesPendingRendering() {
        return this.framesPendingRenderingProperty().get();
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

    /*
     * ==========
     * Public API
     * ==========
     */
    
    /**
     * 
     * @return A queue of computed MandelbrotViews ready for rendering
     */
    public BlockingQueue<MandelbrotView> getViewQueue() {
        return viewQueue;
    }
    
    /**
     * 
     * @return The current JuliaSetView, or null if no JuliaSet is displayed.
     */
    public JuliaSetView getCurrentJuliaSet() {
        return currentJuliaSet.get() ;
    }

    /**
     * 
     * @return The rendering progress during a zoom, as a double between 0.0 and 1.0
     */
    public double getRenderProgress() {
        return 1.0 - getFramesPendingRendering() / (ANIMATION_FRAMES - 1.0);
    }

    /**
     * Gracefully shutdown.
     */
    public void shutdown() {
        exec.shutdown();
    }

    /**
     * Reset to the default image. No zooming is performed.
     */
    public void reset() {
        exec.execute(() -> {
            try {
                computationQueue.put(createMandelbrotView(-0.5, 0, 3, 3, 50));
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Zoom to a new location, based on the pixel coordinates in the space
     * of <code>getCurrentMandelbrot().getImage()</code>.
     * <strong>Thread safety:</strong> This method must be called from a single thread. It references the
     * <code>currentMandelbrotProperty()</code>, so any other references to that property must be performed on the
     * same thread. This method creates the MandelbrotViews and schedules them for computation on a background
     * thread, placing them in the `viewQueue` when computation is complete.
     * @param pixelX x-coordinate of the center of the target zoom, in pixel coordinate space of the current view.
     * @param pixelY y-coordinate of the center of the target zoom, in pixel coordinate space of the current view.
     * @param zoomFactor Factor by which to zoom. A <code>zoomFactor > 1</code> indicates "zooming in".
     * @param maxIterations Maximum number of iterations for the computation.
     */
    public void startZoom(double pixelX, double pixelY, double zoomFactor,
            Optional<Integer> maxIterations) {

        final Bounds bounds = currentMandelbrot.get().getBounds();

        final double currentX = bounds.getMinX() + bounds.getWidth() / 2;
        final double currentY = bounds.getMinY() + bounds.getHeight() / 2;

        final double deltaX = bounds.getWidth() * (pixelX / VIEW_WIDTH - 0.5); 
        final double deltaY = bounds.getHeight() * (1 - pixelY / VIEW_HEIGHT - 0.5); 

        final double currentWidth = bounds.getWidth();
        final double currentHeight = bounds.getHeight();

        final double deltaWidth = (1 - zoomFactor) * bounds.getWidth() / zoomFactor; // targetWidth - currentWidth ;
        final double deltaHeight = (1 - zoomFactor) * bounds.getHeight() / zoomFactor; // targetHeight - currentHeight ;

        final double frameDeltaX = deltaX / ANIMATION_FRAMES;
        final double frameDeltaY = deltaY / ANIMATION_FRAMES;
        final double frameDeltaWidth = deltaWidth / ANIMATION_FRAMES;
        final double frameDeltaHeight = deltaHeight / ANIMATION_FRAMES;
        
        for (int i = 1; i <= ANIMATION_FRAMES; i++) {
            try {

                double width = currentWidth + i * frameDeltaWidth;
                int iterationLevel = maxIterations.orElse(estimateIterationLevel(1 / width));

                computationQueue.put(createMandelbrotView(
                        currentX + i * frameDeltaX, 
                        currentY + i * frameDeltaY,
                        currentWidth + i * frameDeltaWidth,
                        currentHeight + i * frameDeltaHeight,
                        iterationLevel));
            } catch (InterruptedException exc) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Compute a new JuliaSetView in a background thread. When computation is complete,
     * the JuliaSetView is passed to the <code>whenFinished</code> callback.
     * @param cx The x-value in the complex plane for the Julia Set.
     * @param cy The y-value in the complex plane for the Julia Set.
     * @param iterationLevel The maximum number of iterations used in the computation.
     * @param whenFinished Callback to process the JuliaSetView when computation is finished.
     */
    public void computeJuliaSet(double cx, double cy, int iterationLevel,
            Consumer<JuliaSetView> whenFinished) {
        exec.execute(() -> {
            JuliaSetView juliaSet = new JuliaSetView(VIEW_WIDTH, VIEW_HEIGHT,
                    iterationLevel, cx, cy);
            juliaSet.compute(exec, PARALLELIZATION_LEVEL);
            whenFinished.accept(juliaSet);
        });
    }
    
    private MandelbrotView createMandelbrotView(double centerX, double centerY,
            double width, double height, int maxIterations) {

        Bounds bounds = new BoundingBox(centerX - width / 2, centerY - height / 2, width, height);
        MandelbrotView mandelbrot = new MandelbrotView(VIEW_WIDTH, VIEW_HEIGHT,
                bounds, maxIterations);

        return mandelbrot;
    }

    private int estimateIterationLevel(double scale) {
        return (int) (Math.sqrt(2 * Math.sqrt(Math.abs(1 - Math.sqrt(6.66 * scale)))) * 66.6);
    }


    @SuppressWarnings("serial")
    public class ViewQueue extends ArrayBlockingQueue<MandelbrotView> {

        public ViewQueue(int capacity) {
            super(capacity) ;
        }
        
        @Override
        public MandelbrotView remove() {
            MandelbrotView view = super.remove();
            framesPendingRendering.set(framesPendingRendering.get() - 1);
            return view ;
        }
        @Override
        public MandelbrotView poll() {
            MandelbrotView view = super.poll();
            if (view != null) {
                framesPendingRendering.set(framesPendingRendering.get() - 1);
            }
            return view ;
        }
        @Override
        public MandelbrotView take() throws InterruptedException {
            MandelbrotView view = super.take();
            framesPendingRendering.set(framesPendingRendering.get() - 1);
            return view ;
            
        }
        @Override
        public MandelbrotView poll(long timeout, TimeUnit unit)
                throws InterruptedException {
            MandelbrotView view = super.poll(timeout, unit);
            if (view != null) {
                framesPendingRendering.set(framesPendingRendering.get() - 1);
            }
            return view ;
        }
       
        @Override
        public boolean remove(Object o) {
            boolean result = super.remove(o);
            if (result) {
                framesPendingRendering.set(framesPendingRendering.get() - 1);                
            }
            return result ;
        }
        @Override
        public int drainTo(Collection<? super MandelbrotView> c) {
            int result = super.drainTo(c);
            framesPendingRendering.set(framesPendingRendering.get() - result);
            return result ;
        }
        @Override
        public int drainTo(Collection<? super MandelbrotView> c, int maxElements) {
            int result = super.drainTo(c, maxElements);
            framesPendingRendering.set(framesPendingRendering.get() - result);
            return result ;
        }
        
        
    }
    
    @SuppressWarnings("serial")
    public class ComputationQueue extends ArrayBlockingQueue<MandelbrotView> {


        public ComputationQueue(int capacity) {
            super(capacity);
        }
      

        @Override
        public boolean add(MandelbrotView e) {
            boolean result = super.add(e);
            if (result) {
                framesPendingRendering.set(framesPendingRendering.get() + 1);
            }
            return result ;
        }

        @Override
        public boolean offer(MandelbrotView e) {
            boolean result = super.offer(e);
            if (result) {
                framesPendingRendering.set(framesPendingRendering.get() + 1);
            }
            return result ;
        }

        @Override
        public void put(MandelbrotView e) throws InterruptedException {
            super.put(e);
             framesPendingRendering.set(framesPendingRendering.get() + 1);
        }

        @Override
        public boolean offer(MandelbrotView e, long timeout, TimeUnit unit)
                throws InterruptedException {
            boolean result = super.offer(e, timeout, unit);
            if (result) {
                framesPendingRendering.set(framesPendingRendering.get() + 1);
            }
            return result ;
        }


    }
    
}
