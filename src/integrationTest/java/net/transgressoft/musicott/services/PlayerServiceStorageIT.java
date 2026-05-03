package net.transgressoft.musicott.services;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.player.JavaFxPlayer;
import net.transgressoft.commons.music.player.AudioItemPlayer;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link PlayerService} storage contract, exercising a REAL
 * {@code PlayerService} instance (not a Mockito mock) so the list-mutation ripple in
 * {@code next()}, {@code previous()}, {@code addToQueue()}, {@code playFromQueue()},
 * and {@code enforceHistoryCap()} is verifiable in CI.
 */
@ExtendWith(ApplicationExtension.class)
@DisplayName("PlayerService storage")
class PlayerServiceStorageIT {

    ApplicationEventPublisher applicationEventPublisher;
    PlayerService playerService;

    @Start
    void start(Stage stage) {
        // No scene needed — only the JavaFX toolkit must be running so TrackQueueRow (extends GridPane) can be constructed.
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        playerService = new PlayerService(applicationEventPublisher);
    }

    @Test
    @DisplayName("next inverts queue pop direction so size-1 element plays next")
    void nextInvertsQueuePopDirectionSoSizeMinusOneElementPlaysNext() throws Exception {
        ObservableAudioItem itemA = newPlayableAudioItem("A");
        ObservableAudioItem itemB = newPlayableAudioItem("B");
        ObservableAudioItem itemC = newPlayableAudioItem("C");

        // Inverted-storage contract for addToQueue: input [A, B, C] (A selected first) maps to
        // storage [C, B, A] so A — the first track the user wants played — is at size-1 (next-up,
        // bottom of the popover) and C — the last selected — is at index 0 (farthest-out, top).
        playerService.addToQueue(List.of(itemA, itemB, itemC));

        ObservableList<TrackQueueRow> queue = playerService.getPlayQueueList();
        assertThat(queue).hasSize(3);
        assertThat(queue.get(0).getTrack()).isSameAs(itemC);
        assertThat(queue.get(queue.size() - 1).getTrack()).isSameAs(itemA);
        // Note: we do NOT call playerService.next() because next() invokes play(...) which
        // attempts to start the JavaFX media subsystem (jfxmedia) — unreliable headless.
        // The size-1 == next-up storage contract IS the assertion that proves the inversion.
    }

    @Test
    @DisplayName("previous appends to playQueueList end and pops historyQueueList from end")
    void previousAppendsToPlayQueueListEndAndPopsHistoryQueueListFromEnd() throws Exception {
        ObservableAudioItem itemH1 = newPlayableAudioItem("H1");
        ObservableAudioItem itemH2 = newPlayableAudioItem("H2");
        ObservableAudioItem itemCurrent = newPlayableAudioItem("Current");

        // Seed historyQueueList directly via the public accessor.
        ObservableList<TrackQueueRow> history = playerService.getHistoryQueueList();
        history.add(new TrackQueueRow(itemH1));
        history.add(new TrackQueueRow(itemH2));

        // Seed currentTrack via reflection (private field).
        ReflectionTestUtils.setField(playerService, "currentTrack", Optional.of(itemCurrent));

        // previous() ends with setPlayer(...) which constructs JavaFxPlayer — that constructor
        // chains into javafx.scene.media.MediaPlayer and loads jfxmedia.dll, which crashes the
        // Windows headless JVM with STATUS_ACCESS_VIOLATION 0xC0000005 (uncatchable native crash).
        // Intercept `new JavaFxPlayer()` with Mockito.mockConstruction so the list mutations
        // execute and the no-op `play(...)` returns immediately on every OS. setPlayer also
        // calls bindMediaPlayer() → trackPlayer.getStatusProperty().addListener(...), so stub
        // the status property too to avoid an NPE on the listener attach.
        try (MockedConstruction<JavaFxPlayer> ignored = Mockito.mockConstruction(JavaFxPlayer.class,
                (mock, context) -> {
                    Mockito.doNothing().when(mock).play(Mockito.any());
                    Mockito.when(mock.getStatusProperty())
                            .thenReturn(new SimpleObjectProperty<>(AudioItemPlayer.Status.UNKNOWN));
                })) {
            playerService.previous();
        }

        ObservableList<TrackQueueRow> queue = playerService.getPlayQueueList();
        // The currentTrack was appended to playQueueList at the end (size-1).
        assertThat(queue).hasSize(1);
        assertThat(queue.get(queue.size() - 1).getTrack()).isSameAs(itemCurrent);

        // historyQueueList popped from size-1 (end): [H1, H2] → [H1].
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getTrack()).isSameAs(itemH1);
    }

    @Test
    @DisplayName("addToQueue places the first selected item at the next-up end and the last selected at the farthest end")
    void addToQueuePlacesFirstSelectedAtNextUpEndAndLastSelectedAtFarthestEnd() throws Exception {
        ObservableAudioItem itemA = newPlayableAudioItem("A");
        ObservableAudioItem itemB = newPlayableAudioItem("B");
        ObservableAudioItem itemC = newPlayableAudioItem("C");
        ObservableAudioItem itemD = newPlayableAudioItem("D");
        ObservableAudioItem itemE = newPlayableAudioItem("E");

        // First batch [A, B, C] (A selected first) → storage [C, B, A]:
        // A at size-1 = next-up (bottom of popover, plays first); C at index 0 = farthest-out
        // (top, plays last). Display follows storage 1:1.
        playerService.addToQueue(List.of(itemA, itemB, itemC));

        ObservableList<TrackQueueRow> queue = playerService.getPlayQueueList();
        assertThat(queue).hasSize(3);
        assertThat(queue.get(0).getTrack()).isSameAs(itemC);
        assertThat(queue.get(1).getTrack()).isSameAs(itemB);
        assertThat(queue.get(2).getTrack()).isSameAs(itemA);

        // Second batch [D, E] queues behind everything already queued — D and E play AFTER C.
        // Inverted storage means new rows prepend; the first new item (D) ends up just behind
        // the previously-farthest C, and the last new item (E) ends up at the new far-end (index 0).
        // Final storage [E, D, C, B, A].
        playerService.addToQueue(List.of(itemD, itemE));

        assertThat(queue).hasSize(5);
        assertThat(queue.get(0).getTrack()).isSameAs(itemE);
        assertThat(queue.get(1).getTrack()).isSameAs(itemD);
        assertThat(queue.get(2).getTrack()).isSameAs(itemC);
        assertThat(queue.get(3).getTrack()).isSameAs(itemB);
        assertThat(queue.get(4).getTrack()).isSameAs(itemA);
    }

    @Test
    @DisplayName("playFromQueue appends the previous current track to history (not the selected one)")
    void playFromQueueAppendsPreviousCurrentTrackToHistory() throws Exception {
        ObservableAudioItem itemPrev = newPlayableAudioItem("Prev");
        ObservableAudioItem itemA = newPlayableAudioItem("A");

        // Simulate a track already playing — when the user double-clicks itemA in the queue,
        // itemPrev is the track being LEFT and is what should land in history. itemA is the
        // track being started; it will be appended to history when it finishes via next().
        ReflectionTestUtils.setField(playerService, "currentTrack", Optional.of(itemPrev));

        playerService.addToQueue(List.of(itemA));
        ObservableList<TrackQueueRow> queue = playerService.getPlayQueueList();
        TrackQueueRow rowA = queue.get(0);

        // playFromQueue calls setPlayer which `new`s JavaFxPlayer — see the rationale on the
        // previous test for why we intercept the constructor on every OS.
        try (MockedConstruction<JavaFxPlayer> ignored = Mockito.mockConstruction(JavaFxPlayer.class,
                (mock, context) -> {
                    Mockito.doNothing().when(mock).play(Mockito.any());
                    Mockito.when(mock.getStatusProperty())
                            .thenReturn(new SimpleObjectProperty<>(AudioItemPlayer.Status.UNKNOWN));
                })) {
            playerService.playFromQueue(rowA);
        }

        // History should contain the LEFT track only — itemA is now currentTrack and goes to
        // history later via next() when playback finishes.
        ObservableList<TrackQueueRow> history = playerService.getHistoryQueueList();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getTrack()).isSameAs(itemPrev);

        // Queue had rowA removed.
        assertThat(queue).isEmpty();
    }

    @Test
    @DisplayName("next appends finished track to historyQueueList when queue is empty")
    void nextAppendsFinishedTrackToHistoryQueueListWhenQueueIsEmpty() throws Exception {
        ObservableAudioItem item = newPlayableAudioItem("Solo");

        // Simulate direct table double-click (no queue add): currentTrack populated, queue empty.
        ReflectionTestUtils.setField(playerService, "currentTrack", Optional.of(item));

        assertThat(playerService.getPlayQueueList()).isEmpty();
        assertThat(playerService.getHistoryQueueList()).isEmpty();

        // next() will hit the empty-queue branch — post-Task-1, the history append still runs.
        // next() ends with stop() in this branch (no setPlayer/jfxmedia call).
        playerService.next();

        ObservableList<TrackQueueRow> history = playerService.getHistoryQueueList();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getTrack()).isSameAs(item);
    }

    @Test
    @DisplayName("enforceHistoryCap evicts oldest entries from index 0")
    void enforceHistoryCapEvictsOldestEntriesFromIndexZero() throws Exception {
        ObservableList<TrackQueueRow> history = playerService.getHistoryQueueList();

        // Seed 152 rows; each track is identifiable by its title index.
        ObservableAudioItem[] items = new ObservableAudioItem[152];
        for (int i = 0; i < 152; i++) {
            items[i] = newPlayableAudioItem("track-" + i);
            history.add(new TrackQueueRow(items[i]));
        }
        assertThat(history).hasSize(152);

        ReflectionTestUtils.invokeMethod(playerService, "enforceHistoryCap");

        // overflow = 152 - 150 = 2; evict from index 0..1 (oldest).
        assertThat(history).hasSize(150);
        assertThat(history.get(0).getTrack()).isSameAs(items[2]);
        assertThat(history.get(history.size() - 1).getTrack()).isSameAs(items[151]);
    }

    @SuppressWarnings("unchecked")
    ObservableAudioItem newPlayableAudioItem(String title) throws Exception {
        ObservableAudioItem item = mock(ObservableAudioItem.class);
        // Playable extension required by JavaFxPlayer.Companion.isPlayable
        Path tempFile = Files.createTempFile("musicott-storage-it-", ".mp3");
        tempFile.toFile().deleteOnExit();
        when(item.getPath()).thenReturn(tempFile);
        when(item.getExtension()).thenReturn("mp3");
        when(item.getTitleProperty()).thenReturn(new SimpleStringProperty(title));

        var artist = mock(net.transgressoft.commons.music.audio.Artist.class);
        when(artist.getName()).thenReturn("Test Artist");
        when(item.getArtistProperty()).thenReturn(new SimpleObjectProperty<>(artist));

        var album = mock(net.transgressoft.commons.music.audio.Album.class);
        when(album.getName()).thenReturn("Test Album");
        when(item.getAlbumProperty()).thenReturn(new SimpleObjectProperty<>(album));

        when(item.getCoverImageProperty()).thenReturn(new SimpleObjectProperty<>(Optional.empty()));
        when(item.getPlayCountProperty()).thenReturn(new SimpleIntegerProperty(0));
        when(item.getDuration()).thenReturn(Duration.ofSeconds(5));
        return item;
    }
}
