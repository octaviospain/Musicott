package net.transgressoft.musicott.view.custom.alerts;

import net.transgressoft.commons.music.itunes.ImportError;
import net.transgressoft.commons.music.itunes.ItunesImportResult;

import javafx.scene.control.TextArea;

/**
 * Alert dialog displaying the results of an iTunes library import operation.
 * Shows counts for imported, skipped, and errored tracks. When errors exist,
 * provides an expandable text area listing each error's track title and message.
 *
 * @author Octavio Calleya
 */
public class ItunesImportResultAlert extends ApplicationAlertBase {

    public ItunesImportResultAlert(ItunesImportResult result) {
        super(AlertType.INFORMATION);
        setTitle("iTunes Import Complete");
        setHeaderText(String.format("Imported: %d    Skipped: %d    Errors: %d    Playlists: %d",
            result.getImportedCount(), result.getSkippedCount(),
            result.getErrorCount(), result.getPlaylistsCreated()));

        if (!result.getErrors().isEmpty()) {
            var errorText = new StringBuilder();
            for (ImportError error : result.getErrors()) {
                errorText.append(error.getTrackTitle())
                         .append(" — ")
                         .append(error.getMessage())
                         .append("\n");
            }
            var errorArea = new TextArea(errorText.toString());
            errorArea.setEditable(false);
            errorArea.setWrapText(true);
            errorArea.setPrefRowCount(10);
            getDialogPane().setExpandableContent(errorArea);
        }
    }
}
