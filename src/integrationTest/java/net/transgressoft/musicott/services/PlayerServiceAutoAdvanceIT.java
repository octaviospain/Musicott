package net.transgressoft.musicott.services;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import net.transgressoft.commons.fx.music.player.FXAudioItemPlayer;
import net.transgressoft.commons.music.player.AudioItemPlayer;
import net.transgressoft.musicott.events.AudioItemChangedEvent;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test asserting that {@link PlayerService#next()} correctly advances
 * to the next queued track and publishes an {@link AudioItemChangedEvent} — covering
 * the regression where a READY (post-natural-end) player status caused {@code play()}
 * to no-op and the queue advance to stall silently.
 */
@ExtendWith(ApplicationExtension.class)
@DisplayName("PlayerService auto-advance")
class PlayerServiceAutoAdvanceIT {

    ApplicationEventPublisher applicationEventPublisher;
    PlayerService playerService;

    @Start
    void start(Stage stage) {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        playerService = new PlayerService(applicationEventPublisher);
    }

    @Test
    @DisplayName("next on a non-empty queue advances currentTrack to the next item and shrinks the queue by one")
    void nextOnNonEmptyQueueAdvancesCurrentTrackAndShrinksQueue() throws Exception {
        ObservableAudioItem currentItem = newPlayableAudioItem("Current");
        ObservableAudioItem nextItem = newPlayableAudioItem("Next");

        ReflectionTestUtils.setField(playerService, "currentTrack", Optional.of(currentItem));
        playerService.addToQueue(List.of(nextItem));

        ObservableList<TrackQueueRow> queue = playerService.getPlayQueueList();
        assertThat(queue).hasSize(1);

        // Intercept FXAudioItemPlayer construction so the test stays headless on all platforms.
        // The status property is set to READY to simulate the post-natural-end state that
        // previously caused play() to no-op — the regression scenario for this fix.
        try (MockedConstruction<FXAudioItemPlayer> ignored = Mockito.mockConstruction(FXAudioItemPlayer.class,
                (mockPlayer, context) -> {
                    Mockito.doNothing().when(mockPlayer).play(Mockito.any());
                    Mockito.when(mockPlayer.getStatusProperty())
                            .thenReturn(new SimpleObjectProperty<>(AudioItemPlayer.Status.READY));
                })) {
            playerService.next();
        }

        // Pump the JavaFX thread — statusProperty is posted via Platform.runLater.
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(playerService.currentTrack()).contains(nextItem);
        assertThat(queue).isEmpty();
    }

    @Test
    @DisplayName("next on a non-empty queue publishes AudioItemChangedEvent carrying the next track")
    void nextOnNonEmptyQueuePublishesAudioItemChangedEventWithNextTrack() throws Exception {
        ObservableAudioItem currentItem = newPlayableAudioItem("Current");
        ObservableAudioItem nextItem = newPlayableAudioItem("Next");

        // Reset the shared mock so only events from this test are captured.
        Mockito.reset(applicationEventPublisher);

        ReflectionTestUtils.setField(playerService, "currentTrack", Optional.of(currentItem));
        playerService.addToQueue(List.of(nextItem));

        try (MockedConstruction<FXAudioItemPlayer> ignored = Mockito.mockConstruction(FXAudioItemPlayer.class,
                (mockPlayer, context) -> {
                    Mockito.doNothing().when(mockPlayer).play(Mockito.any());
                    Mockito.when(mockPlayer.getStatusProperty())
                            .thenReturn(new SimpleObjectProperty<>(AudioItemPlayer.Status.READY));
                })) {
            playerService.next();
        }

        WaitForAsyncUtils.waitForFxEvents();

        ArgumentCaptor<org.springframework.context.ApplicationEvent> eventCaptor =
                ArgumentCaptor.forClass(org.springframework.context.ApplicationEvent.class);
        verify(applicationEventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());

        boolean audioItemChangedFired = eventCaptor.getAllValues().stream()
                .filter(e -> e instanceof AudioItemChangedEvent)
                .map(e -> (AudioItemChangedEvent) e)
                .anyMatch(e -> e.currentTrack == nextItem);
        assertThat(audioItemChangedFired)
                .as("AudioItemChangedEvent carrying the next track must be published on auto-advance")
                .isTrue();
    }

    @Test
    @DisplayName("swapping players detaches the previous player's status listener so a late STOPPED never clears the new track")
    void previousPlayerStatusListenerDetachedOnSwap() throws Exception {
        ObservableAudioItem first = newPlayableAudioItem("First");
        ObservableAudioItem second = newPlayableAudioItem("Second");

        Mockito.reset(applicationEventPublisher);

        // Capture each constructed player's status property in construction order so the test can
        // later mutate the *previous* player's status and assert its listener was removed.
        List<SimpleObjectProperty<AudioItemPlayer.Status>> statusProps = new java.util.ArrayList<>();

        ReflectionTestUtils.setField(playerService, "currentTrack", Optional.of(first));
        playerService.addToQueue(List.of(second));

        try (MockedConstruction<FXAudioItemPlayer> ignored = Mockito.mockConstruction(FXAudioItemPlayer.class,
                (mockPlayer, context) -> {
                    Mockito.doNothing().when(mockPlayer).play(Mockito.any());
                    var statusProperty = new SimpleObjectProperty<>(AudioItemPlayer.Status.READY);
                    statusProps.add(statusProperty);
                    Mockito.when(mockPlayer.getStatusProperty()).thenReturn(statusProperty);
                })) {
            // Build the first player and attach its status listener.
            playerService.next();
            WaitForAsyncUtils.waitForFxEvents();
            // Swap to a fresh player; this must detach the first player's status listener.
            playerService.play(first);
            WaitForAsyncUtils.waitForFxEvents();
        }

        // Simulate the swapped-out player's delayed STOPPED transition.
        Mockito.clearInvocations(applicationEventPublisher);
        statusProps.get(0).set(AudioItemPlayer.Status.STOPPED);
        WaitForAsyncUtils.waitForFxEvents();

        ArgumentCaptor<org.springframework.context.ApplicationEvent> eventCaptor =
                ArgumentCaptor.forClass(org.springframework.context.ApplicationEvent.class);
        verify(applicationEventPublisher, Mockito.atLeast(0)).publishEvent(eventCaptor.capture());

        boolean stoppedPublishedAfterSwap = eventCaptor.getAllValues().stream()
                .anyMatch(e -> e instanceof net.transgressoft.musicott.events.PlaybackStatusChangedEvent p
                        && p.status == AudioItemPlayer.Status.STOPPED);
        assertThat(stoppedPublishedAfterSwap)
                .as("the detached previous player must not emit STOPPED after being swapped out")
                .isFalse();
    }

    @SuppressWarnings("unchecked")
    ObservableAudioItem newPlayableAudioItem(String title) throws Exception {
        ObservableAudioItem item = mock(ObservableAudioItem.class);
        Path tempFile = Files.createTempFile("musicott-advance-it-", ".mp3");
        tempFile.toFile().deleteOnExit();
        when(item.getPath()).thenReturn(tempFile);
        when(item.getExtension()).thenReturn("mp3");
        when(item.getTitleProperty()).thenReturn(new SimpleStringProperty(title));

        var artist = mock(net.transgressoft.commons.music.audio.Artist.class);
        when(artist.getName()).thenReturn("Test Artist");
        when(item.getArtistProperty()).thenReturn(new SimpleObjectProperty<>(artist));

        var album = AudioItemTestFactory.createAlbum("Test Album");
        when(item.getAlbumProperty()).thenReturn(new SimpleObjectProperty<>(album));

        when(item.getCoverImageProperty()).thenReturn(new SimpleObjectProperty<>(Optional.empty()));
        when(item.getPlayCountProperty()).thenReturn(new SimpleIntegerProperty(0));
        when(item.getDuration()).thenReturn(Duration.ofSeconds(5));
        return item;
    }
}
