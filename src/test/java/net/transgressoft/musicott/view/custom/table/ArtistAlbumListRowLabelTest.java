package net.transgressoft.musicott.view.custom.table;

import javafx.scene.control.Label;
import javafx.stage.Stage;
import net.transgressoft.musicott.test.FxAudioItems;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.Set;

import static net.transgressoft.commons.music.audio.Artist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the album-info label logic in {@link ArtistAlbumListRow}: related-artists,
 * year, and album-label display rules.
 */
@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
@DisplayName("ArtistAlbumListRow")
class ArtistAlbumListRowLabelTest {

    SimpleAudioItemTableView tableView;

    @Start
    void start(Stage stage) {}

    @BeforeEach
    void setUp() {
        tableView = new SimpleAudioItemTableView(mock(ApplicationEventPublisher.class));
    }

    @Test
    @DisplayName("related-artists label is absent from the layout when all involved artists are the same as the primary artist")
    void relatedArtistsLabelIsAbsentWhenNoOtherArtistsAreInvolved() {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, Set.of(bonobo));

        var row = new ArtistAlbumListRow(bonobo, albumSet("Black Sands", track), tableView, 0);

        // When no other artists are involved the label is removed from the VBox; lookup returns null.
        assertThat(row.lookup("#relatedArtistsLabel")).isNull();
    }

    @Test
    @DisplayName("related-artists label shows 'with <name>' when other artists are involved")
    void relatedArtistsLabelShowsWithPrefixWhenOtherArtistsAreInvolved() {
        Artist bonobo = of("Bonobo");
        Artist guest = of("Erykah Badu");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, Set.of(bonobo, guest));

        var row = new ArtistAlbumListRow(bonobo, albumSet("Black Sands", track), tableView, 0);

        Label relatedArtistsLabel = (Label) row.lookup("#relatedArtistsLabel");
        assertThat(relatedArtistsLabel).isNotNull();
        assertThat(relatedArtistsLabel.getText()).startsWith("with ");
        assertThat(relatedArtistsLabel.getText()).contains("Erykah Badu");
        assertThat(relatedArtistsLabel.getText()).doesNotContain("Bonobo");
    }

    @Test
    @DisplayName("year label shows only the first (lowest) year when tracks in the same album span different years")
    void yearLabelShowsSingleYearWhenTracksHaveDifferentYears() {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track1 = audioItemWithYear("Kiara", bonobo, "Black Sands", (short) 2010);
        ObservableAudioItem track2 = audioItemWithYear("Kiara (Live)", bonobo, "Black Sands", (short) 2012);

        var row = new ArtistAlbumListRow(bonobo, albumSet("Black Sands", track1, track2), tableView, 0);

        Label yearLabel = (Label) row.lookup("#yearLabel");
        assertThat(yearLabel).isNotNull();
        assertThat(yearLabel.getText()).doesNotContain(",");
        assertThat(yearLabel.getText()).isEqualTo("2010");
    }

    @Test
    @DisplayName("album-label label shows a single value when multiple tracks share the same label")
    void albumLabelLabelShowsSingleValueWhenTracksDuplicateTheSameLabel() {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track1 = audioItemWithLabel("Kiara", bonobo, "Black Sands", "Ninja Tune");
        ObservableAudioItem track2 = audioItemWithLabel("Kong", bonobo, "Black Sands", "Ninja Tune");

        var row = new ArtistAlbumListRow(bonobo, albumSet("Black Sands", track1, track2), tableView, 0);

        Label albumLabelLabel = (Label) row.lookup("#albumLabelLabel");
        assertThat(albumLabelLabel).isNotNull();
        assertThat(albumLabelLabel.getText()).doesNotContain(",");
        assertThat(albumLabelLabel.getText()).isEqualTo("Ninja Tune");
    }

    private static AlbumTrackGroup albumSet(String albumName, ObservableAudioItem... tracks) {
        return new AlbumTrackGroup(albumName, List.of(tracks));
    }

    private static ObservableAudioItem audioItem(
            String title,
            Artist artist,
            String albumName,
            Artist albumArtist,
            Set<Artist> artistsInvolved) {
        return FxAudioItems.createFxAudioItem(attributes -> {
            attributes.setTitle(title);
            attributes.setArtist(artist);
            attributes.setAlbum(AudioItemTestFactory.createAlbum(albumName, albumArtist));
            attributes.setTrackNumber((short) 1);
            attributes.setDiscNumber((short) 1);
        }, artistsInvolved);
    }

    private static ObservableAudioItem audioItemWithYear(
            String title,
            Artist artist,
            String albumName,
            short year) {
        return FxAudioItems.createFxAudioItem(attributes -> {
            attributes.setTitle(title);
            attributes.setArtist(artist);
            attributes.setAlbum(AudioItemTestFactory.createAlbum(albumName, artist, false, year));
            attributes.setTrackNumber((short) 1);
            attributes.setDiscNumber((short) 1);
        }, Set.of(artist));
    }

    private static ObservableAudioItem audioItemWithLabel(
            String title,
            Artist artist,
            String albumName,
            String labelName) {
        return FxAudioItems.createFxAudioItem(attributes -> {
            attributes.setTitle(title);
            attributes.setArtist(artist);
            attributes.setAlbum(AudioItemTestFactory.createAlbum(
                    albumName, artist, false, null,
                    net.transgressoft.commons.music.audio.Label.of(labelName)));
            attributes.setTrackNumber((short) 1);
            attributes.setDiscNumber((short) 1);
        }, Set.of(artist));
    }
}
