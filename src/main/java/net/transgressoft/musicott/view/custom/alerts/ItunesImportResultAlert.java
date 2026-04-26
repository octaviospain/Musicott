package net.transgressoft.musicott.view.custom.alerts;

import net.transgressoft.commons.music.itunes.ImportResult;
import net.transgressoft.commons.music.itunes.RejectedPlaylistName;
import net.transgressoft.commons.music.itunes.UnresolvedReason;
import net.transgressoft.commons.music.itunes.UnresolvedTrack;

import javafx.scene.control.TextArea;

/**
 * Alert dialog displaying the results of an iTunes library import operation.
 * Reports four counts in the header — imported tracks, skipped tracks (file not
 * found or unsupported type), errored tracks, and rejected playlist names — and
 * shows an expandable text area listing every unresolved track and rejected
 * playlist with its reason when any non-imported items exist.
 *
 * @author Octavio Calleya
 */
public class ItunesImportResultAlert extends ApplicationAlertBase {

    public ItunesImportResultAlert(ImportResult result) {
        super(AlertType.INFORMATION);
        setTitle("iTunes Import Complete");

        int imported = result.getImported().size();
        int skipped = 0;
        int errored = 0;
        for (UnresolvedTrack unresolved : result.getUnresolved()) {
            UnresolvedReason reason = unresolved.getReason();
            if (reason instanceof UnresolvedReason.ImportError) {
                errored++;
            } else {
                skipped++;
            }
        }
        int rejectedPlaylists = result.getRejectedPlaylistNames().size();

        setHeaderText(String.format("Imported: %d    Skipped: %d    Errors: %d    Rejected playlists: %d",
            imported, skipped, errored, rejectedPlaylists));

        if (!result.getUnresolved().isEmpty() || !result.getRejectedPlaylistNames().isEmpty()) {
            var details = new StringBuilder();
            for (UnresolvedTrack unresolved : result.getUnresolved()) {
                details.append(unresolved.getTitle())
                       .append(" — ")
                       .append(unresolved.getReason())
                       .append("\n");
            }
            for (RejectedPlaylistName rejected : result.getRejectedPlaylistNames()) {
                details.append("[playlist] ")
                       .append(rejected.getName())
                       .append(" — ")
                       .append(rejected.getReason())
                       .append("\n");
            }
            var detailsArea = new TextArea(details.toString());
            detailsArea.setEditable(false);
            detailsArea.setWrapText(true);
            detailsArea.setPrefRowCount(10);
            getDialogPane().setExpandableContent(detailsArea);
        }
    }
}
