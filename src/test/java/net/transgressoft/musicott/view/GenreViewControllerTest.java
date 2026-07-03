package net.transgressoft.musicott.view;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import net.transgressoft.musicott.search.SearchCoordinator;
import net.transgressoft.musicott.test.FxAudioItems;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableGenreIndex;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import net.transgressoft.commons.music.audio.Label;
import net.transgressoft.musicott.view.custom.table.AlbumTrackGroup;
import org.junit.jupiter.api.BeforeEach;
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
 * Unit tests for {@link GenreViewController}, covering the album-grouped, genre-filtered section
 * builder ({@link GenreViewController#buildGenreSections}) in isolation — no JavaFX toolkit required.
 *
 * @author Octavio Calleya
 */
class GenreViewControllerTest {

    GenreViewController controller;

    @BeforeEach
    void setUp() {
        controller = new GenreViewController(mock(ObservableAudioLibrary.class), mock(ApplicationContext.class), mock(SearchCoordinator.class));
    }

    @Test
    @DisplayName("GenreViewController groups genre tracks into one section for a single-disc album")
    void groupsGenreTracksIntoOneSectionForSingleDiscAlbum() {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track1 = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 1);
        ObservableAudioItem track2 = audioItem("Kong", bonobo, "Black Sands", bonobo, 2, Set.of(bonobo), 1);

        ObservableGenreIndex genre = mockGenre(track1, track2);

        List<Map.Entry<AlbumTrackGroup, Integer>> sections = controller.buildGenreSections(genre);

        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).getValue()).isEqualTo(0);
        assertThat(sections.get(0).getKey().getAlbumName()).isEqualTo("Black Sands");
        assertThat(sections.get(0).getKey().tracks()).containsExactly(track1, track2);
    }

    @Test
    @DisplayName("GenreViewController splits a multi-disc album into one section per disc")
    void splitsTwoDiscAlbumIntoOneSectionPerDisc() {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem disc1Track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 1);
        ObservableAudioItem disc2Track = audioItem("Stay the Same", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 2);

        ObservableGenreIndex genre = mockGenre(disc1Track, disc2Track);

        List<Map.Entry<AlbumTrackGroup, Integer>> sections = controller.buildGenreSections(genre);

        assertThat(sections).hasSize(2);
        assertThat(sections.stream().map(Map.Entry::getValue).toList()).containsExactly(1, 2);
        assertThat(sections.get(0).getKey().tracks()).containsExactly(disc1Track);
        assertThat(sections.get(1).getKey().tracks()).containsExactly(disc2Track);
    }

    @Test
    @DisplayName("GenreViewController normalizes null and zero disc numbers to a single disc-0 section")
    void normalizesNullAndZeroDiscNumbersToSingleSection() {
        Artist bonobo = of("Bonobo");
        // discNumber=0 stored by the factory becomes null when retrieved (values <= 0 are not stored)
        ObservableAudioItem zeroDiscTrack = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 0);
        ObservableAudioItem anotherZeroDiscTrack = audioItem("Kong", bonobo, "Black Sands", bonobo, 2, Set.of(bonobo), 0);

        ObservableGenreIndex genre = mockGenre(zeroDiscTrack, anotherZeroDiscTrack);

        List<Map.Entry<AlbumTrackGroup, Integer>> sections = controller.buildGenreSections(genre);

        // Both tracks normalize to disc 1 → single-disc album → disc key 0 (no disc label)
        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).getValue()).isEqualTo(0);
        assertThat(sections.get(0).getKey().tracks()).containsExactlyInAnyOrder(zeroDiscTrack, anotherZeroDiscTrack);
    }

    @Test
    @DisplayName("GenreViewController preserves only the genre-tagged tracks for a partial album")
    void preservesOnlyGenreTaggedTracksForPartialAlbum() {
        Artist bonobo = of("Bonobo");
        // Feed only 3 of a hypothetical 12-track album — the builder must not re-aggregate
        ObservableAudioItem track1 = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 1);
        ObservableAudioItem track2 = audioItem("Kong", bonobo, "Black Sands", bonobo, 3, Set.of(bonobo), 1);
        ObservableAudioItem track3 = audioItem("Terrapin", bonobo, "Black Sands", bonobo, 7, Set.of(bonobo), 1);

        ObservableGenreIndex genre = mockGenre(track1, track2, track3);

        List<Map.Entry<AlbumTrackGroup, Integer>> sections = controller.buildGenreSections(genre);

        assertThat(sections).hasSize(1);
        // The section holds exactly the 3 fed tracks — no re-aggregation to the full album
        assertThat(sections.get(0).getKey().tracks()).hasSize(3);
        assertThat(sections.get(0).getKey().tracks()).containsExactlyInAnyOrder(track1, track2, track3);
    }

    @Test
    @DisplayName("GenreViewController collects album-less genre tracks into a trailing 'Unknown Album' section at disc 0")
    void collectsAlbumlessTracksIntoTrailingUnknownAlbumSection() {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem albumlessTrack1 = audioItemNoAlbum("Track Without Album", bonobo, 1, Set.of(bonobo));
        ObservableAudioItem albumlessTrack2 = audioItemNoAlbum("Another Albumless Track", bonobo, 2, Set.of(bonobo));

        ObservableGenreIndex genre = mockGenre(albumlessTrack1, albumlessTrack2);

        List<Map.Entry<AlbumTrackGroup, Integer>> sections = controller.buildGenreSections(genre);

        assertThat(sections).hasSize(1);
        Map.Entry<AlbumTrackGroup, Integer> unknownSection = sections.get(0);
        assertThat(unknownSection.getKey().getAlbumName()).isEqualTo(GenreViewController.UNKNOWN_ALBUM);
        assertThat(unknownSection.getValue()).isEqualTo(0);
        assertThat(unknownSection.getKey().tracks()).containsExactlyInAnyOrder(albumlessTrack1, albumlessTrack2);
    }

    @Test
    @DisplayName("GenreViewController places 'Unknown Album' section last when mixing album and album-less tracks")
    void placesUnknownAlbumSectionLastWhenMixingAlbumAndAlbumlessTracks() {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem albumTrack = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 1);
        ObservableAudioItem albumlessTrack = audioItemNoAlbum("Orphan Track", bonobo, 99, Set.of(bonobo));

        ObservableGenreIndex genre = mockGenre(albumTrack, albumlessTrack);

        List<Map.Entry<AlbumTrackGroup, Integer>> sections = controller.buildGenreSections(genre);

        assertThat(sections).hasSize(2);
        assertThat(sections.get(0).getKey().getAlbumName()).isEqualTo("Black Sands");
        assertThat(sections.get(1).getKey().getAlbumName()).isEqualTo(GenreViewController.UNKNOWN_ALBUM);
        assertThat(sections.get(1).getValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("GenreViewController orders album sections by natural album-name string order")
    void ordersAlbumSectionsByNaturalAlbumNameOrder() {
        Artist bonobo = of("Bonobo");
        // Deliberately feed albums out of alphabetical order
        ObservableAudioItem zephyrTrack = audioItem("Zephyr", bonobo, "Zebra Album", bonobo, 1, Set.of(bonobo), 1);
        ObservableAudioItem apexTrack = audioItem("Apex", bonobo, "Apex Album", bonobo, 1, Set.of(bonobo), 1);
        ObservableAudioItem midTrack = audioItem("Middle", bonobo, "Middle Album", bonobo, 1, Set.of(bonobo), 1);
        ObservableAudioItem albumlessTrack = audioItemNoAlbum("Orphan", bonobo, 1, Set.of(bonobo));

        ObservableGenreIndex genre = mockGenre(zephyrTrack, apexTrack, midTrack, albumlessTrack);

        List<Map.Entry<AlbumTrackGroup, Integer>> sections = controller.buildGenreSections(genre);

        assertThat(sections).hasSize(4);
        assertThat(sections.get(0).getKey().getAlbumName()).isEqualTo("Apex Album");
        assertThat(sections.get(1).getKey().getAlbumName()).isEqualTo("Middle Album");
        assertThat(sections.get(2).getKey().getAlbumName()).isEqualTo("Zebra Album");
        // "Unknown Album" is always last regardless of alphabetical position
        assertThat(sections.get(3).getKey().getAlbumName()).isEqualTo(GenreViewController.UNKNOWN_ALBUM);
    }

    private static ObservableGenreIndex mockGenre(ObservableAudioItem... tracks) {
        var genre = mock(ObservableGenreIndex.class);
        when(genre.getTracksProperty())
                .thenReturn(new SimpleListProperty<>(FXCollections.observableArrayList(tracks)));
        return genre;
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

    private static ObservableAudioItem audioItemNoAlbum(
            String title,
            Artist artist,
            int trackNumber,
            Set<Artist> artistsInvolved) {
        return FxAudioItems.createFxAudioItem(attributes -> {
            attributes.setTitle(title);
            attributes.setArtist(artist);
            // A blank album name triggers the album-less partition in the section builder
            attributes.setAlbum(AudioItemTestFactory.createAlbum("", artist, false, null, Label.of("Test Label")));
            attributes.setTrackNumber((short) trackNumber);
        }, artistsInvolved);
    }
}
