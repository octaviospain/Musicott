package net.transgressoft.musicott.view;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import net.transgressoft.commons.music.audio.Label;
import net.transgressoft.musicott.test.FxAudioItems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Optional;
import java.util.Set;

import static net.transgressoft.commons.music.audio.Artist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ArtistViewController}.
 */
class ArtistViewControllerTest {

    @Test
    @DisplayName("ArtistViewController returns album sets for artists that only appear as involved artists")
    void returnsAlbumSetsForArtistsThatOnlyAppearAsInvolvedArtists() {
        Artist akkya = of("Akkya");
        Artist triggerLive = of("Trigger Live");
        Artist bonobo = of("Bonobo");
        ObservableAudioItem akkyaTrack = audioItem(
                "Circle (Akkya Remix)",
                triggerLive,
                "Diffusion 7.0",
                of(""),
                21,
                Set.of(triggerLive, akkya));
        ObservableAudioItem bonoboTrack = audioItem(
                "Kiara",
                bonobo,
                "Black Sands",
                bonobo,
                1,
                Set.of(bonobo));

        var audioItems = new SimpleListProperty<>(FXCollections.observableArrayList(akkyaTrack, bonoboTrack));
        var audioLibrary = mock(ObservableAudioLibrary.class);
        when(audioLibrary.getAudioItemsProperty()).thenReturn(audioItems);
        when(audioLibrary.getArtistCatalog(akkya)).thenReturn(Optional.empty());

        var controller = new ArtistViewController(audioLibrary, mock(ApplicationContext.class), mock(net.transgressoft.musicott.search.SearchCoordinator.class));

        assertThat(controller.albumSetsForArtist(akkya).keySet())
                .singleElement()
                .satisfies(albumSet -> {
                    assertThat(albumSet.getAlbumName()).isEqualTo("Diffusion 7.0");
                    assertThat(albumSet.tracks()).containsExactly(akkyaTrack);
                });
    }

    @Test
    @DisplayName("ArtistViewController shows a non-compilation track under its album artist as well as its performer")
    void showsNonCompilationTrackUnderItsAlbumArtist() {
        Artist performer = of("Feature Guy");
        Artist albumArtist = of("Album Owner");
        ObservableAudioItem track = audioItem(
                "Guest Spot", performer, "Owner's Album", albumArtist, 1, Set.of(performer, albumArtist));

        var audioItems = new SimpleListProperty<>(FXCollections.observableArrayList(track));
        var audioLibrary = mock(ObservableAudioLibrary.class);
        when(audioLibrary.getAudioItemsProperty()).thenReturn(audioItems);
        var controller = new ArtistViewController(audioLibrary, mock(ApplicationContext.class), mock(net.transgressoft.musicott.search.SearchCoordinator.class));

        assertThat(controller.albumSetsForArtist(albumArtist).keySet())
                .as("track surfaces under its album artist")
                .singleElement()
                .satisfies(albumSet -> assertThat(albumSet.tracks()).containsExactly(track));
        assertThat(controller.albumSetsForArtist(performer).keySet())
                .as("track still surfaces under its performing artist")
                .singleElement()
                .satisfies(albumSet -> assertThat(albumSet.tracks()).containsExactly(track));
    }

    @Test
    @DisplayName("ArtistViewController does not show a compilation track under its album artist")
    void doesNotShowCompilationTrackUnderItsAlbumArtist() {
        Artist performer = of("Track Artist");
        Artist various = of("Various Artists");
        ObservableAudioItem track = compilationItem(
                "Comp Track", performer, "Best Of 2010", various, Set.of(performer, various));

        var audioItems = new SimpleListProperty<>(FXCollections.observableArrayList(track));
        var audioLibrary = mock(ObservableAudioLibrary.class);
        when(audioLibrary.getAudioItemsProperty()).thenReturn(audioItems);
        var controller = new ArtistViewController(audioLibrary, mock(ApplicationContext.class), mock(net.transgressoft.musicott.search.SearchCoordinator.class));

        assertThat(controller.albumSetsForArtist(performer).keySet())
                .as("compilation track still surfaces under its performer")
                .isNotEmpty();
        assertThat(controller.albumSetsForArtist(various))
                .as("compilation track does not collapse under the Various Artists grouping")
                .isEmpty();
    }

    private static ObservableAudioItem audioItem(
            String title,
            Artist artist,
            String albumName,
            Artist albumArtist,
            int trackNumber,
            Set<Artist> artistsInvolved) {
        return audioItem(title, artist, albumName, albumArtist, trackNumber, artistsInvolved, false);
    }

    private static ObservableAudioItem compilationItem(
            String title,
            Artist artist,
            String albumName,
            Artist albumArtist,
            Set<Artist> artistsInvolved) {
        return audioItem(title, artist, albumName, albumArtist, 1, artistsInvolved, true);
    }

    private static ObservableAudioItem audioItem(
            String title,
            Artist artist,
            String albumName,
            Artist albumArtist,
            int trackNumber,
            Set<Artist> artistsInvolved,
            boolean isCompilation) {
        return FxAudioItems.createFxAudioItem(attributes -> {
            attributes.setTitle(title);
            attributes.setArtist(artist);
            attributes.setAlbum(AudioItemTestFactory.createAlbum(
                    albumName,
                    albumArtist,
                    isCompilation,
                    null,
                    Label.of("Test Label")));
            attributes.setTrackNumber((short) trackNumber);
            attributes.setDiscNumber((short) 1);
        }, artistsInvolved);
    }
}
