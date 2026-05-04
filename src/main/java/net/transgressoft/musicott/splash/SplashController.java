package net.transgressoft.musicott.splash;

import net.transgressoft.musicott.view.custom.ApplicationImage;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Branded startup splash window shown before Spring's {@code ApplicationContext} is
 * constructed and dismissed only after the main scene is fully laid out.
 *
 * <p>The splash is a plain JavaFX class (not a Spring bean) because it must exist
 * before Spring boots. It renders a four-element vertical layout — logo, build
 * version, progress bar, status label — on a {@link StageStyle#TRANSPARENT} stage
 * centered on the primary screen so the rounded-corner card sits on a transparent
 * surround.
 *
 * <p>All input events (mouse, key, drag) are consumed by event filters so the splash
 * cannot be dismissed early or interacted with. The progress bar and status label
 * are bindable to a {@link Task}'s {@code progressProperty} and {@code messageProperty}
 * via {@link #bindToTask(Task)}; binding MUST happen before the task starts to capture
 * early progress updates.
 *
 * <p>The splash version label is populated synchronously at construction via
 * {@link BuildVersionReader#read()}, which reads {@code /META-INF/build-info.properties}
 * from the classpath without waiting for Spring's {@code BuildProperties} bean.
 */
public class SplashController {

    private static final Logger logger = LoggerFactory.getLogger(SplashController.class);

    private static final double SPLASH_WIDTH = 480;
    private static final double SPLASH_HEIGHT = 320;
    private static final double LOGO_WIDTH = 120;
    private static final String STYLESHEET = "/css/splash.css";

    private static final String STYLE_CLASS_ROOT = "splash-root";
    private static final String STYLE_CLASS_VERSION = "splash-version";
    private static final String STYLE_CLASS_STATUS = "splash-status";

    final Stage splashStage;
    final VBox splashRoot;
    final ImageView logoView;
    final Label versionLabel;
    final ProgressBar progressBar;
    final Label statusLabel;

    private final DoubleProperty progressBacking = new SimpleDoubleProperty(0);
    private final StringProperty messageBacking = new SimpleStringProperty("");

    /**
     * Constructs the splash Stage and scene graph. Must be called on the FX thread.
     * The Stage is created but not yet shown — call {@link #show()} to display it.
     */
    public SplashController() {
        this.logoView = new ImageView(ApplicationImage.ABOUT_IMAGE.get());
        logoView.setFitWidth(LOGO_WIDTH);
        logoView.setPreserveRatio(true);

        this.versionLabel = new Label(BuildVersionReader.read());
        versionLabel.getStyleClass().add(STYLE_CLASS_VERSION);

        this.progressBar = new ProgressBar(0);
        progressBar.progressProperty().bind(progressBacking);

        this.statusLabel = new Label("");
        statusLabel.getStyleClass().add(STYLE_CLASS_STATUS);
        statusLabel.textProperty().bind(messageBacking);

        this.splashRoot = new VBox(logoView, versionLabel, progressBar, statusLabel);
        splashRoot.setAlignment(Pos.CENTER);
        splashRoot.getStyleClass().add(STYLE_CLASS_ROOT);

        Scene scene = new Scene(splashRoot, SPLASH_WIDTH, SPLASH_HEIGHT);
        // Transparent scene fill lets the rounded-corner background painted by .splash-root
        // be the only visible surface — without this, the corners outside the radius render
        // as the platform default white. Pairs with StageStyle.TRANSPARENT below.
        scene.setFill(Color.TRANSPARENT);
        var stylesheet = getClass().getResource(STYLESHEET);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        } else {
            logger.warn("Splash stylesheet {} not found on classpath; splash will render with platform defaults", STYLESHEET);
        }

        this.splashStage = new Stage(StageStyle.TRANSPARENT);
        splashStage.setScene(scene);
        splashStage.setResizable(false);
        splashStage.setAlwaysOnTop(true);
        splashStage.getIcons().add(ApplicationImage.APP_ICON.get());

        installNonInteractiveFilters();
        centerOnPrimaryScreen();
    }

    /**
     * Shows the splash Stage. Idempotent — safe to call once.
     */
    public void show() {
        splashStage.show();
    }

    /**
     * Hides and closes the splash Stage. Called by the dismiss flow after the fade
     * transition completes.
     */
    public void hide() {
        splashStage.hide();
        splashStage.close();
    }

    /**
     * Binds the splash progress bar and status label to the given task's
     * {@code progressProperty} and {@code messageProperty}. MUST be called BEFORE
     * the task is started to capture early progress updates.
     */
    public void bindToTask(Task<?> task) {
        progressBacking.bind(task.progressProperty());
        messageBacking.bind(task.messageProperty());
    }

    /**
     * Returns the splash Stage so the dismiss flow can run a {@code FadeTransition}
     * on its scene root.
     */
    public Stage getStage() {
        return splashStage;
    }

    /**
     * Returns the splash root node so {@code FadeTransition} can target it directly
     * (fading the root rather than the Stage avoids platform-specific Stage opacity
     * inconsistencies).
     */
    public VBox getRoot() {
        return splashRoot;
    }

    /**
     * Consumes mouse, key, and drag events at the capture phase so the splash is
     * non-interactive — clicks, key presses, and drag gestures have no effect.
     */
    private void installNonInteractiveFilters() {
        splashRoot.addEventFilter(MouseEvent.ANY, Event::consume);
        splashRoot.addEventFilter(KeyEvent.ANY, Event::consume);
        splashRoot.addEventFilter(DragEvent.ANY, Event::consume);
    }

    private void centerOnPrimaryScreen() {
        var visualBounds = Screen.getPrimary().getVisualBounds();
        splashStage.setX(visualBounds.getMinX() + (visualBounds.getWidth() - SPLASH_WIDTH) / 2);
        splashStage.setY(visualBounds.getMinY() + (visualBounds.getHeight() - SPLASH_HEIGHT) / 2);
    }
}
