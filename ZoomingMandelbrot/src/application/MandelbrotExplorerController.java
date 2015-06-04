package application;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.AnimationTimer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;

public class MandelbrotExplorerController {
    @FXML
    private ImageView mandelbrotView;
    @FXML
    private Rectangle dragRect;
    @FXML
    private Rectangle zoomRect;
    @FXML
    private ImageView juliaView;

    @FXML
    private ControlPanelController controlPanelController;
    @FXML
    private ContextMenuController contextMenuController;

    private final Model model;

    private final ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();

    public MandelbrotExplorerController(Model model) {
        this.model = model;
    }

    @FXML
    private void initialize() {
        model.currentMandelbrotProperty().addListener(
                (obs, oldMandelbrot, newMandelbrot) -> mandelbrotView
                        .setImage(newMandelbrot.getImage()));
        model.currentJuliaSetProperty().addListener(
                (obs, oldJulia, newJulia) -> juliaView.setImage(model
                        .getCurrentJuliaSet().getImage()));
        mandelbrotView.disableProperty()
                .bind(model.zoomingInProgressProperty());

        setUpJuliaSetTracking();
        setUpMandelbrotUpdating();

        bindZoomingRectangles();

        model.reset();
    }

    @FXML
    private void zoomByClick(MouseEvent e) {
        if (e.getClickCount() == 2) {
            double zoomFactor;
            if (e.isShiftDown()) {
                zoomFactor = 1 / Model.ZOOM_FACTOR;
            } else {
                zoomFactor = Model.ZOOM_FACTOR;
            }
            if (model.isReverseZoomAction()) {
                zoomFactor = 1 / zoomFactor;
            }
            model.startZoom(e.getX(), e.getY(), zoomFactor,
                    controlPanelController.getIterationLevel());
        }
    }

    @FXML
    private void startDragging(MouseEvent e) {
        mouseDown.set(new Point2D(e.getX(), e.getY()));
    }

    @FXML
    private void drag(MouseEvent e) {
        if (mouseDown.get() != null) {
            dragRect.setX(Math.min(e.getX(), mouseDown.get().getX()));
            dragRect.setY(Math.min(e.getY(), mouseDown.get().getY()));
            dragRect.setWidth(Math.abs(e.getX() - mouseDown.get().getX()));
            dragRect.setHeight(Math.abs(e.getY() - mouseDown.get().getY()));
        }

    }

    @FXML
    private void zoomByDrag() {
        if (mouseDown.get() != null) {
            double x = zoomRect.getX();
            double y = zoomRect.getY();
            double w = zoomRect.getWidth();
            double h = zoomRect.getHeight();
            mouseDown.set(null);
            model.startZoom(x + w / 2, y + h / 2, Model.VIEW_WIDTH / w,
                    controlPanelController.getIterationLevel());
        }

    }

    private void bindZoomingRectangles() {
        ChangeListener<Number> updateZoomRect = (obs, oldValue, newValue) -> {
            if (dragRect.getHeight() < dragRect.getWidth()) {
                zoomRect.setX(dragRect.getX());
                zoomRect.setWidth(dragRect.getWidth());
                zoomRect.setY(dragRect.getY()
                        - (dragRect.getWidth() - dragRect.getHeight()) / 2);
                zoomRect.setHeight(dragRect.getWidth());
            } else {
                zoomRect.setY(dragRect.getY());
                zoomRect.setHeight(dragRect.getHeight());
                zoomRect.setX(dragRect.getX()
                        - (dragRect.getHeight() - dragRect.getWidth()) / 2);
                zoomRect.setWidth(dragRect.getHeight());
            }
        };

        dragRect.widthProperty().addListener(updateZoomRect);
        dragRect.heightProperty().addListener(updateZoomRect);
        zoomRect.visibleProperty().bind(dragRect.visibleProperty());
        dragRect.visibleProperty().bind(mouseDown.isNotNull());
    }

    private void setUpJuliaSetTracking() {

        AtomicBoolean juliaComputing = new AtomicBoolean();

        mandelbrotView.setOnMouseMoved(e -> {
            if (model.isTrackingJuliaSet() && juliaComputing.compareAndSet(false, true)) {
                Bounds bounds = model.getCurrentMandelbrot().getBounds();
                final double cx = bounds.getWidth() * e.getX() / Model.VIEW_WIDTH + bounds.getMinX();
                final double cy = bounds.getHeight() * (1 - e.getY() / Model.VIEW_HEIGHT) + bounds.getMinY();
                model.computeJuliaSet(cx, cy, 50,
                        () -> juliaComputing.set(false));
            }
        });
    }

    private void setUpMandelbrotUpdating() {

        AnimationTimer viewUpdater = new AnimationTimer() {
            @Override
            public void handle(long now) {
                MandelbrotView mandelbrot = model.getViewQueue().poll();
                if (mandelbrot != null) {
                    model.setFrameCount(model.getFrameCount() + 1);
                    model.setFramesPendingRendering(model
                            .getFramesPendingRendering() - 1);
                    model.setCurrentMandelbrot(mandelbrot);
                }
            }
        };

        viewUpdater.start();
    }

    void saveMandelbrotImage(Scene scene) {
        contextMenuController.saveMandelbrotImage(scene);
    }

    void saveJuliaSetImage(Scene scene) {
        contextMenuController.saveJuliaSetImage(scene);
    }

    void showHelp(Window window) {
        contextMenuController.showHelp(window);
    }
}
