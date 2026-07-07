package net.transgressoft.musicott.splash;

import net.transgressoft.musicott.PrimaryStageInitializer;
import net.transgressoft.musicott.events.ExceptionEvent;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Coordinates the splash UI lifecycle from start() through fade-dismiss.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Construct {@link SplashController}, bind it to {@link BootProgressTask},
 *       show the splash on the FX thread.</li>
 *   <li>Spawn a background thread running the boot task; Spring boot + library
 *       load run there, off the FX thread.</li>
 *   <li>On task success: build the main scene via the existing
 *       {@link PrimaryStageInitializer#initializePrimaryStage(Stage)} (FX thread,
 *       inside the {@code setOnSucceeded} callback), enforce the 800ms minimum
 *       display gate via {@link PauseTransition}, then run the 250ms
 *       {@link FadeTransition} on the splash root in parallel with
 *       {@code primaryStage.show()}, and finally hide the splash.</li>
 *   <li>On task failure: bifurcate by {@link BootProgressTask#isSpringContextReady()}.
 *       Pre-Spring failure: log + hide splash + Platform.exit + System.exit(1).
 *       Post-Spring failure: publish {@link ExceptionEvent} via the now-ready
 *       Spring context's event publisher; existing
 *       {@code ErrorDialogController} flow takes over.</li>
 * </ol>
 *
 * <p>This class is NOT a Spring bean. It is constructed by
 * {@code MusicottApplication.start()} before Spring boot starts (in the splash
 * path) so it can show the splash before any blocking I/O.
 */
public class SplashOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(SplashOrchestrator.class);

    static final long MIN_DISPLAY_MS = 800;
    static final Duration FADE_DURATION = Duration.millis(250);

    private final Class<?> applicationClass;

    public SplashOrchestrator(Class<?> applicationClass) {
        this.applicationClass = applicationClass;
    }

    /**
     * Runs the splash + boot + dismiss sequence. Must be called on the FX thread.
     * Returns immediately after starting the background boot Task — the rest of the
     * sequence runs asynchronously via Task callbacks.
     */
    public void orchestrate(Stage primaryStage) {
        SplashController splash = new SplashController();
        BootProgressTask bootTask = new BootProgressTask(applicationClass);

        // Bind BEFORE starting the thread — late binding loses early progress updates
        splash.bindToTask(bootTask);
        splash.show();
        long splashShownAt = System.currentTimeMillis();

        bootTask.setOnSucceeded(e -> {
            // setOnSucceeded fires on the FX thread; safe to touch scene graph.
            ConfigurableApplicationContext context = bootTask.getValue();
            try {
                PrimaryStageInitializer initializer = context.getBean(PrimaryStageInitializer.class);
                initializer.initializePrimaryStage(primaryStage);
            } catch (RuntimeException ex) {
                // Treat scene-build failures the same as post-Spring failures.
                logger.error("Failed to initialize primary stage after Spring boot", ex);
                splash.hide();
                context.publishEvent(new ExceptionEvent(ex, this));
                return;
            }
            String version = BuildVersionReader.read();
            int trackCount = context.getBean(ObservableAudioLibrary.class).getAudioItemsProperty().size();
            long loadMs = System.currentTimeMillis() - splashShownAt;
            logger.info("Musicott {} started — {} track(s) loaded in {} ms", version, trackCount, loadMs);
            scheduleDismiss(splash, primaryStage, splashShownAt);
        });

        bootTask.setOnFailed(e -> {
            Throwable failure = bootTask.getException();
            handleFailure(failure, splash, bootTask);
        });

        Thread bootThread = new Thread(bootTask, "musicott-spring-boot-thread");
        bootThread.setDaemon(false);
        bootThread.start();
    }

    /**
     * Schedules splash dismissal after the minimum display gate. Computes the
     * remaining wait as max(0, MIN_DISPLAY_MS - elapsed). When the gate fires, runs
     * primaryStage.show() and a parallel FadeTransition on the splash root.
     *
     * <p>Ordering note: by the time the gate's {@code setOnFinished} fires,
     * {@link PrimaryStageInitializer#initializePrimaryStage(Stage)} has already
     * executed in the boot Task's {@code setOnSucceeded} callback (the call site of
     * this method). The primary stage's scene is fully laid out — installed via
     * {@code primaryStage.setScene(...)}, with {@code installSceneDrivenMinimumSize}
     * pre-show measurement already done. Therefore {@code primaryStage.show()} only
     * makes the already-laid-out window visible; it does not trigger any further
     * scene-graph work, so the show + fade pair runs cleanly with no layout
     * stutter.
     */
    public void scheduleDismiss(SplashController splash, Stage primaryStage, long splashShownAt) {
        long elapsed = System.currentTimeMillis() - splashShownAt;
        long remaining = computeRemainingMillis(elapsed);

        PauseTransition gate = new PauseTransition(Duration.millis(remaining));
        gate.setOnFinished(e -> {
            primaryStage.show();
            FadeTransition fade = new FadeTransition(FADE_DURATION, splash.getRoot());
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(done -> splash.hide());
            fade.play();
        });
        gate.play();
    }

    /**
     * Pure-math helper for the min-display gate. Extracted as package-private static
     * so the unit test can verify the math without spinning up a JavaFX runtime.
     */
    static long computeRemainingMillis(long elapsedMillis) {
        return Math.max(0, MIN_DISPLAY_MS - elapsedMillis);
    }

    /**
     * Bifurcated failure routing
     *
     * <p>{@link Task#getValue()} returns {@code null} on a failed Task per the JavaFX
     * Task contract, so the post-Spring branch reads the cached context via
     * {@link BootProgressTask#getContextOrNull()} instead. This guarantees that a
     * failure occurring AFTER {@code SpringApplicationBuilder.run()} returned (e.g.
     * during a later stage of {@code call()} or during scene-build inside
     * {@code setOnSucceeded}) still has a live context to publish through.
     */
    public void handleFailure(Throwable failure, SplashController splash, BootProgressTask bootTask) {
        if (bootTask.isSpringContextReady()) {
            ConfigurableApplicationContext context = bootTask.getContextOrNull();
            logger.error("Post-Spring startup failure; routing through ErrorDialogController", failure);
            splash.hide();
            if (context != null) {
                context.publishEvent(new ExceptionEvent(failure, this));
            } else {
                // Defensive: isSpringContextReady() == true but contextRef was never set.
                // This indicates an ordering bug in BootProgressTask — log and fall back to
                // hard exit so the user is not silently stuck on a hidden splash.
                logger.error("isSpringContextReady=true but contextRef is null; hard exit as fallback");
                exitJvm(1);
            }
        } else {
            logger.error("Pre-Spring startup failure; exiting JVM", failure);
            splash.hide();
            exitJvm(1);
        }
    }

    /**
     * Hard-exit hook: tears down the FX toolkit and terminates the JVM with the given
     * status code. Extracted as a {@code protected} seam so tests can subclass and
     * override to verify the exit was reached without actually terminating the test
     * JVM. {@code Platform.exit()} and {@code System.exit(int)} cannot be mocked
     * directly under JDK 24 (the {@code SecurityManager} API is unsupported and
     * {@code java.lang.System} is a bootstrap class beyond Mockito's inline mock
     * maker reach).
     */
    protected void exitJvm(int status) {
        Platform.exit();
        System.exit(status);
    }
}
