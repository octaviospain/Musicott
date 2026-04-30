package net.transgressoft.musicott.service;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.AudioFileType;
import net.transgressoft.commons.music.itunes.ItunesImportPolicy;
import net.transgressoft.musicott.view.custom.alerts.AlertFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MediaImportService}, covering iTunes import lifecycle,
 * mutual exclusion state, cancellation behaviour, and the hardcoded
 * {@link ItunesImportPolicy} returned by the internal policy builder.
 *
 * @author Octavio Calleya
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MediaImportService")
class MediaImportServiceTest {

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @Mock
    ObservableAudioLibrary audioLibrary;

    @Mock
    FXMusicLibrary fxMusicLibrary;

    @Mock
    AlertFactory alertFactory;

    MediaImportService service;

    @BeforeEach
    void setUp() {
        when(fxMusicLibrary.audioLibrary()).thenReturn(audioLibrary);
        service = new MediaImportService(applicationEventPublisher, fxMusicLibrary, alertFactory);
    }

    @Test
    @DisplayName("returns null from getLastParsedLibrary when no library has been parsed yet")
    void returnsNullWhenNoLibraryParsed() {
        assertThat(service.getLastParsedLibrary()).isNull();
    }

    @Test
    @DisplayName("throws IllegalStateException on importSelectedPlaylists when no library has been parsed")
    void throwsWhenImportCalledBeforeParse() {
        assertThatThrownBy(() -> service.importSelectedPlaylists(List.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No iTunes library parsed");
    }

    @Test
    @DisplayName("cancelImport does not throw when no import is in progress")
    void cancelImportDoesNotThrowWhenNoImportInProgress() {
        service.cancelImport();
        assertThat(service.getLastParsedLibrary()).isNull();
    }

    @Test
    @DisplayName("isImporting returns false initially")
    void isImportingReturnsFalseInitially() {
        assertThat(service.isImporting()).isFalse();
    }

    @Test
    @DisplayName("applies hardcoded ItunesImportPolicy defaults — useFileMetadata, holdPlayCount, writeMetadata true and all 4 supported AudioFileTypes")
    void appliesHardcodedItunesImportPolicyDefaults() throws Exception {
        Method buildPolicy = MediaImportService.class.getDeclaredMethod("buildPolicy");
        buildPolicy.setAccessible(true);
        ItunesImportPolicy policy = (ItunesImportPolicy) buildPolicy.invoke(service);

        assertThat(policy.getUseFileMetadata()).isTrue();
        assertThat(policy.getHoldPlayCount()).isTrue();
        assertThat(policy.getWriteMetadata()).isTrue();
        assertThat(policy.getAcceptedFileTypes())
            .containsExactlyInAnyOrder(AudioFileType.values());
    }
}
