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

import com.cedarsoftware.util.io.*;
import com.google.common.collect.*;
import com.sun.javafx.collections.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import javafx.application.*;
import javafx.collections.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.view.MusicottController.*;

/**
 * This class extends from {@link BaseLoadAction} in order to perform the loading
 * of the {@link Map} of tracks of the application's music library stored on a {@code json} file.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 * @since 0.9.2-b
 */
public class TracksLoadAction extends BaseLoadAction {

    private static final int MAX_TRACKS_TO_UPDATE_PER_ACTION = 350;
    private static final int NUMBER_OF_ITEMS_PARTITIONS = 4;
    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    private List<Track> tracksToSetProperties;
    private int step = 0;

    public TracksLoadAction(List<Track> tracks, String applicationFolder, MusicLibrary musicLibrary,
            Application musicottApplication) {
        super(applicationFolder, musicLibrary, musicottApplication);
        tracksToSetProperties = tracks;
    }

    @Override
    protected void compute() {
        if (tracksToSetProperties == null)
            loadTracks();

        if (tracksToSetProperties.size() > MAX_TRACKS_TO_UPDATE_PER_ACTION) {
            int subListsSize = tracksToSetProperties.size() / NUMBER_OF_ITEMS_PARTITIONS;
            ImmutableList<TracksLoadAction> subActions = Lists.partition(tracksToSetProperties, subListsSize).stream()
                                                              .map(subList -> new TracksLoadAction(
                                                                      tracksToSetProperties, applicationFolder,
                                                                      musicLibrary, musicottApplication))
                                                              .collect(ImmutableList.toImmutableList());

            subActions.forEach(TracksLoadAction::fork);
            LOG.debug("Forking updating of loaded tracks into {} sub actions", subActions.size());
        }
        else
            tracksToSetProperties.forEach(this::setTrackProperties);
    }

    /**
     * Loads save tracks or creates a new collection. This is done only by the first action
     */
    private void loadTracks() {
        notifyPreloader(- 1, 0, "Loading tracks...");
        File tracksFile = new File(applicationFolder + File.separator + TRACKS_PERSISTENCE_FILE);
        ObservableMap<Integer, Track> tracksMap;
        if (tracksFile.exists())
            tracksMap = parseTracksFromJsonFile(tracksFile);
        else
            tracksMap = FXCollections.observableHashMap();
        tracksToSetProperties = ImmutableList.copyOf(tracksMap.values());
        musicLibrary.addTracks(tracksMap);
    }

    /**
     * Loads the tracks from a saved file formatted in JSON
     *
     * @param tracksFile The JSON formatted file of the tracks
     *
     * @return an {@link ObservableMap} of the tracks, where the key is the track
     * id and the value the {@link Track} object
     */
    @SuppressWarnings ("unchecked")
    private ObservableMap<Integer, Track> parseTracksFromJsonFile(File tracksFile) {
        ObservableMap<Integer, Track> tracksMap;
        int totalTracks;
        try {
            JsonReader.assignInstantiator(ObservableMapWrapper.class, new ObservableMapWrapperCreator());
            tracksMap = (ObservableMap<Integer, Track>) parseJsonFile(tracksFile);
            totalTracks = tracksMap.size();

            tracksMap.values().parallelStream().forEach(track -> {
                setTrackProperties(track);
                notifyPreloader(++ step, totalTracks, "Loading tracks...");
            });
            LOG.info("Loaded tracks from {}", tracksFile);
        }
        catch (IOException exception) {
            tracksMap = FXCollections.observableHashMap();
            LOG.error("Error loading track library: {}", exception.getMessage(), exception);
        }
        return tracksMap;
    }

    /**
     * Sets the values of the properties of a {@link Track} object,
     * because they are not restored on the <tt>json</tt> file when deserialized
     *
     * @param track The track to set its properties values
     */
    private void setTrackProperties(Track track) {
        track.nameProperty().setValue(track.getName());
        track.artistProperty().setValue(track.getArtist());
        track.albumProperty().setValue(track.getAlbum());
        track.genreProperty().setValue(track.getGenre());
        track.commentsProperty().setValue(track.getComments());
        track.albumArtistProperty().setValue(track.getAlbumArtist());
        track.labelProperty().setValue(track.getLabel());
        track.trackNumberProperty().setValue(track.getTrackNumber());
        track.yearProperty().setValue(track.getYear());
        track.discNumberProperty().setValue(track.getDiscNumber());
        track.bpmProperty().setValue(track.getBpm());
        track.lastDateModifiedProperty().setValue(track.getLastDateModified());
        track.playCountProperty().setValue(track.getPlayCount());
        track.getCoverImage().ifPresent(coverBytes -> track.hasCoverProperty().set(true));
        track.isPlayableProperty().setValue(track.isPlayable());
    }
}
