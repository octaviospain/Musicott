package net.transgressoft.musicott;

import net.transgressoft.musicott.view.EditController;
import net.transgressoft.musicott.view.ErrorDialogController;
import net.transgressoft.musicott.view.LogViewerController;
import net.transgressoft.musicott.view.MainController;

import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.testfx.api.FxToolkit;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.asyncFx;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test for {@link PrimaryStageInitializer}, pinning the auxiliary-view prewarm
 * contract. Windows that are opened later purely in response to an application event (the log
 * viewer) depend on their FXML being loaded here so their {@code @FXML} fields — including the
 * scene root — are injected into the Spring singleton before the window is built.
 */
@DisplayName("PrimaryStageInitializer")
class PrimaryStageInitializerIT {

    @BeforeAll
    static void initFx() throws Exception {
        FxToolkit.registerPrimaryStage();
    }

    @AfterAll
    static void cleanUp() throws Exception {
        FxToolkit.cleanupStages();
    }

    @Test
    @DisplayName("prewarms every auxiliary view, including the log viewer, without throwing")
    void prewarmsEveryAuxiliaryViewIncludingTheLogViewerWithoutThrowing() throws Exception {
        FxWeaver fxWeaver = mock(FxWeaver.class);
        when(fxWeaver.loadView(any())).thenAnswer(invocation -> new AnchorPane());
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        PrimaryStageInitializer initializer = new PrimaryStageInitializer(fxWeaver, publisher);

        AtomicReference<Throwable> failure = new AtomicReference<>();
        asyncFx(() -> {
            try {
                initializer.initializePrimaryStage(new Stage());
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        waitForFxEvents();

        assertThat(failure.get()).as("primary stage initialization must not throw").isNull();
        verify(fxWeaver).loadView(ErrorDialogController.class);
        verify(fxWeaver).loadView(EditController.class);
        verify(fxWeaver).loadView(LogViewerController.class);
        verify(fxWeaver).loadView(MainController.class);
    }
}
