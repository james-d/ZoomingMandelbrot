package application;

import java.util.Optional;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;

public class ControlPanelController {

    private final Model model;

    @FXML
    private Pane root;
    @FXML
    private Label sizeLabel;
    @FXML
    private Label fpsLabel;
    @FXML
    private TextField iterationTextField;
    @FXML
    private CheckBox guessIterationLevel;
    @FXML
    private ProgressBar renderProgressBar;

    private TextFormatter<Integer> iterationLevelFormatter;

    public ControlPanelController(Model model) {
        this.model = model;
    }

    public void initialize() {

        root.disableProperty().bind(model.zoomingInProgressProperty());

        setUpSizeLabelBinding();
        setUpFrameCount();
        setupIterationControl();
        setupProgressBarBinding();
    }
    
    @FXML
    private void updateIterationLevel() {
        model.updateMaxIterations(getIterationLevel().orElseThrow(() ->
                new IllegalStateException("Updating iteration level when \"Guess\" is selected")));
    }

    private void setUpSizeLabelBinding() {
        model.currentMandelbrotProperty().addListener(
                (obs, oldMandelbrot, newMandelbrot) -> 
                    sizeLabel.setText(String.format("Size: %.2g", newMandelbrot.getBounds().getWidth())));
    }

    private void setUpFrameCount() {

        Timeline fpsMeter = new Timeline(new KeyFrame(Duration.seconds(0.2),
                e -> {
                    fpsLabel.setText("Frames per second: "+ model.getFrameCount() * 5);
                    model.setFrameCount(0);
                }));

        fpsMeter.setCycleCount(Animation.INDEFINITE);

        fpsMeter.play();
    }

    private void setupIterationControl() {
        model.guessIterationProperty().bindBidirectional(guessIterationLevel.selectedProperty());

        iterationLevelFormatter = new TextFormatter<Integer>(new IntegerStringConverter());
        iterationLevelFormatter.setValue(50);

        iterationTextField.textProperty().addListener(
                (obs, oldValue, newValue) -> iterationTextField.commitValue());
        iterationTextField.setTextFormatter(iterationLevelFormatter);
        iterationTextField.disableProperty().bind(
                model.guessIterationProperty());

        model.currentMandelbrotProperty().addListener(
                (obs, oldMandelbrot, newMandelbrot) -> 
                    iterationLevelFormatter.setValue(newMandelbrot.getIterationLevel()));
    }

    private void setupProgressBarBinding() {
        renderProgressBar.progressProperty().bind(
                Bindings.createDoubleBinding(() -> model.getRenderProgress(),
                        model.framesPendingRenderingProperty()));
    }

    Optional<Integer> getIterationLevel() {
        Optional<Integer> maxIterations;
        if (model.isGuessIteration()) {
            maxIterations = Optional.empty();
        } else {
            Integer userIterationLevel = iterationLevelFormatter.getValue();
            if (userIterationLevel == null || userIterationLevel < 10) {
                userIterationLevel = 10 ;
            }
            maxIterations = Optional.of(userIterationLevel);
        }
        return maxIterations;
    }

}
