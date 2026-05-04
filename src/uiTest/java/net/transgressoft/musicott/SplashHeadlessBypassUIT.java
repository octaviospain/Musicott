package net.transgressoft.musicott;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
@DisplayName("SplashHeadlessBypass")
class SplashHeadlessBypassUIT {

    @Start
    void start(Stage primaryStage) {
        // Empty — this UIT inspects global JVM and JavaFX state (system property +
        // open Window list). No scene graph is needed; the @Start hook only exists
        // because TestFX's ApplicationExtension requires one.
    }

    @Test
    @DisplayName("propagates the splash bypass system property to the test JVM")
    void propagatesTheSplashBypassSystemPropertyToTheTestJvm() {
        // gradle/test-suites.gradle's configureTestFxSystemProperties closure sets
        // musicott.splash.disabled=true unconditionally for every Test task. This
        // assertion fails the moment that line is removed or relocated.
        assertThat(System.getProperty("musicott.splash.disabled"))
                .as("musicott.splash.disabled JVM arg must reach the test JVM via gradle/test-suites.gradle")
                .isEqualTo("true");
        assertThat(Boolean.getBoolean("musicott.splash.disabled"))
                .as("Boolean.getBoolean must read the same value the production start() reads")
                .isTrue();
    }

    @Test
    @DisplayName("opens no splash stage during a TestFX session — splash is fully bypassed")
    void opensNoSplashStageDuringATestFxSession() {
        // Snapshot all open Windows. TestFX's ApplicationExtension installs its own
        // primary Stage which can vary in style across platforms, so discriminating
        // by StageStyle is unreliable; the splash-root style class on the scene root
        // is the only splash-specific signal in the scene graph (matches the E2E
        // discriminator in MusicottApplicationE2E).
        long splashWindowCount = Window.getWindows().stream()
                .map(Window::getScene)
                .filter(scene -> scene != null)
                .map(Scene::getRoot)
                .filter(root -> root != null)
                .filter(root -> root.getStyleClass().contains("splash-root"))
                .count();

        assertThat(splashWindowCount)
                .as("no SplashController scene may be open under -Dmusicott.splash.disabled=true")
                .isEqualTo(0);
    }
}
