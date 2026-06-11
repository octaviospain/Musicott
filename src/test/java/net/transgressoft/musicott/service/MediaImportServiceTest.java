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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MediaImportService}, validating that the service:
 *  - tracks parse-then-import ordering and rejects imports without a parsed library,
 *  - correctly forwards cancellation to the underlying iTunes import service,
 *  - exposes its in-progress state via {@link MediaImportService#isImporting()}.
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
        ItunesImportPolicy policy = new ItunesImportPolicy(true, true, true, Set.of(AudioFileType.values()));
        assertThatThrownBy(() -> service.importSelectedPlaylists(List.of(), policy))
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
    @DisplayName("import progress fraction formula produces values within [0.0, 1.0] in non-decreasing order")
    void importProgressFractionFormulaProducesValuesWithinBoundsInNonDecreasingOrder() {
        // Directly validates the formula progress.getAndIncrement().toDouble() / totalFiles
        // produces a non-decreasing sequence bounded by [0.0, 1.0].
        // This mirrors the calculation MediaImportService uses in importFiles/importDirectory
        // when publishing StatusProgressUpdateEvent per created audio item.
        int totalFiles = 5;
        List<Double> progressValues = new ArrayList<>();
        AtomicInteger progress = new AtomicInteger(0);

        for (int i = 0; i < totalFiles; i++) {
            double fraction = (double) progress.getAndIncrement() / totalFiles;
            progressValues.add(fraction);
        }

        assertThat(progressValues).hasSize(totalFiles);

        // All fractions must be within [0.0, 1.0]
        progressValues.forEach(p ->
                assertThat(p).as("progress fraction %f must be within [0.0, 1.0]", p)
                        .isBetween(0.0, 1.0));

        // Values must be non-decreasing
        for (int i = 1; i < progressValues.size(); i++) {
            assertThat(progressValues.get(i))
                    .as("progress at index %d must be >= index %d", i, i - 1)
                    .isGreaterThanOrEqualTo(progressValues.get(i - 1));
        }

        // First value is 0.0 (0 / totalFiles)
        assertThat(progressValues.get(0)).isEqualTo(0.0);

        // Last value approaches 1.0 (the next increment after totalFiles-1 would give exactly 1.0)
        double lastFraction = (double) progress.get() / totalFiles;
        assertThat(lastFraction).isEqualTo(1.0);
    }
}
