package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.musicott.services.PlayerService;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Start
    void start(Stage stage) throws Exception {
        playerService = mock(PlayerService.class);
        audioLibrary = mock(ObservableAudioLibrary.class);
        ObservableList<TrackQueueRow> playQueue = FXCollections.observableArrayList();
        ObservableList<TrackQueueRow> historyQueue = FXCollections.observableArrayList();
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
}
