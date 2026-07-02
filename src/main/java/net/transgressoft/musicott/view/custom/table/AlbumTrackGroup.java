package net.transgressoft.musicott.view.custom.table;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import java.util.List;

/**
 * A named group of audio items that belong to the same album (or the same disc of a multi-disc
 * album) within the Artists view. The album catalog domain model exposes albums as top-level
 * projections keyed by full album identity, but the Artists view needs a finer, per-disc grouping
 * scoped to a single artist; this lightweight value carries the album name together with the
 * already-grouped tracks the row renders.
 *
 * @param albumName the display name shared by every track in the group
 * @param tracks    the audio items belonging to this album group
 */
public record AlbumTrackGroup(String albumName, List<ObservableAudioItem> tracks) {

    /**
     * Returns the album name shared by the tracks in this group.
     *
     * @return the album name
     */
    public String getAlbumName() {
        return albumName;
    }
}
