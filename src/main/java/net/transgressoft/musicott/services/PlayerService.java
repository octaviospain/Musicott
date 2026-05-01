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
import net.transgressoft.commons.fx.music.player.JavaFxPlayer;
import net.transgressoft.commons.music.player.AudioItemPlayer.Status;
import net.transgressoft.commons.music.player.event.AudioItemPlayerEvent;
import net.transgressoft.musicott.events.*;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static net.transgressoft.commons.music.player.AudioItemPlayer.Status.*;

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
    private JavaFxPlayer trackPlayer;
    private boolean playingRandom = false;
    private boolean played = false;
    private boolean scrobbled = false;

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

    public ReadOnlyObjectProperty<Duration> getCurrentTimeProperty() {
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
        } else {
            Status status = playerStatus();
            if (status.equals(STOPPED) || status.equals(PAUSED) || status.equals(UNKNOWN)) {
                setPlayer(audioItem);
            } else if (status.equals(PLAYING)) {
                // TODO ask user if stop current and play or add to top of the queue
                stop();
                setPlayer(audioItem);
            }
        }
    }

    private void setPlayer(ObservableAudioItem audioItem) {
        scrobbled = false;
        played = false;
        trackPlayer = new JavaFxPlayer();
        trackPlayer.play(audioItem);
        currentTrack = Optional.of(audioItem);
        bindMediaPlayer();
        trackPlayer.subscribe(event -> {
            if (event.getType() == AudioItemPlayerEvent.Type.PLAYED) {
                Platform.runLater(() ->
                    currentTrack.ifPresent(track -> track.getPlayCountProperty().add(1))
                );
            }
        });
        trackPlayer.onFinish(() -> Platform.runLater(this::next));
        applicationEventPublisher.publishEvent(new AudioItemChangedEvent(audioItem, this));
        logger.debug("Created new player for track {}", audioItem);
    }

    private void bindMediaPlayer() {
        applicationEventPublisher.publishEvent(new PlaybackStatusChangedEvent(playerStatus(), this));

        trackPlayer.getStatusProperty().addListener((obs, oldStatus, newStatus) -> {
            applicationEventPublisher.publishEvent(new PlaybackStatusChangedEvent(newStatus, this));
        });
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
            case STOPPED:
                playRandom();
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
        if (playQueueList.isEmpty()) {
            stop();
        } else {
            currentTrack.ifPresent(track -> {
                TrackQueueRow row = new TrackQueueRow(track);
                historyQueueList.add(row);
                enforceHistoryCap();
                publishHistoryUpdatedEvent();
            });
            TrackQueueRow nextRow = playQueueList.remove(playQueueList.size() - 1);
            publishQueueUpdatedEvent();
            play(nextRow.getTrack());
        }
    }

    public void previous() {
        if (!historyQueueList.isEmpty()) {
            currentTrack.ifPresent(track -> {
                TrackQueueRow row = new TrackQueueRow(track);
                row.setOnDeleteButtonClickedHandler(e -> {
                    playQueueList.remove(row);
                    publishQueueUpdatedEvent();
                });
                playQueueList.add(row);
                publishQueueUpdatedEvent();
            });
            TrackQueueRow previousRow = historyQueueList.remove(historyQueueList.size() - 1);
            publishHistoryUpdatedEvent();
            setPlayer(previousRow.getTrack());
        } else {
            stop();
        }
    }

    public void playRandom() {
        // TODO
    }

    public void addToQueue(Collection<ObservableAudioItem> audioItems) {
        if (playingRandom) {
            playQueueList.clear();
            playingRandom = false;
        }
        var newRows = audioItems.stream()
                .filter(JavaFxPlayer.Companion::isPlayable)
                .map(item -> {
                    TrackQueueRow row = new TrackQueueRow(item);
                    row.setOnDeleteButtonClickedHandler(e -> {
                        playQueueList.remove(row);
                        publishQueueUpdatedEvent();
                    });
                    return row;
                })
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
            trackPlayer.seek(seekTime.toMillis());
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
