package net.transgressoft.musicott.view.custom.alerts;

import javafx.scene.control.Alert;
import net.transgressoft.musicott.services.SimpleWebRedirectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for creating pre-configured alert dialogs used throughout the application.
 *
 * @author Octavio Calleya
 */
@Component
public class AlertFactory {

    private final SimpleWebRedirectionService webRedirectionService;

    @Autowired
    public AlertFactory(SimpleWebRedirectionService webRedirectionService) {
        this.webRedirectionService = webRedirectionService;
    }

    public Alert aboutWindowAlert() {
        return new AboutWindowAlert(webRedirectionService);
    }

    public Alert importConfirmationAlert(int importSize) {
        return new ImportConfirmationAlert(importSize);
    }

    public Alert emptyImportAlert() {
        return new EmptyImportAlert();
    }

    /**
     * Creates a confirmation dialog for playlist or folder deletion.
     *
     * @param playlistName        name of the playlist or folder being deleted
     * @param itemCount           number of audio items in the playlist (0 for folders)
     * @param isFolder            whether the target is a folder
     * @param nestedPlaylistCount number of nested playlists (only relevant for folders)
     * @return a CONFIRMATION alert ready to show
     */
    public Alert deletePlaylistConfirmationAlert(String playlistName, int itemCount, boolean isFolder, int nestedPlaylistCount) {
        return new DeletePlaylistConfirmationAlert(playlistName, itemCount, isFolder, nestedPlaylistCount);
    }
}
