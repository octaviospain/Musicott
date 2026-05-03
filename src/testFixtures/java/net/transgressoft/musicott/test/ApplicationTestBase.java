package net.transgressoft.musicott.test;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

public abstract class ApplicationTestBase<T extends Parent> extends ApplicationTest {

    protected static Stage testStage;

    @BeforeAll
    static void beforeAll() throws Exception {
        testStage = FxToolkit.registerPrimaryStage();
    }

    @BeforeEach
    protected void beforeEach() {
        // Block on a CountDownLatch so the scene mount + show + initial render
        // pulse fully completes on the FX thread before the test body runs.
        // waitForFxEvents() alone is racy when the scene attachment fires
        // skin construction + CSS application that mutate JavaFX's internal
        // listener maps while the test thread is iterating them.
        CountDownLatch mounted = new CountDownLatch(1);
        Platform.runLater(() -> {
            Scene scene = new Scene(javaFxComponent(), 800, 600);
            testStage.setScene(scene);
            testStage.show();
            // Re-post the latch so it counts down only after the show event
            // and any pulses it scheduled have drained on the FX thread.
            Platform.runLater(mounted::countDown);
        });
        try {
            if (!mounted.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Scene mount did not complete within 10s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for scene mount", e);
        }
        waitForFxEvents();
    }

    @AfterAll
    static void afterAll() throws Exception {
        Platform.runLater(() -> {
            if (testStage != null && testStage.isShowing()) {
                testStage.close();
            }
        });
        FxToolkit.cleanupStages();
        waitForFxEvents();
    }

    protected abstract T javaFxComponent();

    @AfterEach
    void tearDown() {
        // Detach the previous scene root so its property listeners and skin
        // internals unbind on the FX thread before the next test mounts a new
        // scene. Without this step, a stale scene's listener-map cleanup can
        // race the next mount, surfacing as JavaFX-internal LinkedHashMap
        // AIOOBE / TreeMap CME failures when the same Stage is reused across
        // test methods. The CountDownLatch ensures the detach has actually
        // completed on the FX thread before this method returns.
        CountDownLatch torndown = new CountDownLatch(1);
        Platform.runLater(() -> {
            if (testStage.isShowing()) {
                testStage.hide();
            }
            testStage.setScene(null);
            // Re-post so the latch counts down only after hide+detach pulses drain.
            Platform.runLater(torndown::countDown);
        });
        try {
            torndown.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        waitForFxEvents();
    }
}
