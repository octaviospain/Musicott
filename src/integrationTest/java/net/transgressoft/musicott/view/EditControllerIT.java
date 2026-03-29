package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.musicott.events.InvalidAudioItemsForEditionEvent;
import net.transgressoft.musicott.events.OpenAudioItemEditorView;

import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
}
