/*
 * This file is part of Musicott software.
 *
 * Musicott software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Musicott library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Musicott. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package com.transgressoft.musicott.player;

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.model.Track;
import com.transgressoft.musicott.services.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.view.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.media.*;
import javafx.scene.media.MediaPlayer.*;
import javafx.util.*;
import org.slf4j.*;

import java.util.*;
import java.util.stream.*;

import static javafx.scene.media.MediaPlayer.Status.*;
import static org.fxmisc.easybind.EasyBind.*;

/**
 * Singleton class that isolates the usage of the music player.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 */
public class PlayerFacade {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    private PlayQueueController playQueueController;
    private PlayerController playerController;
    private Optional<Track> currentTrack;
    private TrackPlayer trackPlayer;
    private ObservableList<TrackQueueRow> playList;
    private ObservableList<TrackQueueRow> historyList;
    private boolean playingRandom;
    private boolean played;
    private boolean scrobbled;

    private StageDemon stageDemon = StageDemon.getInstance();
    private MusicLibrary musicLibrary = MusicLibrary.getInstance();
    private ServiceDemon services = ServiceDemon.getInstance();
    private TaskDemon taskDemon = TaskDemon.getInstance();

    private static class InstanceHolder {
        static final PlayerFacade INSTANCE = new PlayerFacade();
        private InstanceHolder() {}
    }

    private PlayerFacade() {
        playList = FXCollections.observableArrayList();
        historyList = FXCollections.observableArrayList();
        playingRandom = false;
        played = false;
        scrobbled = false;
        currentTrack = Optional.empty();
        trackPlayer = new JavaFxPlayer();
        trackPlayer.setOnEndOfMedia(this::next);
    }

    public static PlayerFacade getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public ObservableList<TrackQueueRow> getPlayList() {
        return playList;
    }

    public ObservableList<TrackQueueRow> getHistoryList() {
        return historyList;
    }

    public Optional<Track> getCurrentTrack() {
        return currentTrack;
    }

    public Status getPlayerStatus() {
        return trackPlayer.getStatus();
    }

    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
    }

    public void setPlayQueueController(PlayQueueController playQueueController) {
        this.playQueueController = playQueueController;
    }

    /**
     * Adds a {@link List} of tracks to the play queue. Checks if all tracks given are playable.
     *
     * @param tracksId              The {@code List} of tracks ids to add.
     * @param placeFirstInPlayQueue {@code true} if the tracks added should be placed in the beginning
     *                              of the play queue, {@code false} otherwise
     */
    public void addTracksToPlayQueue(Collection<Integer> tracksId, boolean placeFirstInPlayQueue) {
        Thread playableTracksThread = new Thread(() -> {

            List<Integer> playableTracks = tracksId.stream().filter(trackID -> {
                Optional<Track> track = musicLibrary.getTrack(trackID);
                boolean[] isPlayable = new boolean[1];
                track.ifPresent(t -> isPlayable[0] = t.isPlayable());
                if (! isPlayable[0])
                    Platform.runLater(() -> stageDemon.getNavigationController().setStatusMessage("Unplayable file"));
                return isPlayable[0];
            }).collect(Collectors.toList());

            Platform.runLater(() -> addPlayableTracksToPlayQueue(playableTracks, placeFirstInPlayQueue));
        });
        playableTracksThread.start();
    }

    /**
     * Adds a {@link List} of tracks to the play queue.
     *
     * @param playableTracks        The {@code List} of tracks ids that are playable.
     * @param placeFirstInPlayQueue {@code true} if the tracks added should be placed in the head of the play queue
     */
    private void addPlayableTracksToPlayQueue(List<Integer> playableTracks, boolean placeFirstInPlayQueue) {
        if (playingRandom) {
            playList.clear();
            playableTracks.forEach(trackID -> playList.add(new TrackQueueRow(trackID, playQueueController)));
            playingRandom = false;
        }
        else if (! playableTracks.isEmpty()) {
            List<TrackQueueRow> newTrackRows = playableTracks.stream()
                                                             .map(id -> new TrackQueueRow(id, playQueueController))
                                                             .collect(Collectors.toList());
            synchronized (playList) {
                if (placeFirstInPlayQueue)
                    playList.addAll(0, newTrackRows);
                else
                    playList.addAll(newTrackRows);
            }
            LOG.info("Added {} tracks to player", playableTracks.size());
            if (placeFirstInPlayQueue)
                play(true);
        }
    }

    public void play(boolean playRandom) {
        if (trackPlayer.getStatus().equals(STOPPED) ||
                trackPlayer.getStatus().equals(PAUSED) ||
                trackPlayer.getStatus().equals(UNKNOWN)) {
            if (! playList.isEmpty())
                setCurrentTrack();
            else if (playRandom)
                musicLibrary.makeRandomPlaylist();
        }
        else if (trackPlayer.getStatus().equals(PLAYING))
            if (playList.isEmpty())
                stop();
            else
                setCurrentTrack();
    }

    private void setCurrentTrack() {
        historyList.add(0, playList.get(0));
        setPlayer(playList.get(0).getRepresentedTrackId());
        playList.remove(0);
        trackPlayer.play();
        LOG.info("Playing {}", currentTrack);
    }

    public void stop() {
        trackPlayer.stop();
        currentTrack = Optional.empty();
    }

    private void setPlayer(int trackId) {
        scrobbled = false;
        played = false;
        currentTrack = musicLibrary.getTrack(trackId);
        currentTrack.ifPresent(track -> {
            if (track.isPlayable()) {
                trackPlayer.setTrack(track);
                playerController.updatePlayer(track);
                bindMediaPlayer();
                LOG.debug("Created new player");
            }
        });
    }

    /**
     * Binds the properties of the {@link MediaPlayer} to the components of the view.
     */
    private void bindMediaPlayer() {
        trackPlayer.volumeProperty().bindBidirectional(playerController.volumeSliderValueProperty());

        bindPlayerConfiguration(trackPlayer);

        subscribe(trackPlayer.statusProperty(), status -> {
            if (Status.PLAYING == status)
                playerController.setPlaying();
            else if (Status.PAUSED == status)
                playerController.playButtonSelectedProperty().setValue(false);
            else if (STOPPED == status) {
                playerController.setStopped();
                playerController.volumeSliderValueProperty().unbindBidirectional(trackPlayer.volumeProperty());
            }
        });
    }

    private void bindPlayerConfiguration(TrackPlayer trackPlayer) {
        subscribe(trackPlayer.currentTimeProperty(), time -> {
            Duration halfTime = trackPlayer.getStopTime().divide(2.0);
            if (time.greaterThanOrEqualTo(halfTime))
                incrementCurrentTrackPlayCount();
            if (isCurrentTrackValidToScrobble(trackPlayer, time)) {
                scrobbled = true;
                if (services.usingLastFm()) {
                    services.updateAndScrobbleTrack(currentTrack.get());
                }
            }
        });

        DoubleProperty trackSliderMaxProperty = playerController.trackSliderMaxProperty();
        currentTrack.ifPresent(track -> trackSliderMaxProperty.setValue(track.getTotalTime().toMillis()));

        BooleanProperty trackSliderValueChangingProperty = playerController.trackSliderValueChangingProperty();
        DoubleProperty trackSliderValueProperty = playerController.trackSliderValueProperty();
        subscribe(trackPlayer.currentTimeProperty(), time -> {
            if (! trackSliderValueChangingProperty.get())
                trackSliderValueProperty.setValue(time.toMillis());
        });

        DoubleProperty trackProgressBarProgressProperty = playerController.trackProgressBarProgressProperty();
        subscribe(trackSliderValueProperty, value -> {
            Double endTime = trackPlayer.getStopTime().toMillis();
            if (trackSliderValueChangingProperty.get() && (! endTime.equals(Double.POSITIVE_INFINITY) || ! endTime
                    .equals(Double.NaN))) {
                trackProgressBarProgressProperty.set(value.doubleValue() / endTime);
                trackPlayer.seek(Duration.millis(value.doubleValue()));
            }
        });

        subscribe(trackPlayer.currentTimeProperty(),
                  t -> trackProgressBarProgressProperty.set(t.toMillis() / trackSliderMaxProperty.get()));
        subscribe(trackPlayer.currentTimeProperty(),
                  t -> playerController.updateTrackLabels(t, trackPlayer.getMedia().getDuration()));
    }

    private void incrementCurrentTrackPlayCount() {
        if (! played) {
            currentTrack.ifPresent(Track::incrementPlayCount);
            taskDemon.saveLibrary(true, false, false);
            played = true;
        }
    }

    private boolean isCurrentTrackValidToScrobble(TrackPlayer trackPlayer, Duration newTime) {
        boolean isDurationBeyond30Seconds = trackPlayer.getTotalDuration().greaterThanOrEqualTo(Duration.seconds(30));
        boolean isDurationBeyondMidTime = newTime.greaterThanOrEqualTo(trackPlayer.getStopTime().divide(2.0));
        boolean isDurationLongerThan4Minutes = newTime.greaterThanOrEqualTo(Duration.minutes(4));

        return ! scrobbled && isDurationBeyond30Seconds && (isDurationBeyondMidTime || isDurationLongerThan4Minutes);
    }

    /**
     * Deletes the {@link TrackQueueRow} objects from the play queue and from the history queue
     *
     * @param tracksToDelete The {@link List} of track ids
     */
    public void deleteFromQueues(List<Integer> tracksToDelete) {
        currentTrack.ifPresent(track -> {
            if (tracksToDelete.contains(track.getTrackId())) {
                currentTrack = Optional.empty();
                stop();
            }
        });

        tracksToDelete.forEach(trackId -> {
            Iterator<TrackQueueRow> trackQueueRowIterator = playList.iterator();
            while (trackQueueRowIterator.hasNext())
                if (trackQueueRowIterator.next().getRepresentedTrackId() == trackId)
                    trackQueueRowIterator.remove();

            trackQueueRowIterator = historyList.iterator();
            while (trackQueueRowIterator.hasNext())
                if (trackQueueRowIterator.next().getRepresentedTrackId() == trackId)
                    trackQueueRowIterator.remove();
        });
    }

    public void pause() {
        if (trackPlayer.getStatus().equals(PLAYING)) {
            trackPlayer.pause();
            LOG.info("Player paused");
        }
    }

    public void next() {
        if (playList.isEmpty())
            stop();
        else
            play(false);
    }

    public void previous() {
        if (! historyList.isEmpty()) {
            setPlayer(historyList.get(0).getRepresentedTrackId());
            historyList.remove(0);
            trackPlayer.play();
        }
        else
            stop();
    }

    public void resume() {
        trackPlayer.play();
        LOG.info("Player resumed");
    }

    public void seek(Duration seekTime) {
        trackPlayer.seek(seekTime);
        LOG.debug("Player seeked value {}", seekTime);
    }

    /**
     * Plays a {@link Track} that is on the history queue selected by the user.
     *
     * @param index The position of the {@code track} in the history queue.
     */
    public void playHistoryIndex(int index) {
        setPlayer(historyList.get(index).getRepresentedTrackId());
        historyList.remove(index);
        trackPlayer.play();
    }

    /**
     * Plays a {@link Track} that is on the play queue selected by the user.
     *
     * @param index The position of the {@code track} in the history queue.
     */
    public void playQueueIndex(int index) {
        setPlayer(playList.get(index).getRepresentedTrackId());
        historyList.add(0, playList.get(index));
        playList.remove(index);
        trackPlayer.play();
    }

    public void increaseVolume(double value) {
        double currentVolume = trackPlayer.volumeProperty().get();
        trackPlayer.setVolume(currentVolume + value);
    }

    public void decreaseVolume(double value) {
        double currentVolume = trackPlayer.volumeProperty().get();
        trackPlayer.setVolume(currentVolume - value);
    }

    public void setRandomList(List<Integer> randomTrackIds) {
        if (! randomTrackIds.isEmpty()) {
            LOG.info("Created random list of tracks");
            playingRandom = true;
            for (int index = 0; index < randomTrackIds.size(); index++) {
                int i = index;
                Platform.runLater(() -> {
                    playList.add(new TrackQueueRow(randomTrackIds.get(i), playQueueController));
                    if (i == 0) {
                        setCurrentTrack();
                    }
                });
            }
            Platform.runLater(() -> stageDemon.getNavigationController().setStatusMessage("Playing random playlist"));
        }
        else
            Platform.runLater(playerController::setStopped);
    }
}
