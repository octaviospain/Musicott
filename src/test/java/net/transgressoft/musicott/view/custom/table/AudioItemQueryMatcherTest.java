package net.transgressoft.musicott.view.custom.table;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.music.audio.AlbumDetails;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static net.transgressoft.commons.music.audio.Artist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AudioItemQueryMatcher}, the shared search predicate driving the cover-grid
 * navigation views. Covers each matched field, case-insensitivity, and null-field safety.
 */
@DisplayName("AudioItemQueryMatcher")
class AudioItemQueryMatcherTest {

    @Test
    @DisplayName("matches on a title substring")
    void matchesOnTitleSubstring() {
        assertThat(AudioItemQueryMatcher.matches(item("Kiara", of("Bonobo"), Set.of(of("Bonobo")), album("Black Sands", of("Bonobo"))), "kia")).isTrue();
    }

    @Test
    @DisplayName("matches on the primary artist name")
    void matchesOnPrimaryArtist() {
        assertThat(AudioItemQueryMatcher.matches(item("Kiara", of("Bonobo"), Set.of(of("Bonobo")), album("Black Sands", of("Bonobo"))), "bono")).isTrue();
    }

    @Test
    @DisplayName("matches on any involved artist name")
    void matchesOnInvolvedArtist() {
        var item = item("Kiara", of("Bonobo"), Set.of(of("Bonobo"), of("Erykah Badu")), album("Black Sands", of("Bonobo")));
        assertThat(AudioItemQueryMatcher.matches(item, "erykah")).isTrue();
    }

    @Test
    @DisplayName("matches on the album artist name")
    void matchesOnAlbumArtist() {
        var item = item("Kiara", of("Bonobo"), Set.of(of("Bonobo")), album("Black Sands", of("Various Artists")));
        assertThat(AudioItemQueryMatcher.matches(item, "various")).isTrue();
    }

    @Test
    @DisplayName("matches on the album name")
    void matchesOnAlbumName() {
        assertThat(AudioItemQueryMatcher.matches(item("Kiara", of("Bonobo"), Set.of(of("Bonobo")), album("Black Sands", of("Bonobo"))), "sands")).isTrue();
    }

    @Test
    @DisplayName("does not match when no field contains the query")
    void doesNotMatchWhenNoFieldContainsQuery() {
        assertThat(AudioItemQueryMatcher.matches(item("Kiara", of("Bonobo"), Set.of(of("Bonobo")), album("Black Sands", of("Bonobo"))), "zzz")).isFalse();
    }

    @Test
    @DisplayName("matches case-insensitively against the stored field value")
    void matchesCaseInsensitively() {
        assertThat(AudioItemQueryMatcher.matches(item("KIARA", of("BONOBO"), Set.of(of("BONOBO")), album("BLACK SANDS", of("BONOBO"))), "kiara")).isTrue();
    }

    @Test
    @DisplayName("returns false without throwing when artist, involved artists, and album are all null")
    void handlesNullFieldsSafely() {
        var item = mock(ObservableAudioItem.class);
        when(item.getTitle()).thenReturn("Kiara");
        // artist / artistsInvolved / album left unstubbed → null
        assertThat(AudioItemQueryMatcher.matches(item, "zzz")).isFalse();
        assertThat(AudioItemQueryMatcher.matches(item, "kia")).isTrue();
    }

    private static ObservableAudioItem item(String title, Artist artist, Set<Artist> involved, AlbumDetails album) {
        var item = mock(ObservableAudioItem.class);
        when(item.getTitle()).thenReturn(title);
        when(item.getArtist()).thenReturn(artist);
        when(item.getArtistsInvolved()).thenReturn(involved);
        when(item.getAlbum()).thenReturn(album);
        return item;
    }

    private static AlbumDetails album(String name, Artist albumArtist) {
        return AudioItemTestFactory.createAlbum(name, albumArtist);
    }
}
