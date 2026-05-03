package net.transgressoft.musicott.view.custom.alerts;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuBar;
import net.transgressoft.commons.music.itunes.ImportResult;
import net.transgressoft.musicott.services.SimpleWebRedirectionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

/**
 * Factory for creating pre-configured alert dialogs used throughout the application.
 *
 * @author Octavio Calleya
 */
@Component
public class AlertFactory {

    private final SimpleWebRedirectionService webRedirectionService;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @Autowired
    public AlertFactory(SimpleWebRedirectionService webRedirectionService,
                        ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.webRedirectionService = webRedirectionService;
        this.buildPropertiesProvider = buildPropertiesProvider;
    }

    public Alert aboutWindowAlert() {
        return new AboutWindowAlert(webRedirectionService, buildPropertiesProvider.getIfAvailable());
    }

    /**
     * Builds a self-maintaining keyboard shortcuts help dialog populated by walking the supplied menu bar.
     *
     * @param rootMenuBar the live application menu bar whose accelerators populate the dialog
     * @return a modal dialog displaying every menu item with an accelerator
     */
    public Dialog<Void> keyboardShortcutsDialog(MenuBar rootMenuBar) {
        return new KeyboardShortcutsDialog(rootMenuBar);
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

    /**
     * Creates a dialog displaying iTunes import results with an expandable detail list
     * of unresolved tracks and rejected playlist names.
     *
     * @param result the import result containing imported items, unresolved tracks, and rejected playlist names
     * @return an INFORMATION alert with result summary
     */
    public Alert itunesImportResultAlert(ImportResult result) {
        return new ItunesImportResultAlert(result);
    }

    public Alert importInProgressAlert() {
        return new ImportInProgressAlert();
    }
}
