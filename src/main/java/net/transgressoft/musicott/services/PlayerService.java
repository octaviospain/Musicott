/******************************************************************************
 *     Copyright (C) 2025  Octavio Calleya Garcia                             *
 *                                                                            *
 *     This program is free software: you can redistribute it and/or modify   *
 *     it under the terms of the GNU General Public License as published by   *
 *     the Free Software Foundation, either version 3 of the License, or      *
 *     (at your option) any later version.                                    *
 *                                                                            *
 *     This program is distributed in the hope that it will be useful,        *
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 *     GNU General Public License for more details.                           *
 *                                                                            *
 *     You should have received a copy of the GNU General Public License      *
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>. *
 ******************************************************************************/

package net.transgressoft.musicott.services;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.player.FXAudioItemPlayer;
import net.transgressoft.commons.music.event.PlayedEventSubscriber;
import net.transgressoft.commons.music.player.AudioItemPlayer;
import net.transgressoft.commons.music.player.AudioItemPlayer.Status;
import net.transgressoft.commons.music.player.UnsupportedAudioPlaybackException;
import net.transgressoft.commons.music.player.event.AudioItemPlayerEvent.*;
import net.transgressoft.musicott.events.*;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.time.Duration;
import java.util.stream.Collectors;

import static net.transgressoft.commons.music.player.AudioItemPlayer.Status.*;
import static net.transgressoft.commons.music.player.event.AudioItemPlayerEvent.Type.PLAYED;

/**
 * Manages audio playback state, play queue, and history queue.
 */
@Service
public class PlayerService {

    private static final int HISTORY_CAP = 150;

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final ApplicationEventPublisher applicationEventPublisher;

    private final ObservableList<TrackQueueRow> playQueueList = FXCollections.observableArrayList();
    private final ObservableList<TrackQueueRow> historyQueueList = FXCollections.observableArrayList();

    private Optional<ObservableAudioItem> currentTrack = Optional.empty();
    private FXAudioItemPlayer trackPlayer;
    private ChangeListener<Status> statusListener;
    private PlayedEventSubscriber playedEventSubscriber;
    private boolean playingRandom = false;

    @Autowired
    public PlayerService(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public ObservableList<TrackQueueRow> getPlayQueueList() {
        return playQueueList;
    }

    public ObservableList<TrackQueueRow> getHistoryQueueList() {
        return historyQueueList;
    }

    public Optional<ObservableAudioItem> currentTrack() {
        return currentTrack;
    }

    public Status playerStatus() {
        return trackPlayer == null ? UNKNOWN : trackPlayer.status();
    }

    public DoubleProperty getVolumeProperty() {
        return trackPlayer != null ? trackPlayer.getVolumeProperty() : null;
    }

    public ReadOnlyObjectProperty<javafx.util.Duration> getCurrentTimeProperty() {
        return trackPlayer != null ? trackPlayer.getCurrentTimeProperty() : null;
    }

    public Duration getTotalDuration() {
        return trackPlayer != null ? trackPlayer.getTotalDuration() : Duration.ZERO;
    }

    public ReadOnlyObjectProperty<Status> getStatusProperty() {
        return trackPlayer != null ? trackPlayer.getStatusProperty() : null;
    }

    public void play(ObservableAudioItem audioItem) {
        if (!audioItem.getPath().toFile().exists()) {
            applicationEventPublisher.publishEvent(new ErrorEvent("File not found", audioItem.getPath().toString(), this));
            return;
        }
        // After a natural end-of-track the core reports READY, which the old branch list
        // did not handle — causing silent no-ops and broken auto-advance. Treat every
        // non-PLAYING status as "idle": stop only when actively playing, then always start
        // a fresh player. setPlayer() disposes any existing player before constructing the new one.
        if (playerStatus() == PLAYING) {
            stop();
        }
        setPlayer(audioItem);
    }

    private void setPlayer(ObservableAudioItem audioItem) {
        // Release the previous player and its subscription before swapping in a new one — the
        // direct callers (previous, playFromQueue, playFromHistoryQueue) do not stop it themselves.
        if (trackPlayer != null) {
            // Detach the status listener before stopping/disposing the outgoing player. Otherwise
            // the dispose-time STOPPED transition is delivered asynchronously and reaches the UI
            // after the new track's labels are bound, clearing them for the rest of the session.
            if (statusListener != null) {
                trackPlayer.getStatusProperty().removeListener(statusListener);
                statusListener = null;
            }
            trackPlayer.stop();
            trackPlayer.dispose();
        }
        if (playedEventSubscriber != null) {
            playedEventSubscriber.cancelSubscription();
            playedEventSubscriber = null;
        }

        var newPlayer = new FXAudioItemPlayer();
        // Bind to the AudioItemPlayer interface so Kotlin's @Throws on the interface method is
        // visible to Java's checked-exception analysis (the concrete overrides do not carry it).
        AudioItemPlayer playerView = newPlayer;
        try {
            playerView.play(audioItem);
        } catch (UnsupportedAudioPlaybackException e) {
            logger.error("Cannot play audio item {}", audioItem.getPath(), e);
            newPlayer.dispose();
            applicationEventPublisher.publishEvent(new ErrorEvent("Unsupported audio format", e.getMessage(), this));
            return;
        }
        trackPlayer = newPlayer;
        currentTrack = Optional.of(audioItem);
        bindMediaPlayer();

        playedEventSubscriber = new PlayedEventSubscriber();
        playedEventSubscriber.addOnNextEventAction(
            new Type[] { PLAYED }, _ -> Platform.runLater(() ->
                currentTrack.ifPresent(track -> track.getPlayCountProperty().add(1))));
        trackPlayer.subscribe(playedEventSubscriber);

        trackPlayer.onFinish(() -> Platform.runLater(this::next));
        applicationEventPublisher.publishEvent(new AudioItemChangedEvent(audioItem, this));
        logger.debug("Created new player for track {}", audioItem);
    }

    private void bindMediaPlayer() {
        applicationEventPublisher.publishEvent(new PlaybackStatusChangedEvent(playerStatus(), this));

        statusListener = (obs, oldStatus, newStatus) ->
            applicationEventPublisher.publishEvent(new PlaybackStatusChangedEvent(newStatus, this));
        trackPlayer.getStatusProperty().addListener(statusListener);
    }

    public void pause() {
        if (trackPlayer == null)
            return;
        switch (playerStatus()) {
            case PLAYING:
                trackPlayer.pause();
                logger.info("Player paused");
                break;
            case PAUSED:
                resume();
                break;
            default:
        }
    }

    public void resume() {
        if (trackPlayer == null)
            return;
        trackPlayer.resume();
        logger.info("Player resumed");
    }

    private void stop() {
        if (trackPlayer != null)
            trackPlayer.stop();
        currentTrack = Optional.empty();
        logger.info("Player stopped");
    }

    public void next() {
        currentTrack.ifPresent(track -> {
            TrackQueueRow row = new TrackQueueRow(track);
            historyQueueList.add(row);
            enforceHistoryCap();
            publishHistoryUpdatedEvent();
        });
        if (playQueueList.isEmpty()) {
            stop();
        } else {
            TrackQueueRow nextRow = playQueueList.remove(playQueueList.size() - 1);
            publishQueueUpdatedEvent();
            play(nextRow.getTrack());
        }
    }

    public void previous() {
        if (!historyQueueList.isEmpty()) {
            currentTrack.ifPresent(track -> {
                // Delete-button handler is set by PlayQueueController.queueChangeListener
                // when this row is observed entering playQueueList — keeping handler ownership
                // in one place avoids the previous pattern where the controller silently
                // overwrote a PlayerService-set handler.
                playQueueList.add(new TrackQueueRow(track));
                publishQueueUpdatedEvent();
            });
            TrackQueueRow previousRow = historyQueueList.remove(historyQueueList.size() - 1);
            publishHistoryUpdatedEvent();
            setPlayer(previousRow.getTrack());
        } else {
            stop();
        }
    }

    /**
     * Shuffles the playable items from the given pool, fills the play queue, and starts playback.
     *
     * <p>If the pool contains no playable items, a {@link StatusMessageUpdateEvent} is published
     * with the message "No playable tracks available" and the queue remains unchanged.
     * The {@code playingRandom} flag is set to {@code true} after the queue is populated, so
     * any subsequent explicit {@link #addToQueue} call will clear the random queue first.
     *
     * @param pool the collection of audio items to draw from; non-playable items are filtered out
     */
    public void playRandom(Collection<ObservableAudioItem> pool) {
        var playable = pool.stream()
                .filter(AudioItemPlayer.Companion::isPlayable)
                .collect(Collectors.toCollection(ArrayList::new));
        if (playable.isEmpty()) {
            applicationEventPublisher.publishEvent(
                    new StatusMessageUpdateEvent("No playable tracks available", this));
            return;
        }
        Collections.shuffle(playable);
        // Random playback replaces any existing queue. Clear explicitly first (with the flag still
        // false so addToQueue does not self-clear the items being enqueued), then mark the queue as
        // random once it is populated.
        playQueueList.clear();
        publishQueueUpdatedEvent();
        playingRandom = false;
        addToQueue(playable);
        playingRandom = true;
        next();
    }

    public void addToQueue(Collection<ObservableAudioItem> audioItems) {
        if (playingRandom) {
            playQueueList.clear();
            playingRandom = false;
        }
        // Delete-button handler is set by PlayQueueController.queueChangeListener when each
        // row is observed entering playQueueList — see removeFromPlayQueue / removeFromHistoryQueue.
        var newRows = audioItems.stream()
                .filter(AudioItemPlayer.Companion::isPlayable)
                .map(TrackQueueRow::new)
                .toList();
        // Inverted storage: index size-1 is next-up, index 0 is farthest-out. The first input is
        // next-up (bottom of the popover), the last input is farthest (top). Reverse the new rows
        // and addAll at index 0 in one call, avoiding O(n²) repeated shifts and N change events.
        if (!newRows.isEmpty()) {
            var reversed = new java.util.ArrayList<>(newRows);
            java.util.Collections.reverse(reversed);
            playQueueList.addAll(0, reversed);
            publishQueueUpdatedEvent();
        }
    }

    public void playFromQueue(TrackQueueRow trackQueueRow) {
        ObservableAudioItem track = trackQueueRow.getTrack();
        // Append the previous current track to history (the one being LEFT). The selected
        // track is what's about to start playing; it will be appended to history when it
        // finishes naturally via next() — appending it here would duplicate it.
        currentTrack.ifPresent(prev -> {
            historyQueueList.add(new TrackQueueRow(prev));
            enforceHistoryCap();
            publishHistoryUpdatedEvent();
        });
        playQueueList.remove(trackQueueRow);
        publishQueueUpdatedEvent();
        setPlayer(track);
        logger.debug("Play from queue selected. Queue size {}, history queue size {}", playQueueList.size(), historyQueueList.size());
    }

    public void playFromHistoryQueue(TrackQueueRow trackQueueRow) {
        ObservableAudioItem track = trackQueueRow.getTrack();
        setPlayer(track);
        historyQueueList.remove(trackQueueRow);
        publishHistoryUpdatedEvent();
        logger.debug("Play from history selected. History queue size {}", historyQueueList.size());
    }

    public void clearQueue() {
        playQueueList.clear();
        publishQueueUpdatedEvent();
        logger.debug("Play queue cleared");
    }

    public void removeFromPlayQueue(TrackQueueRow row) {
        if (playQueueList.remove(row)) {
            publishQueueUpdatedEvent();
        }
    }

    public void removeFromHistoryQueue(TrackQueueRow row) {
        if (historyQueueList.remove(row)) {
            publishHistoryUpdatedEvent();
        }
    }

    public void increaseVolume() {
        if (trackPlayer != null) {
            double currentVolume = trackPlayer.getVolumeProperty().get();
            trackPlayer.setVolume(currentVolume + 0.05);
        }
    }

    public void decreaseVolume() {
        if (trackPlayer != null) {
            double currentVolume = trackPlayer.getVolumeProperty().get();
            trackPlayer.setVolume(currentVolume - 0.05);
        }
    }

    public void seek(Duration seekTime) {
        if (trackPlayer != null) {
            trackPlayer.seek(seekTime);
            logger.debug("Player seeked value {}", seekTime.toSeconds());
        }
    }

    private void enforceHistoryCap() {
        if (historyQueueList.size() > HISTORY_CAP) {
            int overflow = historyQueueList.size() - HISTORY_CAP;
            historyQueueList.remove(0, overflow);
        }
    }

    private void publishQueueUpdatedEvent() {
        applicationEventPublisher.publishEvent(new QueueUpdatedEvent(List.copyOf(playQueueList), this));
    }

    private void publishHistoryUpdatedEvent() {
        applicationEventPublisher.publishEvent(new HistoryUpdatedEvent(List.copyOf(historyQueueList), this));
    }
}
