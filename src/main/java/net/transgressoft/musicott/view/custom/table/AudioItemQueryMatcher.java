package net.transgressoft.musicott.view.custom.table;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.music.audio.Artist;

/**
 * Shared search predicate for audio items, matching a track against a query by title, primary
 * artist, involved artists, album artist, and album name. Used by the cover-grid navigation views
 * (Albums, Genres) so a typed query narrows every grid identically.
 *
 * <p>The query is expected already lower-cased; matching is a case-insensitive substring test.
 *
 * @author Octavio Calleya
 */
public final class AudioItemQueryMatcher {

    private AudioItemQueryMatcher() {
    }

    /**
     * @param audioItem the track to test
     * @param query     the lower-cased search query
     * @return {@code true} when the title, primary artist, any involved artist, the album artist, or
     *         the album name of {@code audioItem} contains {@code query}
     */
    // Defensive null guards — imported tracks from partial catalogs can carry null artist/album or
    // null name fields; one malformed track must not NPE the whole filter.
    @SuppressWarnings("java:S2589")
    public static boolean matches(ObservableAudioItem audioItem, String query) {
        if (contains(audioItem.getTitle(), query)) {
            return true;
        }

        var artist = audioItem.getArtist();
        if (artist != null && contains(artist.getName(), query)) {
            return true;
        }

        var involved = audioItem.getArtistsInvolved();
        if (involved != null && involved.stream().map(Artist::getName).anyMatch(name -> contains(name, query))) {
            return true;
        }

        var album = audioItem.getAlbum();
        if (album == null) {
            return false;
        }
        var albumArtist = album.getAlbumArtist();
        return (albumArtist != null && contains(albumArtist.getName(), query))
                || contains(album.getName(), query);
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
