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
import javafx.scene.media.*;
import javafx.util.*;

import java.io.*;

/**
 * @author Octavio Calleya
 */
public class NativePlayer implements TrackPlayer {

    private MediaPlayer mediaPlayer;

    MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public String getStatus() {
        return mediaPlayer == null ? "STOPPED" : mediaPlayer.getStatus().name();
    }

    @Override
    public void setTrack(Track track) {
        File mp3File = new File(track.getFileFolder() + "/" + track.getFileName());
        Media media = new Media(mp3File.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnEndOfMedia(PlayerFacade.getInstance()::next);
    }

    @Override
    public void seek(double seekValue) {
        mediaPlayer.seek(Duration.millis(seekValue));
    }

    @Override
    public void setVolume(double value) {
        mediaPlayer.setVolume(mediaPlayer.getVolume() + value);
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
    public void dispose() {
        mediaPlayer.dispose();
        mediaPlayer = null;
    }
}
