package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.itunes.ItunesLibrary;
import net.transgressoft.musicott.events.ExceptionEvent;
import net.transgressoft.musicott.service.MediaImportService;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Unit test for {@link ItunesWizardLibraryStepController} validating the file-picker step
 * UI rendering and the validity gating against {@link MediaImportService#parseLibrary} using
 * a standalone pattern (no Spring context required).
 *
 * @author Octavio Calleya
 */
@ExtendWith({MockitoExtension.class, ApplicationExtension.class})
@DisplayName("ItunesWizardLibraryStepController")
class ItunesWizardLibraryStepControllerTest {

    @Mock
    MediaImportService mediaImportService;
    @Mock
    ApplicationEventPublisher applicationEventPublisher;
    @Mock
    ItunesLibrary parsedLibrary;

    ItunesWizardLibraryStepController controller;
    ItunesImportDraft draft;

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItunesWizardLibraryStep.fxml"));
        loader.setControllerFactory(type -> new ItunesWizardLibraryStepController(mediaImportService, applicationEventPublisher));
        Parent root = loader.load();
        controller = loader.getController();
        draft = new ItunesImportDraft();
        controller.bind(draft);
        stage.setScene(new Scene(root));
        stage.show();
        waitForFxEvents();
    }

    @Test
    @DisplayName("ItunesWizardLibraryStepController renders the choose-file button and selected-path label")
    void rendersChooseFileButtonAndSelectedPathLabel() {
        verifyThat("#chooseFileButton", isVisible());
        verifyThat("#selectedFileLabel", isVisible());
    }

    @Test
    @DisplayName("ItunesWizardLibraryStepController is invalid until a library file is parsed")
    void isInvalidUntilLibraryFileIsParsed() {
        assertThat(controller.invalidProperty().get()).isTrue();
    }

    @Test
    @DisplayName("ItunesWizardLibraryStepController parses chosen file and flips invalidProperty to false")
    void parsesChosenFileAndFlipsInvalidPropertyToFalse() {
        Path xmlPath = Path.of("/fixture/library.xml");
        when(mediaImportService.parseLibrary(xmlPath)).thenReturn(parsedLibrary);

        Platform.runLater(() -> controller.acceptLibraryFile(xmlPath));
        waitForFxEvents();

        verify(mediaImportService).parseLibrary(xmlPath);
        assertThat(draft.libraryPath).isEqualTo(xmlPath);
        assertThat(draft.parsedLibrary).isSameAs(parsedLibrary);
        assertThat(controller.invalidProperty().get()).isFalse();
    }

    @Test
    @DisplayName("ItunesWizardLibraryStepController publishes ExceptionEvent and stays invalid when parse fails")
    void publishesExceptionEventAndStaysInvalidWhenParseFails() {
        Path bad = Path.of("/bad/library.xml");
        RuntimeException boom = new RuntimeException("malformed");
        when(mediaImportService.parseLibrary(bad)).thenThrow(boom);

        Platform.runLater(() -> controller.acceptLibraryFile(bad));
        waitForFxEvents();

        ArgumentCaptor<ExceptionEvent> captor = ArgumentCaptor.forClass(ExceptionEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().exception).isSameAs(boom);
        assertThat(draft.libraryPath).isNull();
        assertThat(draft.parsedLibrary).isNull();
        assertThat(controller.invalidProperty().get()).isTrue();
    }
}
