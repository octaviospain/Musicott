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

import com.google.inject.*;
import com.transgressoft.musicott.model.Track;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.services.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.view.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.media.*;
import javafx.scene.media.MediaPlayer.*;
import javafx.util.*;
import org.slf4j.*;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

import static javafx.scene.media.MediaPlayer.Status.*;
import static org.fxmisc.easybind.EasyBind.*;

/**
 * Singleton class that isolates the usage of the music player.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
@Singleton
public class PlayerFacade {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final TracksLibrary tracksLibrary;
    private final ServiceDemon serviceDemon;

    private ErrorDialogController errorDialog;
    private NavigationController navigationController;
    private PlayQueueController playQueueController;
    private PlayerController playerController;
    private Optional<Track> currentTrack;
    private TrackPlayer trackPlayer;
    private ObservableList<TrackQueueRow> playList;
    private ObservableList<TrackQueueRow> historyList;
    private boolean playingRandom;
    private boolean played;
    private boolean scrobbled;

    @Inject
    public PlayerFacade(TracksLibrary tracksLibrary, ServiceDemon serviceDemon) {
        this.tracksLibrary = tracksLibrary;
        this.serviceDemon = serviceDemon;
        playList = FXCollections.observableArrayList();
        historyList = FXCollections.observableArrayList();
        playingRandom = false;
        played = false;
        scrobbled = false;
        currentTrack = Optional.empty();
        trackPlayer = new JavaFxPlayer();
        trackPlayer.setOnEndOfMedia(this::next);
        LOG.debug("PlayerFacade created {} ", this);
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

    /**
     * Adds a {@link List} of tracks to the play queue. Checks if all tracks given are playable.
     *
     * @param tracks                The {@code List} of tracks to add.
     * @param placeFirstInPlayQueue {@code true} if the tracks added should be placed in the beginning
     *                              of the play queue, {@code false} otherwise
     */
    public void addTracksToPlayQueue(Collection<Track> tracks, boolean placeFirstInPlayQueue) {
        Thread playableTracksThread = new Thread(() -> {
            List<Track> playableTracks = tracks.stream()
                                               .filter(this::isPlayable).collect(Collectors.toList());
            Platform.runLater(() -> addPlayableTracksToPlayQueue(playableTracks, placeFirstInPlayQueue));
        });
        playableTracksThread.start();
    }

    private boolean isPlayable(Track track) {
        boolean isPlayable = false;
        try {
            isPlayable = track.isPlayable();
        }
        catch (IOException exception) {
            String fullPath = track.getFileFolder() + track.getFileName();
            errorDialog.show("Track not found", fullPath);
        }
        if (! isPlayable)
            Platform.runLater(() -> navigationController.setStatusMessage("Unplayable file"));
        return isPlayable;
    }

    /**
     * Adds a {@link List} of tracks to the play queue.
     *
     * @param playableTracks        The {@code List} of tracks ids that are playable.
     * @param placeFirstInPlayQueue {@code true} if the tracks added should be placed in the head of the play queue
     */
    private void addPlayableTracksToPlayQueue(List<Track> playableTracks, boolean placeFirstInPlayQueue) {
        if (playingRandom) {
            playList.clear();
            playableTracks.forEach(track -> playList.add(new TrackQueueRow(track, playQueueController)));
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
                playRandomPlaylist();
        }
        else if (trackPlayer.getStatus().equals(PLAYING))
            if (playList.isEmpty())
                stop();
            else
                setCurrentTrack();
    }

    /**
     * Creates a random playlist asynchronously
     */
    private void playRandomPlaylist() {
        Thread randomPlaylistThread = new Thread(() -> {
            List<Track> randomPlaylist = tracksLibrary.getRandomTracks();
            setRandomListAndPlay(randomPlaylist);
        }, "Random Playlist Thread");
        randomPlaylistThread.start();
    }

    private void setCurrentTrack() {
        historyList.add(0, playList.get(0));
        setPlayer(playList.get(0).getRepresentedTrack());
        playList.remove(0);
        trackPlayer.play();
        LOG.info("Playing {}", currentTrack);
    }

    public void stop() {
        trackPlayer.stop();
        currentTrack = Optional.empty();
    }

    private void setPlayer(Track track) {
        scrobbled = false;
        played = false;
        if (isPlayable(track)) {
            try {
                trackPlayer.setTrack(track);
                currentTrack = Optional.of(track);
                playerController.updatePlayer(track);
                bindMediaPlayer();
                LOG.debug("Created new player");
            }
            catch (MediaException exception) {
                currentTrack = Optional.empty();
                LOG.warn("MediaException, track not playable: {}", exception);
            }
        }
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
                if (serviceDemon.usingLastFm()) {
                    serviceDemon.updateAndScrobbleTrack(currentTrack.get());
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
    public void deleteFromQueues(Collection<Track> tracksToDelete) {
        currentTrack.ifPresent(track -> {
            if (tracksToDelete.contains(track)) {
                currentTrack = Optional.empty();
                stop();
            }
        });

        tracksToDelete.forEach(track -> {
            Iterator<TrackQueueRow> trackQueueRowIterator = playList.iterator();
            while (trackQueueRowIterator.hasNext())
                if (trackQueueRowIterator.next().getRepresentedTrack().equals(track))
                    trackQueueRowIterator.remove();

            trackQueueRowIterator = historyList.iterator();
            while (trackQueueRowIterator.hasNext())
                if (trackQueueRowIterator.next().getRepresentedTrack().equals(track))
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
            setPlayer(historyList.get(0).getRepresentedTrack());
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
        setPlayer(historyList.get(index).getRepresentedTrack());
        historyList.remove(index);
        trackPlayer.play();
    }

    /**
     * Plays a {@link Track} that is on the play queue selected by the user.
     *
     * @param index The position of the {@code track} in the history queue.
     */
    public void playQueueIndex(int index) {
        setPlayer(playList.get(index).getRepresentedTrack());
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

    public void setRandomListAndPlay(List<Track> randomTracks) {
        if (! randomTracks.isEmpty()) {
            LOG.info("Created random list of tracks");
            playingRandom = true;
            for (int index = 0; index < randomTracks.size(); index++) {
                int i = index;
                Platform.runLater(() -> {
                    playList.add(0, new TrackQueueRow(randomTracks.get(i), playQueueController));
                    if (i == 0)
                        setCurrentTrack();
                });
            }
            Platform.runLater(() -> navigationController.setStatusMessage("Playing random playlist"));
        }
        else
            Platform.runLater(playerController::setStopped);
    }

    public void setWaveform(Track track) {
        SwingUtilities.invokeLater(() -> playerController.setWaveform(track));
    }

    @Inject (optional = true)
    public void setErrorDialogController(@ErrorCtrl ErrorDialogController errorDialogController) {
        errorDialog = errorDialogController;
        LOG.debug("ErrorDialogController setted {} ", errorDialogController);
    }

    @Inject (optional = true)
    public void setNavigationController(@NavigationCtrl NavigationController navigationController) {
        this.navigationController = navigationController;
        LOG.debug("NavigationController setted {} ", navigationController);
    }

    @Inject (optional = true)
    public void setPlayerController(@PlayerCtrl PlayerController playerController) {
        this.playerController = playerController;
        LOG.debug("PlayerController setted {} ", playerController);
    }

    @Inject (optional = true)
    public void setPlayQueueController(@PlayQueueCtrl PlayQueueController playQueueController) {
        this.playQueueController = playQueueController;
        LOG.debug("PlayQueueController setted {} ", playQueueController);
    }
}