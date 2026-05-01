package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.musicott.services.PlayerService;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test for {@link PlayQueueController}, verifying play queue view rendering and
 * the toggle behavior between play queue and history queue views using a standalone pattern.
 */
@ExtendWith(ApplicationExtension.class)
class PlayQueueControllerIT {

    PlayerService playerService;

    ObservableAudioLibrary audioLibrary;

    PlayQueueController controller;

    ObservableList<TrackQueueRow> historyQueue;

    @Start
    void start(Stage stage) throws Exception {
        playerService = mock(PlayerService.class);
        audioLibrary = mock(ObservableAudioLibrary.class);
        ObservableList<TrackQueueRow> playQueue = FXCollections.observableArrayList();
        historyQueue = FXCollections.observableArrayList();
        when(playerService.getPlayQueueList()).thenReturn(playQueue);
        when(playerService.getHistoryQueueList()).thenReturn(historyQueue);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PlayQueueController.fxml"));
        loader.setControllerFactory(type -> new PlayQueueController(playerService, audioLibrary));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root, 300, 467));
        stage.show();
    }

    @Test
    @DisplayName("PlayQueueController renders play queue view")
    void rendersPlayQueueView() {
        waitForFxEvents();

        verifyThat("#playQueuePane", isVisible());
        verifyThat("#queuesListView", isVisible());
        verifyThat("#titleQueueLabel", isVisible());
        verifyThat("#deleteAllButton", isVisible());
        verifyThat("#historyQueueButton", isVisible());
    }

    @Test
    @DisplayName("PlayQueueController toggles between queue and history queue views")
    void togglesBetweenQueueAndHistoryQueueViews(FxRobot fxRobot) {
        waitForFxEvents();

        Label titleLabel = fxRobot.lookup("#titleQueueLabel").queryAs(Label.class);
        assertThat(titleLabel.getText()).isEqualTo("Play Queue");

        fxRobot.clickOn("#historyQueueButton");
        waitForFxEvents();

        assertThat(titleLabel.getText()).isEqualTo("Recently played");
        ToggleButton historyButton = fxRobot.lookup("#historyQueueButton").queryAs(ToggleButton.class);
        assertThat(historyButton.isSelected()).isTrue();
    }

    @Test
    @DisplayName("PlayQueueController registers a delete handler on rows added to historyQueueList")
    void registersDeleteHandlerOnRowsAddedToHistoryQueueList() {
        // Option B: verify via reflection that the delete-button action handler is set after the
        // listener fires (change.next() must be called for getAddedSubList() to be populated).
        // Option A (robot.lookup("#deleteButton-white")) is skipped because the button starts
        // invisible (visible only on mouse hover) and headless Monocle does not synthesize hover,
        // making scene lookup unreliable in CI.
        ObservableAudioItem item = mock(ObservableAudioItem.class);
        when(item.getTitleProperty()).thenReturn(new SimpleStringProperty("Test Track"));
        var artist = mock(net.transgressoft.commons.music.audio.Artist.class);
        when(artist.getName()).thenReturn("Test Artist");
        when(item.getArtistProperty()).thenReturn(new SimpleObjectProperty<>(artist));
        var album = mock(net.transgressoft.commons.music.audio.Album.class);
        when(album.getName()).thenReturn("Test Album");
        when(item.getAlbumProperty()).thenReturn(new SimpleObjectProperty<>(album));

        // TrackQueueRow extends GridPane and must be created on the FX thread.
        TrackQueueRow[] rowHolder = new TrackQueueRow[1];
        Platform.runLater(() -> {
            rowHolder[0] = new TrackQueueRow(item);
            historyQueue.add(rowHolder[0]);
        });
        waitForFxEvents();

        TrackQueueRow row = rowHolder[0];
        assertThat(historyQueue).containsExactly(row);

        // The historyQueueList listener (now wrapped in while (change.next())) must have called
        // TrackQueueRow.setOnDeleteButtonClickedHandler, which sets a non-null action on the button.
        Button deleteButton = (Button) ReflectionTestUtils.getField(row, "deleteTrackQueueRowButton");
        assertThat(deleteButton).isNotNull();
        assertThat(deleteButton.getOnAction())
                .as("delete handler registered by historyQueueList listener")
                .isNotNull();

        // Fire the action directly to prove the handler removes the row from the list.
        Platform.runLater(() -> deleteButton.getOnAction().handle(null));
        waitForFxEvents();

        assertThat(historyQueue).isEmpty();
    }
}
