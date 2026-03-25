package net.transgressoft.musicott.view.custom.alerts;

/**
 * Confirmation dialog shown before deleting a playlist or playlist folder.
 *
 * @author Octavio Calleya
 */
public class DeletePlaylistConfirmationAlert extends ApplicationAlertBase {

    private static final String FOLDER_TEXT = "This folder contains %d playlist(s). All nested playlists will be deleted. This action cannot be undone.";
    private static final String PLAYLIST_TEXT = "\"%s\" has %d track(s). This action cannot be undone.";

    public DeletePlaylistConfirmationAlert(String playlistName, int itemCount, boolean isFolder, int nestedPlaylistCount) {
        super(AlertType.CONFIRMATION);
        setTitle("Delete playlist");
        setHeaderText("Delete \"" + playlistName + "\"?");
        if (isFolder) {
            setContentText(String.format(FOLDER_TEXT, nestedPlaylistCount));
        } else {
            setContentText(String.format(PLAYLIST_TEXT, playlistName, itemCount));
        }
    }
}
