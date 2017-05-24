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

/**
 * Track player interface
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 */
public interface TrackPlayer {

    Status getStatus();

    void setOnEndOfMedia(Runnable value);

    void setTrack(Track track);

    Track getTrack();

    Media getMedia();

    void setVolume(double value);

    void seek(Duration seekTime);

    void play();

    void pause();

    void stop();

    Duration getStopTime();

    Duration getTotalDuration();

    DoubleProperty volumeProperty();

    ReadOnlyObjectProperty<Status> statusProperty();

    ReadOnlyObjectProperty<Duration> currentTimeProperty();
}
