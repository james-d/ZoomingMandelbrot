package application;

import java.io.File;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;

public class ActionController {
    @FXML
    private ContextMenu menu;
    @FXML
    private CheckMenuItem trackJuliaSet;
    @FXML
    private CheckMenuItem reverseZoom;
    @FXML
    private MenuItem reset;

    private FileChooser fileChooser = new FileChooser();
    private Scene helpScene;

    private final Model model;

    public ActionController(Model model) {
        this.model = model;
    }

    public void initialize() {
        model.trackingJuliaSetProperty().bindBidirectional(
                trackJuliaSet.selectedProperty());
        model.reverseZoomActionProperty().bindBidirectional(
                reverseZoom.selectedProperty());

        reset.disableProperty().bind(model.zoomingInProgressProperty());
    }

    @FXML
    private void reset() {
        model.reset();
    }

    @FXML
    private void saveMandelbrotImage() {
        saveMandelbrotImage(menu.getOwnerNode().getScene());
    }

    void saveMandelbrotImage(Scene scene) {
        if (model.getCurrentMandelbrot() != null) {
            saveImage(model.getCurrentMandelbrot().getImage(),
                    scene.getWindow(), "Save Mandelbrot Image");
        }
    }

    @FXML
    private void saveJuliaSetImage() {
        saveJuliaSetImage(menu.getOwnerNode().getScene());
    }

    void saveJuliaSetImage(Scene scene) {
        if (model.getCurrentJuliaSet() != null) {
            saveImage(model.getCurrentJuliaSet().getImage(), scene.getWindow(),
                    "Save Julia Set Image");
        }
    }

    private void saveImage(Image image, Window window, String title) {
        
        fileChooser.getExtensionFilters().add(
                new ExtensionFilter("Portable Network Graphics", "*.png"));
        
        File file = fileChooser.showSaveDialog(window);
        fileChooser.setTitle(title);
        if (file != null) {
            try {
                String fileName = file.toString();
                String format = fileName.substring(fileName.lastIndexOf('.') + 1);
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), format, file);
            } catch (Exception e) {
                model.errorOccurred("An error occurred saving the image", e);
            }
        }
    }

    @FXML
    private void showHelp() {
        Scene scene = menu.getOwnerNode().getScene();
        showHelp(scene.getWindow());
    }

    void showHelp(Window window) {
        if (helpScene == null) {
            helpScene = createHelpScene();
        }
        Stage helpStage = new Stage();
        helpStage.setScene(helpScene);
        helpStage.initOwner(window);
        helpStage.setX(window.getX() + window.getWidth() * 0.75);
        helpStage.setY(window.getY() + window.getHeight() * 0.25);
        helpStage.show();
    }

    private Scene createHelpScene() {
        WebView webView = new WebView();
        webView.getEngine().load(
                getClass().getResource("/resources/help/help.html")
                        .toExternalForm());

        Button close = new Button("Close");
        close.setOnAction(e -> close.getScene().getWindow().hide());
        HBox controls = new HBox(close);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10));
        BorderPane root = new BorderPane(webView, null, null, controls, null);
        root.setPadding(new Insets(10));
        return new Scene(root, 600, 400);
    }

    @FXML
    private void exit() {
        Platform.exit();
    }

}
