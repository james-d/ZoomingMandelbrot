package application;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class ErrorDialog {
    
    private final Parent view ;
    private final Window owner ;
    
    public ErrorDialog(String message, Exception exc, Window owner) {
        
        this.owner = owner ;
        
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        Label excMessageLabel = new Label(exc.getMessage());
        excMessageLabel.setWrapText(true);
        
        StringWriter sw = new StringWriter();
        exc.printStackTrace(new PrintWriter(sw));
        Label stackTraceLabel = new Label(sw.toString());
        stackTraceLabel.setWrapText(true);
        
        TitledPane stackTracePane = new TitledPane();
        stackTracePane.setExpanded(false);
        stackTracePane.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            Scene scene = stackTracePane.getScene();
            if (scene != null) {
                Window window = scene.getWindow();
                if (window != null) {
                    window.sizeToScene();
                }
            }
        });
        stackTracePane.setText("Details");
        ScrollPane scroller = new ScrollPane(new HBox(stackTraceLabel));
        scroller.setFitToWidth(true);
        stackTracePane.setContent(scroller);
        
        VBox root = new VBox(5, messageLabel, excMessageLabel, stackTracePane);
        root.getStyleClass().add("error-pane");
        
        
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> root.getScene().getWindow().hide());
        
        HBox buttons = new HBox(5, closeButton);
        buttons.getStyleClass().add("buttons");
        
        view = new BorderPane(root, null, null, buttons, null);
        
    }
    
    public void show() {
        Scene scene = new Scene(view);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initOwner(owner);
        stage.setScene(scene);
        stage.setX(owner.getX() + 50);
        stage.setY(owner.getY() + 50);
        stage.show();
    }
    
}
