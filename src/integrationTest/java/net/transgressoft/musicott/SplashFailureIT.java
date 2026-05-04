package net.transgressoft.musicott;

import net.transgressoft.musicott.events.ExceptionEvent;
import net.transgressoft.musicott.splash.BootProgressTask;
import net.transgressoft.musicott.splash.SplashController;
import net.transgressoft.musicott.splash.SplashOrchestrator;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test verifying that post-Spring failures route through the
 * {@code ApplicationEventPublisher} as {@link ExceptionEvent} — the existing
 * {@code ErrorDialogController} flow takes over from there.
 *
 * <p>Pre-Spring failures (which call {@code Platform.exit + System.exit(1)}) are
 * covered by the unit-level {@code SplashFailureTest}.
 */
@JavaFxSpringTest(classes = SplashFailureITConfiguration.class)
@DisplayName("SplashFailure")
class SplashFailureIT extends ApplicationTestBase<Pane> {

    @Autowired
    ExceptionEventCaptor captor;

    @Autowired
    ConfigurableApplicationContext applicationContext;

    SplashController splash;
    SplashOrchestrator orchestrator;

    @Override
    protected Pane javaFxComponent() {
        return new Pane();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        captor.reset();
        Platform.runLater(() -> {
            orchestrator = new SplashOrchestrator(MusicottApplication.class);
            splash = new SplashController();
            splash.show();
        });
        waitForFxEvents();
    }

    @AfterEach
    void closeSplash() {
        if (splash != null) {
            Platform.runLater(splash::hide);
            waitForFxEvents();
        }
    }

    @Test
    @DisplayName("publishes ExceptionEvent when boot task fails after Spring context construction")
    void publishesExceptionEventWhenBootTaskFailsAfterSpringContextConstruction() {
        // Construct a task that already considers Spring ready by overriding the two
        // accessors directly. handleFailure now calls getContextOrNull() (NOT
        // getValue()) so that a failed Task — for which JavaFX returns null from
        // getValue() — can still route the ExceptionEvent through a live context that
        // was cached before the failure happened.
        BootProgressTask task = new BootProgressTask(MusicottApplication.class) {
            @Override public boolean isSpringContextReady() { return true; }
            @Override public ConfigurableApplicationContext getContextOrNull() { return applicationContext; }
        };

        RuntimeException postSpringFailure = new RuntimeException("scene-build boom");
        Platform.runLater(() -> orchestrator.handleFailure(postSpringFailure, splash, task));
        waitForFxEvents();

        assertThat(captor.captured.get())
            .as("post-Spring failure must publish an ExceptionEvent")
            .isNotNull();
        assertThat(captor.captured.get().exception).isSameAs(postSpringFailure);
    }
}

@JavaFxSpringTestConfiguration
class SplashFailureITConfiguration {

    @Bean
    ExceptionEventCaptor exceptionEventCaptor() {
        return new ExceptionEventCaptor();
    }
}

@Component
class ExceptionEventCaptor {

    AtomicReference<ExceptionEvent> captured = new AtomicReference<>();

    @EventListener
    void onExceptionEvent(ExceptionEvent event) {
        captured.set(event);
    }

    void reset() {
        captured.set(null);
    }
}
