package net.transgressoft.musicott.view;

import net.transgressoft.musicott.test.FxAudioItems;
import net.transgressoft.commons.fx.music.audio.ObservableAlbum;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import net.transgressoft.commons.music.audio.Label;
import net.transgressoft.musicott.view.custom.table.AlbumTrackGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.transgressoft.commons.music.audio.Artist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AlbumViewController}, covering the disc-section grouping logic
 * ({@link AlbumViewController#buildAlbumSections}) in isolation — no JavaFX toolkit required.
 */
class AlbumViewControllerTest {

    @Test
    @DisplayName("AlbumViewController builds one section for a single-disc album")
    void buildsOneSectionForSingleDiscAlbum() {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track1 = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 1);
        ObservableAudioItem track2 = audioItem("Kong", bonobo, "Black Sands", bonobo, 2, Set.of(bonobo), 1);

        ObservableAlbum album = mock(ObservableAlbum.class);
        when(album.getTracks()).thenReturn(List.of(track1, track2));
        when(album.getAlbumName()).thenReturn("Black Sands");

        var controller = new AlbumViewController(mock(ObservableAudioLibrary.class), mock(ApplicationContext.class), mock(net.transgressoft.musicott.search.SearchCoordinator.class));
        List<Map.Entry<AlbumTrackGroup, Integer>> sections = controller.buildAlbumSections(album);

        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).getValue()).isEqualTo(0);
        assertThat(sections.get(0).getKey().getAlbumName()).isEqualTo("Black Sands");
        assertThat(sections.get(0).getKey().tracks()).containsExactly(track1, track2);
    }

    @Test
    @DisplayName("AlbumViewController builds one section per disc for a two-disc album")
    void buildsOneSectionPerDiscForTwoDiscAlbum() {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem disc1Track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 1);
        ObservableAudioItem disc2Track = audioItem("Stay the Same", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 2);

        ObservableAlbum album = mock(ObservableAlbum.class);
        when(album.getTracks()).thenReturn(List.of(disc1Track, disc2Track));
        when(album.getAlbumName()).thenReturn("Black Sands");

        var controller = new AlbumViewController(mock(ObservableAudioLibrary.class), mock(ApplicationContext.class), mock(net.transgressoft.musicott.search.SearchCoordinator.class));
        List<Map.Entry<AlbumTrackGroup, Integer>> sections = controller.buildAlbumSections(album);

        assertThat(sections).hasSize(2);
        var discKeys = sections.stream().map(Map.Entry::getValue).toList();
        assertThat(discKeys).containsExactly(1, 2);
        assertThat(sections.get(0).getKey().tracks()).containsExactly(disc1Track);
        assertThat(sections.get(1).getKey().tracks()).containsExactly(disc2Track);
    }

    @Test
    @DisplayName("AlbumViewController normalizes null or zero disc numbers to disc 1")
    void normalizesNullOrZeroDiscNumbersToDiscOne() {
        Artist bonobo = of("Bonobo");
        // discNumber=0 normalizes to 1; discNumber=null normalizes to 1
        ObservableAudioItem zeroDiscTrack = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 0);
        ObservableAudioItem nullDiscTrack = audioItem("Kong", bonobo, "Black Sands", bonobo, 2, Set.of(bonobo), 0);

        ObservableAlbum album = mock(ObservableAlbum.class);
        when(album.getTracks()).thenReturn(List.of(zeroDiscTrack, nullDiscTrack));
        when(album.getAlbumName()).thenReturn("Black Sands");

        var controller = new AlbumViewController(mock(ObservableAudioLibrary.class), mock(ApplicationContext.class), mock(net.transgressoft.musicott.search.SearchCoordinator.class));
        List<Map.Entry<AlbumTrackGroup, Integer>> sections = controller.buildAlbumSections(album);

        // Both tracks normalize to disc 1 → single-disc album → one section with disc key 0
        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).getValue()).isEqualTo(0);
        assertThat(sections.get(0).getKey().tracks()).containsExactlyInAnyOrder(zeroDiscTrack, nullDiscTrack);
    }

    private static ObservableAudioItem audioItem(
            String title,
            Artist artist,
            String albumName,
            Artist albumArtist,
            int trackNumber,
            Set<Artist> artistsInvolved,
            int discNumber) {
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
            attributes.setDiscNumber((short) discNumber);
        }, artistsInvolved);
    }
}
