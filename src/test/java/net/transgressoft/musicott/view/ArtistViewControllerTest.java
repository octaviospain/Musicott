package net.transgressoft.musicott.view;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.musicott.test.ArtistViewTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Optional;
import java.util.Set;

import static net.transgressoft.commons.music.audio.ImmutableArtist.of;
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
        ObservableAudioItem akkyaTrack = ArtistViewTestFixtures.audioItem(
                "Circle (Akkya Remix)",
                triggerLive,
                ArtistViewTestFixtures.album("Diffusion 7.0", of(""), "Gastspiel Records"),
                Set.of(triggerLive, akkya),
                21);
        ObservableAudioItem bonoboTrack = ArtistViewTestFixtures.audioItem(
                "Kiara",
                bonobo,
                ArtistViewTestFixtures.album("Black Sands", bonobo, "Ninja Tune"),
                Set.of(bonobo),
                1);

        var audioItems = new SimpleListProperty<>(FXCollections.observableArrayList(akkyaTrack, bonoboTrack));
        var audioLibrary = mock(ObservableAudioLibrary.class);
        when(audioLibrary.getAudioItemsProperty()).thenReturn(audioItems);
        when(audioLibrary.getArtistCatalog(akkya)).thenReturn(Optional.empty());

        var controller = new ArtistViewController(audioLibrary, mock(ApplicationContext.class));

        assertThat(controller.albumSetsForArtist(akkya))
                .singleElement()
                .satisfies(albumSet -> {
                    assertThat(albumSet.getAlbumName()).isEqualTo("Diffusion 7.0");
                    assertThat((java.util.List<ObservableAudioItem>) albumSet).containsExactly(akkyaTrack);
                });
    }
}
