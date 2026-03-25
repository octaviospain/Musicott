package net.transgressoft.musicott.view;

import net.transgressoft.musicott.tasks.TaskDemon;

import com.worldsworstsoftware.itunes.ItunesPlaylist;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

/**
 * Integration test for {@link ItunesPlaylistsPickerController}, validating the playlist picker UI
 * rendering and the move-between-lists interaction using a standalone pattern (no Spring context required).
 *
 * @author Octavio Calleya
 */
@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
class ItunesPlaylistsPickerControllerTest {

    @Mock
    TaskDemon taskDemon;

    ItunesPlaylistsPickerController controller;

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItunesPlaylistsPickerController.fxml"));
        loader.setControllerFactory(type -> new ItunesPlaylistsPickerController(taskDemon));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    @DisplayName("ItunesPlaylistsPickerController renders source and target playlist lists")
    void rendersSourceAndTargetPlaylistLists() {
        verifyThat("#sourcePlaylists", isVisible());
        verifyThat("#targetPlaylists", isVisible());
        verifyThat("#addSelectedButton", isVisible());
        verifyThat("#addAllButton", isVisible());
        verifyThat("#removeSelectedButton", isVisible());
        verifyThat("#removeAllButton", isVisible());
        verifyThat("#cancelButton", isVisible());
        verifyThat("#importButton", isVisible());
    }

    @Test
    @DisplayName("ItunesPlaylistsPickerController moves selected playlist from source to target list")
    void movesSelectedPlaylistFromSourceToTargetList(FxRobot fxRobot) {
        ItunesPlaylist playlist = mock(ItunesPlaylist.class);
        when(playlist.getName()).thenReturn("My Playlist");
        when(playlist.getTrackIDs()).thenReturn(List.of());
        when(playlist.getTotalSize()).thenReturn(0L);

        Platform.runLater(() -> controller.pickPlaylists(List.of(playlist)));
        WaitForAsyncUtils.waitForFxEvents();

        ListView<ItunesPlaylist> sourcePlaylists = fxRobot.lookup("#sourcePlaylists").query();
        ListView<ItunesPlaylist> targetPlaylists = fxRobot.lookup("#targetPlaylists").query();

        assertThat(sourcePlaylists.getItems()).hasSize(1);
        assertThat(targetPlaylists.getItems()).isEmpty();

        Platform.runLater(() -> sourcePlaylists.getSelectionModel().select(0));
        WaitForAsyncUtils.waitForFxEvents();

        fxRobot.clickOn("#addSelectedButton");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(sourcePlaylists.getItems()).isEmpty();
        assertThat(targetPlaylists.getItems()).hasSize(1);
        assertThat(targetPlaylists.getItems().get(0)).isEqualTo(playlist);
    }
}
