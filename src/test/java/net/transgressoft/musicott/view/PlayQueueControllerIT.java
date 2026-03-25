package net.transgressoft.musicott.view;

import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

/**
 * Integration test for {@link PlayQueueController}, verifying play queue view rendering and
 * the toggle behavior between play queue and history queue views using a standalone pattern.
 */
@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
class PlayQueueControllerIT {

    @Mock
    PlayerController playerController;

    PlayQueueController controller;

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PlayQueueController.fxml"));
        loader.setControllerFactory(type -> new PlayQueueController(playerController));
        Parent root = loader.load();
        controller = loader.getController();

        ObservableList<TrackQueueRow> playQueue = FXCollections.observableArrayList();
        ObservableList<TrackQueueRow> historyQueue = FXCollections.observableArrayList();
        // setPlayQueueList and setHistoryQueueList must be called on the FX thread before
        // the stage is shown so that queuesListView.setItems() has valid list references
        controller.setPlayQueueList(playQueue);
        controller.setHistoryQueueList(historyQueue);

        stage.setScene(new Scene(root, 300, 467));
        stage.show();
    }

    @Test
    @DisplayName("PlayQueueController renders play queue view")
    void rendersPlayQueueView() {
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#playQueuePane", isVisible());
        verifyThat("#queuesListView", isVisible());
        verifyThat("#titleQueueLabel", isVisible());
        verifyThat("#deleteAllButton", isVisible());
        verifyThat("#historyQueueButton", isVisible());
    }

    @Test
    @DisplayName("PlayQueueController toggles between queue and history queue views")
    void togglesBetweenQueueAndHistoryQueueViews(FxRobot fxRobot) {
        WaitForAsyncUtils.waitForFxEvents();

        Label titleLabel = fxRobot.lookup("#titleQueueLabel").queryAs(Label.class);
        assertThat(titleLabel.getText()).isEqualTo("Play Queue");

        fxRobot.clickOn("#historyQueueButton");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(titleLabel.getText()).isEqualTo("Recently played");
        assertThat(controller.isShowingHistoryQueue()).isTrue();
    }
}
