package net.transgressoft.musicott.splash;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitFor;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(ApplicationExtension.class)
@DisplayName("SplashController")
class SplashControllerUIT {

    SplashController splashController;

    @Start
    void start(Stage primaryStage) {
        // primaryStage from TestFX is unused — splash creates its own Stage.
        splashController = new SplashController();
        splashController.show();
    }

    @AfterEach
    void closeSplash() {
        if (splashController != null) {
            Platform.runLater(splashController::hide);
            waitForFxEvents();
        }
    }

    @Test
    @DisplayName("shows a TRANSPARENT stage separate from the primary stage")
    void showsATransparentStageSeparateFromThePrimaryStage() {
        Stage splashStage = splashController.splashStage;

        assertThat(splashStage.getStyle()).isEqualTo(StageStyle.TRANSPARENT);
        assertThat(splashStage.isShowing()).isTrue();
        assertThat(splashStage.getWidth()).isEqualTo(480);
        assertThat(splashStage.getHeight()).isEqualTo(320);
        assertThat(splashStage.getIcons())
            .as("splash stage must carry the app icon so the taskbar entry matches the primary window")
            .isNotEmpty();
    }

    @Test
    @DisplayName("places logo version progress bar and status label in the splash root in order")
    void placesLogoVersionProgressBarAndStatusLabelInTheSplashRootInOrder() {
        var children = splashController.splashRoot.getChildren();

        assertThat(children).hasSize(4);
        assertThat(children.get(0)).isInstanceOf(ImageView.class);
        assertThat(((Label) children.get(1)).getText())
            .as("version label is populated by BuildVersionReader.read() at construction")
            .isNotBlank();
        assertThat(children.get(2)).isInstanceOf(ProgressBar.class);
        assertThat(children.get(3)).isInstanceOf(Label.class);
        assertThat(((Label) children.get(3)).getText()).isEmpty();
    }

    @Test
    @DisplayName("binds progress bar and status label to a task progress and message properties")
    void bindsProgressBarAndStatusLabelToATaskProgressAndMessageProperties() throws Exception {
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                updateProgress(0.25, 1.0);
                updateMessage("Loading library…");
                return null;
            }
        };

        Platform.runLater(() -> splashController.bindToTask(task));
        waitForFxEvents();

        new Thread(task, "splash-uit-task").start();

        // Deterministic polling — D-SPLASH-NO-FLAKE-RISK forbids Thread.sleep for
        // timing assertions. WaitForAsyncUtils.waitFor polls the supplied condition
        // and pumps the FX queue while waiting. Both the ProgressBar and the status
        // label are bound to the Task's properties; the condition flips true the
        // moment Task.updateProgress + updateMessage have propagated to the FX thread.
        waitFor(2, TimeUnit.SECONDS,
            () -> splashController.progressBar.getProgress() == 0.25
                && "Loading library…".equals(splashController.statusLabel.getText()));

        assertThat(splashController.progressBar.getProgress()).isEqualTo(0.25);
        assertThat(splashController.statusLabel.getText()).isEqualTo("Loading library…");
    }

    @Test
    @DisplayName("consumes key events on the splash root so the splash is non-interactive")
    void consumesKeyEventsOnTheSplashRootSoTheSplashIsNonInteractive(FxRobot robot) {
        AtomicInteger probeCount = new AtomicInteger(0);
        // Probe runs at the bubbling phase — capture-phase filter must consume the
        // event first, so probe should never fire.
        splashController.splashRoot.addEventHandler(KeyEvent.KEY_PRESSED, e -> probeCount.incrementAndGet());

        Platform.runLater(() -> splashController.splashRoot.requestFocus());
        waitForFxEvents();
        robot.press(KeyCode.SPACE).release(KeyCode.SPACE);
        waitForFxEvents();

        assertThat(probeCount.get())
            .as("KeyEvent filter must consume key presses before any handler sees them")
            .isEqualTo(0);
    }
}
