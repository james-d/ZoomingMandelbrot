package application;

import static javafx.scene.input.KeyCode.H;
import static javafx.scene.input.KeyCode.J;
import static javafx.scene.input.KeyCode.Q;
import static javafx.scene.input.KeyCode.R;
import static javafx.scene.input.KeyCode.S;
import static javafx.scene.input.KeyCode.Z;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;


public class Main extends Application {

    private final Model model = new Model();

    @Override
    public void start(Stage primaryStage) throws Exception {
        
        model.setErrorHandler((message, exception) -> 
            new ErrorDialog(message, exception, primaryStage).show());
        
        Callback<Class<?>, Object> controllerFactory = type -> {
            try {
                for (Constructor<?> c : type.getConstructors()) {
                    Class<?>[] parameterTypes = c.getParameterTypes();
                    if (parameterTypes.length == 1
                            && parameterTypes[0] == Model.class) {
                        return c.newInstance(model);
                    }
                }
                return type.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MandelbrotExplorer.fxml"));
        loader.setControllerFactory(controllerFactory);
        BorderPane root = loader.load();
        MandelbrotExplorerController controller = loader.getController();

        Scene scene = new Scene(root, Color.TRANSPARENT);
        setUpKeyboardShortcuts(scene, controller);

        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Mandelbrot Set");

        primaryStage.show();

    }

    @Override
    public void stop() {
        model.shutdown();
    }

    private void setUpKeyboardShortcuts(Scene scene,
            MandelbrotExplorerController controller) {

        List<KeyboardAction> actions = Arrays.asList(
                
                new KeyboardAction(() -> model.setTrackingJuliaSet(!model.isTrackingJuliaSet()), 
                        J, SHORTCUT_DOWN),

                new KeyboardAction(() -> model.setReverseZoomAction(!model.isReverseZoomAction()), 
                        Z, SHORTCUT_DOWN),

                new KeyboardAction(model::reset, R, SHORTCUT_DOWN),

                new KeyboardAction(() -> controller.saveMandelbrotImage(scene),
                        S, SHORTCUT_DOWN),

                new KeyboardAction(() -> controller.saveJuliaSetImage(scene),
                        S, SHORTCUT_DOWN, SHIFT_DOWN),

                new KeyboardAction(() -> controller.showHelp(scene.getWindow()), 
                        H, SHORTCUT_DOWN),

                new KeyboardAction(Platform::exit, Q, SHORTCUT_DOWN)

        );

        scene.setOnKeyPressed(e -> 
            actions.forEach(action -> action.runIfMatches(e)));

    }

    private static class KeyboardAction {
        private final KeyCombination key;
        private final Runnable action;

        KeyboardAction(Runnable action, KeyCode keyCode,
                KeyCombination.Modifier... modifiers) {
            this.key = new KeyCodeCombination(keyCode, modifiers);
            this.action = action;
        }

        void runIfMatches(KeyEvent event) {
            if (key.match(event))
                action.run();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
