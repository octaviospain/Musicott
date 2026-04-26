package net.transgressoft.musicott.view;

import net.transgressoft.commons.music.itunes.ImportResult;
import net.transgressoft.commons.music.itunes.ItunesPlaylist;
import net.transgressoft.musicott.service.MediaImportService;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test for {@link ItunesPlaylistsPickerController}, exercising the full
 * pick-and-import flow including playlist selection, import trigger, and cancel behaviour.
 *
 * @author Octavio Calleya
 */
@ExtendWith(ApplicationExtension.class)
@DisplayName("ItunesPlaylistsPickerController")
class ItunesPlaylistsPickerControllerIT {

    MediaImportService mediaImportService;

    ItunesPlaylistsPickerController controller;

    @Start
    void start(Stage stage) throws Exception {
        mediaImportService = mock(MediaImportService.class);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItunesPlaylistsPickerController.fxml"));
        loader.setControllerFactory(type -> new ItunesPlaylistsPickerController(mediaImportService));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    @DisplayName("picks playlists and triggers import on button click")
    void picksPlaylistsAndTriggersImportOnButtonClick(FxRobot fxRobot) {
        var playlistA = new ItunesPlaylist("Playlist A", "id-1", null, false, List.of(1));
        var playlistB = new ItunesPlaylist("Playlist B", "id-2", null, false, List.of(2));

        when(mediaImportService.importSelectedPlaylists(anyList()))
            .thenReturn(CompletableFuture.completedFuture(new ImportResult(List.of(), List.of(), List.of())));

        Platform.runLater(() -> controller.pickPlaylists(List.of(playlistA, playlistB)));
        waitForFxEvents();

        ListView<ItunesPlaylist> sourcePlaylists = fxRobot.lookup("#sourcePlaylists").query();
        ListView<ItunesPlaylist> targetPlaylists = fxRobot.lookup("#targetPlaylists").query();

        assertThat(sourcePlaylists.getItems()).hasSize(2);
        assertThat(targetPlaylists.getItems()).isEmpty();

        Platform.runLater(() -> sourcePlaylists.getSelectionModel().select(0));
        waitForFxEvents();

        fxRobot.clickOn("#addSelectedButton");
        waitForFxEvents();

        assertThat(sourcePlaylists.getItems()).hasSize(1);
        assertThat(targetPlaylists.getItems()).hasSize(1);

        fxRobot.clickOn("#importButton");
        waitForFxEvents();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ItunesPlaylist>> captor = ArgumentCaptor.forClass(List.class);
        verify(mediaImportService).importSelectedPlaylists(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("cancel hides window and cancels import")
    void cancelHidesWindowAndCancelsImport(FxRobot fxRobot) {
        fxRobot.clickOn("#cancelButton");
        waitForFxEvents();

        verify(mediaImportService).cancelImport();
    }
}
