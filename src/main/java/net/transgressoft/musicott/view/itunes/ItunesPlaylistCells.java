package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.itunes.ItunesPlaylist;

import javafx.scene.control.TreeCell;
import javafx.scene.control.cell.CheckBoxTreeCell;

/**
 * Shared cell factories for iTunes playlist tree views. Both the playlist-selection step
 * ({@link ItunesWizardPlaylistsStepController}) and the confirm-step read-only tree
 * ({@link ItunesWizardConfirmStepController}) must render nodes identically: folder nodes
 * display the name only; leaf nodes display {@code name  (X tracks)} with correct
 * singular/plural. Centralising the text logic here prevents the two renderers from
 * drifting apart over time.
 *
 * @author Octavio Calleya
 */
final class ItunesPlaylistCells {

    private ItunesPlaylistCells() {}

    /**
     * Returns the display text for a playlist node: folder playlists show the name only;
     * leaf playlists show {@code name  (X track[s])} with correct singular/plural.
     *
     * @param item the playlist to format; must not be {@code null}
     * @return the formatted display string
     */
    static String displayText(ItunesPlaylist item) {
        if (item.isFolder()) {
            return item.getName();
        }
        int trackCount = item.getTrackIds().size();
        String unit = trackCount == 1 ? "track" : "tracks";
        return item.getName() + "  (" + trackCount + " " + unit + ")";
    }

    /**
     * A {@link CheckBoxTreeCell} that formats each cell using {@link #displayText(ItunesPlaylist)}.
     * Used by the playlist-selection step so checkboxes are preserved while sharing the text logic.
     */
    static final class SelectionTreeCell extends CheckBoxTreeCell<ItunesPlaylist> {

        @Override
        public void updateItem(ItunesPlaylist item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(displayText(item));
            }
        }
    }

    /**
     * A plain (non-checkbox) {@link TreeCell} that formats each cell using
     * {@link #displayText(ItunesPlaylist)}. Used by the read-only confirm-step tree.
     */
    static final class ReadOnlyTreeCell extends TreeCell<ItunesPlaylist> {

        @Override
        public void updateItem(ItunesPlaylist item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(displayText(item));
            }
        }
    }
}
