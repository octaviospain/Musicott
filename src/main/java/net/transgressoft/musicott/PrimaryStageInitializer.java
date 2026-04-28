package net.transgressoft.musicott;

import net.transgressoft.musicott.events.StageReadyEvent;
import net.transgressoft.musicott.events.StopApplicationEvent;
import net.transgressoft.musicott.view.ErrorDialogController;
import net.transgressoft.musicott.view.MainController;
import net.transgressoft.musicott.view.PreferencesController;
import net.transgressoft.musicott.view.custom.ApplicationImage;

import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Bootstraps the primary JavaFX stage once Spring publishes {@link StageReadyEvent}.
 *
 * <p>The initializer (1) prewarms the auxiliary FXML views via FxWeaver so the first user
 * interaction does not pay the FXML parsing cost on the FX thread, (2) builds the main
 * scene, (3) wires graceful shutdown through {@link StopApplicationEvent}, and
 * (4) installs scene-derived minimum-size constraints with a chrome-compensated re-apply
 * after the stage is shown.
 */
@Component
public class PrimaryStageInitializer implements ApplicationListener<StageReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(PrimaryStageInitializer.class);

    /**
     * Empirical floor for the stage's minimum height. Window managers that under-report
     * decoration overhead (e.g. Wayland compositors using client-side decorations) make
     * the chrome compensation in {@link #installSceneDrivenMinimumSize} compute zero,
     * which would otherwise let the user shrink the stage until the player area clips.
     */
    private static final double MIN_HEIGHT_FLOOR = 800.0;

    private final FxWeaver fxWeaver;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public PrimaryStageInitializer(FxWeaver fxWeaver, ApplicationEventPublisher applicationEventPublisher) {
        this.fxWeaver = fxWeaver;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage primaryStage = event.primaryStage;

        prewarmAuxiliaryViews();
        Scene scene = new Scene(fxWeaver.loadView(MainController.class));

        wirePublishStopApplicationOnClose(primaryStage);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(ApplicationImage.APP_ICON.get());
        installSceneDrivenMinimumSize(primaryStage, scene);

        primaryStage.show();
    }

    /**
     * Pre-loads the auxiliary FXML views so their controllers and node trees are cached
     * by FxWeaver before the user can trigger them. Without this, the first
     * {@code preferencesController.show()} or error-dialog raise would block on FXML
     * parsing and node construction on the FX thread.
     */
    private void prewarmAuxiliaryViews() {
        fxWeaver.loadView(PreferencesController.class);
        fxWeaver.loadView(ErrorDialogController.class);
    }

    /**
     * Installs a close handler that publishes {@link StopApplicationEvent} so listeners
     * (notably the background-task coordinator and persistence flushers) can perform
     * graceful shutdown before the JVM exits.
     */
    private void wirePublishStopApplicationOnClose(Stage stage) {
        stage.setOnCloseRequest(e -> applicationEventPublisher.publishEvent(new StopApplicationEvent(this)));
    }

    /**
     * Constrains the stage so the user cannot resize it smaller than the scene's natural
     * content minimum, in two passes:
     * <ol>
     *   <li><b>Pre-show:</b> measures the scene root's natural minimum width/height (via a
     *       forced layout pass — {@code minWidth/minHeight(-1)} return 0 on an unlaid
     *       root) and applies them as a provisional stage minimum. Window-manager chrome
     *       is unknown at this point.</li>
     *   <li><b>Post-show ({@code setOnShown}):</b> measures the actual chrome overhead as
     *       {@code stage.size − scene.size} and re-applies the minimum to include it, so
     *       the constraint reflects the real visible content area.</li>
     * </ol>
     *
     * <p>The {@link #MIN_HEIGHT_FLOOR} guards against window managers that report zero
     * chrome overhead.
     */
    private void installSceneDrivenMinimumSize(Stage stage, Scene scene) {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        double sceneMinWidth = scene.getRoot().minWidth(-1);
        double sceneMinHeight = scene.getRoot().minHeight(-1);

        stage.setMinWidth(sceneMinWidth);
        stage.setMinHeight(Math.max(sceneMinHeight, MIN_HEIGHT_FLOOR));

        stage.setOnShown(shown -> {
            double chromeWidth = Math.max(0, stage.getWidth() - scene.getWidth());
            double chromeHeight = Math.max(0, stage.getHeight() - scene.getHeight());
            stage.setMinWidth(sceneMinWidth + chromeWidth);
            stage.setMinHeight(Math.max(sceneMinHeight + chromeHeight, MIN_HEIGHT_FLOOR));
            logger.info("Stage min size set: width={} (scene {} + chrome {}), height={} (scene {} + chrome {}, floor {})",
                stage.getMinWidth(), sceneMinWidth, chromeWidth,
                stage.getMinHeight(), sceneMinHeight, chromeHeight, MIN_HEIGHT_FLOOR);
        });
    }
}
