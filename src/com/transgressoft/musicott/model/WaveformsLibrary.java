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

package com.transgressoft.musicott.model;

import com.google.inject.*;
import javafx.collections.*;

import java.util.*;

/**
 * Class that isolates the operations over the collection of track waveforms
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.10-b
 */
@Singleton
public class WaveformsLibrary {

    private final ObservableMap<Integer, float[]> waveforms = FXCollections.observableHashMap();

    public Map<Integer, float[]> getWaveforms() {
        return waveforms;
    }

    public void setListener(MapChangeListener<Integer, float[]> listener) {
        waveforms.addListener(listener);
    }

    public synchronized void addWaveform(int trackId, float[] waveform) {
        waveforms.put(trackId, waveform);
    }

    public synchronized void addWaveforms(Map<Integer, float[]> newWaveforms) {
        waveforms.putAll(newWaveforms);
    }

    synchronized void removeWaveforms(Collection<Track> tracks) {
        tracks.stream()
              .map(Track::getTrackId)
              .forEach(id -> waveforms.keySet().remove(id));
    }

    public synchronized boolean containsWaveform(int trackId) {
        return waveforms.containsKey(trackId);
    }

    synchronized void clearWaveforms() {
        waveforms.clear();
    }

    public synchronized float[] getWaveform(int trackId) {
        return waveforms.get(trackId);
    }
}
