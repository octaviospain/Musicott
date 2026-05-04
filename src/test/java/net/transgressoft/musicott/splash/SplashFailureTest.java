package net.transgressoft.musicott.splash;

import net.transgressoft.musicott.events.ExceptionEvent;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(ApplicationExtension.class)
@DisplayName("SplashFailure")
class SplashFailureTest {

    SplashController splash;

    @Start
    void start(Stage stage) {
        splash = new SplashController();
        splash.show();
    }

    @AfterEach
    void closeSplash() {
        if (splash != null) {
            Platform.runLater(splash::hide);
            waitForFxEvents();
        }
    }

    @Test
    @DisplayName("pre-Spring failure invokes exitJvm with status code 1")
    void preSpringFailureInvokesExitJvmWithStatusCode1() {
        // SplashOrchestrator.exitJvm is the protected seam over Platform.exit() +
        // System.exit(int). Both calls cannot be intercepted with Mockito under
        // JDK 24 (SecurityManager API is unsupported; java.lang.System is a
        // bootstrap class beyond MockedStatic's reach). Subclass-override is the
        // only safe pattern that works without terminating the test JVM.
        AtomicInteger capturedStatus = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger callCount = new AtomicInteger(0);
        SplashOrchestrator orchestrator = new SplashOrchestrator(Object.class) {
            @Override protected void exitJvm(int status) {
                capturedStatus.set(status);
                callCount.incrementAndGet();
            }
        };

        BootProgressTask task = new BootProgressTask(Object.class);
        // task.isSpringContextReady() is false because call() never ran.

        Platform.runLater(() ->
            orchestrator.handleFailure(new RuntimeException("pre-spring boom"), splash, task));
        waitForFxEvents();

        assertThat(callCount.get())
            .as("pre-Spring failure must invoke exitJvm exactly once")
            .isEqualTo(1);
        assertThat(capturedStatus.get())
            .as("pre-Spring failure must call exitJvm(1)")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("post-Spring failure publishes ExceptionEvent through the cached context")
    void postSpringFailurePublishesExceptionEventThroughTheCachedContext() {
        // Defensive unit test: even when the boot Task FAILS after Spring is ready,
        // handleFailure must reach a live context. Task.getValue() returns null on a
        // failed Task per the JavaFX contract — so we override getContextOrNull() to
        // return our stub. If handleFailure ever reverted to getValue(), this test
        // would fail because publishEvent would never be invoked.
        SplashOrchestrator orchestrator = new SplashOrchestrator(Object.class);
        ConfigurableApplicationContext stubContext = Mockito.mock(ConfigurableApplicationContext.class);
        BootProgressTask task = new BootProgressTask(Object.class) {
            @Override public boolean isSpringContextReady() { return true; }
            @Override public ConfigurableApplicationContext getContextOrNull() { return stubContext; }
        };
        RuntimeException postSpringFailure = new RuntimeException("post-spring scene-build boom");

        Platform.runLater(() ->
            orchestrator.handleFailure(postSpringFailure, splash, task));
        waitForFxEvents();

        ArgumentCaptor<ExceptionEvent> captor = ArgumentCaptor.forClass(ExceptionEvent.class);
        Mockito.verify(stubContext).publishEvent(captor.capture());
        assertThat(captor.getValue().exception)
            .as("ExceptionEvent must wrap the original Throwable that caused the post-Spring failure")
            .isSameAs(postSpringFailure);
    }
}
