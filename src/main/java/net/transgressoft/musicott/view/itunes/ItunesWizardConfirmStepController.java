package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.itunes.ItunesPlaylist;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Controller;

/**
 * Read-only step that recaps the user's choices before the import is dispatched. Renders
 * four sections — library path, picked playlists with per-playlist track-count subtotals,
 * three policy lines in plain language, and an aggregate track total. Every section is
 * rebuilt on each {@code onEnter} so a Back-navigation that mutates an earlier step is
 * always reflected here.
 *
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/ItunesWizardConfirmStep.fxml")
@Controller
public class ItunesWizardConfirmStepController {

    private final SimpleBooleanProperty invalid = new SimpleBooleanProperty(this, "invalid", false);

    @FXML
    Label libraryPathLabel;
    @FXML
    VBox playlistsList;
    @FXML
    VBox policyList;
    @FXML
    Label totalLabel;

    private ItunesImportDraft draft;

    /**
     * Binds this step controller to the supplied wizard draft. Called by the wizard owner
     * once per wizard session, before the first page is rendered.
     */
    void bind(ItunesImportDraft draft) {
        this.draft = draft;
    }

    /**
     * Refreshes all four recap sections from the current draft. Called every time this
     * step becomes the current page so a Back-navigation that changed any earlier step
     * is always reflected on screen.
     */
    void onEnter() {
        if (draft == null) {
            return;
        }
        libraryPathLabel.setText(draft.libraryPath != null ? draft.libraryPath.toString() : "");

        playlistsList.getChildren().clear();
        int totalTracks = 0;
        for (ItunesPlaylist playlist : draft.selectedPlaylists) {
            int count = playlist.getTrackIds().size();
            totalTracks += count;
            String tracksWord = count == 1 ? "track" : "tracks";
            playlistsList.getChildren().add(new Label("• " + playlist.getName() + " — " + count + " " + tracksWord));
        }

        policyList.getChildren().clear();
        policyList.getChildren().add(new Label(draft.useFileMetadata
                ? "• Read metadata from the audio files"
                : "• Use the metadata recorded in the iTunes library"));
        policyList.getChildren().add(new Label(draft.holdPlayCount
                ? "• Preserve existing play counts"
                : "• Reset play counts on import"));
        policyList.getChildren().add(new Label(draft.writeMetadata
                ? "• Write metadata back to audio files"
                : "• Don't write metadata back to files"));

        int playlistCount = draft.selectedPlaylists.size();
        String tracksWord = totalTracks == 1 ? "track" : "tracks";
        String playlistsWord = playlistCount == 1 ? "playlist" : "playlists";
        totalLabel.setText("Importing " + totalTracks + " " + tracksWord
                + " across " + playlistCount + " " + playlistsWord);
    }

    public ReadOnlyBooleanProperty invalidProperty() {
        return invalid;
    }
}
