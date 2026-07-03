package net.transgressoft.musicott.view;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import net.transgressoft.musicott.test.FxAudioItems;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import net.transgressoft.commons.music.audio.Label;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.List;
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

    private static ObservableAudioItem audioItem(
            String title,
            Artist artist,
            String albumName,
            Artist albumArtist,
            int trackNumber,
            Set<Artist> artistsInvolved) {
        return FxAudioItems.createFxAudioItem(attributes -> {
            attributes.setTitle(title);
            attributes.setArtist(artist);
            attributes.setAlbum(AudioItemTestFactory.createAlbum(
                    albumName,
                    albumArtist,
                    false,
                    null,
                    Label.of("Test Label")));
            attributes.setTrackNumber((short) trackNumber);
            attributes.setDiscNumber((short) 1);
        }, artistsInvolved);
    }
}
