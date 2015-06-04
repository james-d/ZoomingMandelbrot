package application;

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
import javafx.stage.Stage;
import javafx.util.Callback;

public class Main extends Application {

    private final Model model = new Model();

    @Override
    public void start(Stage primaryStage) throws Exception {
        
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
                e.printStackTrace();
                System.exit(1);
                return null;
            }
        };

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MandelbrotExplorer.fxml"));
        loader.setControllerFactory(controllerFactory);
        BorderPane root = loader.load();
        MandelbrotExplorerController controller = loader.getController();

        Scene scene = new Scene(root);
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
                new KeyboardAction(() -> model.setTrackingJuliaSet(!model
                        .isTrackingJuliaSet()), KeyCode.J,
                        KeyCombination.SHORTCUT_DOWN),

                new KeyboardAction(() -> model.setReverseZoomAction(!model
                        .isReverseZoomAction()), KeyCode.Z,
                        KeyCombination.SHORTCUT_DOWN),

                new KeyboardAction(model::reset, KeyCode.R,
                        KeyCombination.SHORTCUT_DOWN),

                new KeyboardAction(() -> controller.saveMandelbrotImage(scene),
                        KeyCode.S, KeyCombination.SHORTCUT_DOWN),

                new KeyboardAction(() -> controller.saveJuliaSetImage(scene),
                        KeyCode.S, KeyCombination.SHORTCUT_DOWN,
                        KeyCombination.SHIFT_DOWN),

                new KeyboardAction(
                        () -> controller.showHelp(scene.getWindow()),
                        KeyCode.H, KeyCombination.SHORTCUT_DOWN),

                new KeyboardAction(Platform::exit, KeyCode.Q,
                        KeyCombination.SHORTCUT_DOWN)

        );

        scene.setOnKeyPressed(e -> actions.forEach(action -> action
                .runIfMatches(e)));

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
