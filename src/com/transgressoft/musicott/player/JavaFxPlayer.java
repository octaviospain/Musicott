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

import com.transgressoft.musicott.model.Track;
import javafx.beans.property.*;
import javafx.scene.media.*;
import javafx.scene.media.MediaPlayer.*;
import javafx.util.*;

import java.io.*;

/**
 * Basic player that uses the native {@link MediaPlayer}
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 */
public class JavaFxPlayer implements TrackPlayer {

    private MediaPlayer mediaPlayer;
    private Runnable endOfMediaAction;
    private Track track;

    @Override
    public Status getStatus() {
        return mediaPlayer == null ? Status.UNKNOWN : mediaPlayer.getStatus();
    }

    @Override
    public void setOnEndOfMedia(Runnable value) {
        endOfMediaAction = value;
        if (mediaPlayer != null)
            mediaPlayer.setOnEndOfMedia(value);
    }

    @Override
    public Track getTrack() {
        return track;
    }

    @Override
    public void setTrack(Track track) throws MediaException {
        this.track = track;
        if (mediaPlayer != null)
            mediaPlayer.dispose();
        File mp3File = new File(track.getFileFolder(), track.getFileName());
        Media media = new Media(mp3File.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnEndOfMedia(endOfMediaAction);
    }

    @Override
    public Media getMedia() {
        return mediaPlayer.getMedia();
    }

    @Override
    public void setVolume(double value) {
        if (value < 0)
            mediaPlayer.setVolume(0.0);
        else
            mediaPlayer.setVolume(value);
    }

    @Override
    public void seek(Duration seekTime) {
        mediaPlayer.seek(seekTime);
    }

    @Override
    public void play() {
        mediaPlayer.play();
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public void stop() {
        mediaPlayer.stop();
    }

    @Override
    public Duration getStopTime() {
        return mediaPlayer.getStopTime();
    }

    @Override
    public Duration getTotalDuration() {
        return mediaPlayer.getTotalDuration();
    }

    @Override
    public DoubleProperty volumeProperty() {
        return mediaPlayer.volumeProperty();
    }

    @Override
    public ReadOnlyObjectProperty<Status> statusProperty() {
        return mediaPlayer.statusProperty();
    }

    @Override
    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return mediaPlayer.currentTimeProperty();
    }
}
