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

package com.transgressoft.musicott.tasks.load;

import com.transgressoft.musicott.model.*;
import javafx.application.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.view.MusicottController.*;

/**
 * This class extends from {@link BaseLoadAction} in order to perform the loading
 * of the {@link Map} of waveforms associated with the tracks of the music library
 * of the application, stored on a {@code json} file.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 * @since 0.9.2-b
 */
public class WaveformsLoadAction extends BaseLoadAction {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    public WaveformsLoadAction(String applicationFolder, MusicLibrary musicLibrary, Application application) {
        super(applicationFolder, musicLibrary, application);
    }

    /**
     * Loads the waveforms or creates a new collection
     */
    @Override
    protected void compute() {
        notifyPreloader(1, 4, "Loading waveforms...");
        File waveformsFile = new File(applicationFolder + File.separator + WAVEFORMS_PERSISTENCE_FILE);
        Map<Integer, float[]> waveformsMap;
        if (waveformsFile.exists())
            waveformsMap = parseWaveformsFromJsonFile(waveformsFile);
        else
            waveformsMap = new HashMap<>();
        musicLibrary.addWaveforms(waveformsMap);
    }

    /**
     * Loads the waveforms from a saved file formatted in JSON
     *
     * @param waveformsFile The JSON formatted file of the tracks
     *
     * @return an {@link Map} of the waveforms, where the key is the track id of the
     * waveform and the value the and array of values representing the amplitudes of
     * the audio {@link Track}
     */
    @SuppressWarnings ("unchecked")
    private Map<Integer, float[]> parseWaveformsFromJsonFile(File waveformsFile) {
        Map<Integer, float[]> waveformsMap;
        try {
            waveformsMap = (Map<Integer, float[]>) parseJsonFile(waveformsFile);
            LOG.info("Loaded waveform images from {}", waveformsFile);
        }
        catch (IOException exception) {
            waveformsMap = new HashMap<>();
            LOG.error("Error loading waveform thumbnails: {}", exception.getMessage(), exception);
        }
        return waveformsMap;
    }
}