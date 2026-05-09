package net.transgressoft.musicott.test;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.music.audio.*;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test fixtures for artist-view controller scenarios.
 */
public final class ArtistViewTestFixtures {

    private ArtistViewTestFixtures() {
    }

    public static Album album(String name, Artist albumArtist, String labelName) {
        return new ImmutableAlbum(name, albumArtist, false, null, ImmutableLabel.of(labelName));
    }

    public static ObservableAudioItem audioItem(
            String title,
            Artist artist,
            Album album,
            Set<Artist> artistsInvolved,
            int trackNumber) {
        var item = mock(ObservableAudioItem.class);
        var titleProperty = new SimpleStringProperty(title);
        var artistProperty = new SimpleObjectProperty<>(artist);
        var albumProperty = new SimpleObjectProperty<>(album);
        var genresProperty = new SimpleObjectProperty<Set<Genre>>(Set.of());
        var commentsProperty = new SimpleStringProperty("");
        var trackNumberProperty = new SimpleIntegerProperty(trackNumber);
        var discNumberProperty = new SimpleIntegerProperty(0);
        var bpmProperty = new SimpleFloatProperty(0);
        var coverImageProperty = new SimpleObjectProperty<Optional<Image>>(Optional.empty());
        var artistsInvolvedProperty = new SimpleSetProperty<>(
                FXCollections.observableSet(new HashSet<>(artistsInvolved)));

        when(item.getTitle()).thenReturn(title);
        when(item.getTitleProperty()).thenReturn(titleProperty);
        when(item.getArtist()).thenReturn(artist);
        when(item.getArtistProperty()).thenReturn(artistProperty);
        when(item.getAlbum()).thenReturn(album);
        when(item.getAlbumProperty()).thenReturn(albumProperty);
        when(item.getGenres()).thenReturn(Set.of());
        when(item.getGenresProperty()).thenReturn(genresProperty);
        when(item.getComments()).thenReturn("");
        when(item.getCommentsProperty()).thenReturn(commentsProperty);
        when(item.getTrackNumber()).thenReturn((short) trackNumber);
        when(item.getTrackNumberProperty()).thenReturn(trackNumberProperty);
        when(item.getDiscNumber()).thenReturn(null);
        when(item.getDiscNumberProperty()).thenReturn(discNumberProperty);
        when(item.getBpm()).thenReturn(null);
        when(item.getBpmProperty()).thenReturn(bpmProperty);
        when(item.getCoverImageProperty()).thenReturn(coverImageProperty);
        when(item.getArtistsInvolved()).thenReturn(artistsInvolved);
        when(item.getArtistsInvolvedProperty()).thenReturn(artistsInvolvedProperty);
        when(item.getPath()).thenReturn(Path.of("/tmp/" + title.replaceAll("[^a-zA-Z0-9]+", "-") + ".mp3"));
        when(item.getFileName()).thenReturn(title + ".mp3");
        when(item.getExtension()).thenReturn("mp3");
        return item;
    }
}
