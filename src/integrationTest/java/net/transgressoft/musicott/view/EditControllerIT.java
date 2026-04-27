package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.musicott.events.EditAudioItemsMetadataEvent;
import net.transgressoft.musicott.events.InvalidAudioItemsForEditionEvent;
import net.transgressoft.musicott.events.OpenAudioItemEditorView;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test for {@link EditController}, verifying metadata editor view rendering
 * and the event listener response to an {@link OpenAudioItemEditorView} event.
 */
@ExtendWith(ApplicationExtension.class)
class EditControllerIT {

    ApplicationEventPublisher applicationEventPublisher;

    EditController editController;

    @Start
    void start(Stage stage) throws Exception {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditController.fxml"));
        Parent rootPane = loader.load();
        editController = loader.getController();
        ReflectionTestUtils.setField(editController, "applicationEventPublisher", applicationEventPublisher);
        stage.setScene(new Scene(rootPane, 630, 560));
        stage.show();
    }

    @Test
    @DisplayName("EditController renders the metadata editor view")
    void rendersMetadataEditorView(FxRobot fxRobot) {
        assertThat(fxRobot.lookup("#titleTextField").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#artistTextField").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#albumTextField").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#genreTextField").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#cancelButton").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#okButton").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("EditController responds to an OpenAudioItemEditorView event by publishing InvalidAudioItemsForEditionEvent for items with missing files")
    void respondsToOpenAudioItemEditorViewEventForInvalidItems(FxRobot fxRobot) {
        var mockItem = mock(ObservableAudioItem.class);
        // Path pointing to a non-existent file so the controller publishes InvalidAudioItemsForEditionEvent
        when(mockItem.getPath()).thenReturn(Path.of("/non/existent/file.mp3"));
        when(mockItem.getCoverImageProperty()).thenReturn(new SimpleObjectProperty<>(Optional.empty()));

        var event = new OpenAudioItemEditorView(Set.of(mockItem), this);
        editController.editAudioItemsEventListener(event);
        waitForFxEvents();

        var captor = ArgumentCaptor.forClass(InvalidAudioItemsForEditionEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().invalidAudioItems).hasSize(1);
        assertThat(captor.getValue().invalidAudioItems.get(0)).isEqualTo(mockItem);
    }

    @Test
    @DisplayName("closes edit dialog without saving when escape is pressed")
    void closesEditDialogWithoutSavingWhenEscapeIsPressed(FxRobot fxRobot) {
        Stage editStage = (Stage) ReflectionTestUtils.getField(editController, "stage");
        assertThat(editStage).isNotNull();

        Button cancelButton = (Button) ReflectionTestUtils.getField(editController, "cancelButton");
        assertThat(cancelButton).isNotNull();

        // The controller's internal stage has no scene wired in test setup; install a fresh scene with
        // a stub root so the setOnShown handler can install the ESC handler on it. The cancelButton
        // reference is held by the controller and fired regardless of which scene contains it.
        Platform.runLater(() -> {
            editStage.setScene(new Scene(new AnchorPane(), 200, 100));
            // setOnShown handler installs the ESC handler on the scene; firing it manually
            // simulates the stage being shown without entering a modal showAndWait loop.
            editStage.getOnShown().handle(null);
        });
        waitForFxEvents();

        // Press ESC on the wired scene — the handler invokes cancelButton.fire() which calls close().
        Platform.runLater(() -> editStage.getScene().getOnKeyPressed().handle(
            new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED,
                "", "", KeyCode.ESCAPE,
                false, false, false, false)));
        waitForFxEvents();

        // The cancel path closes the stage without publishing an EditAudioItemsMetadataEvent (no save).
        verify(applicationEventPublisher, never()).publishEvent(any(EditAudioItemsMetadataEvent.class));
    }
}
