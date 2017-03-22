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

import com.google.common.collect.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;

import java.util.*;

import static com.transgressoft.musicott.model.AlbumsLibrary.*;

/**
 * Class that isolates the operations over the collection of artists
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public class ArtistsLibrary {

    private final TracksLibrary tracksLibrary;
    private final Multimap<String, Integer> artistsTracks;
    private final ObservableList<String> artistsList;
    private final ListProperty<String> artistsListProperty;

    public ArtistsLibrary(TracksLibrary tracksLibrary) {
        this.tracksLibrary = tracksLibrary;
        artistsTracks = Multimaps.synchronizedMultimap(HashMultimap.create());
        // Binds the set of artists to a ListProperty of their elements to be
        // shown on the artists' navigation mode list view.
        ObservableSet<String> artists = FXCollections.observableSet(artistsTracks.keySet());
        artistsList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(artists));
        artistsListProperty = new SimpleListProperty<>(this, "artists set");
        artistsListProperty.bind(new SimpleObjectProperty<>(artistsList));
    }

    /**
     * Link a track to an artist and adds it to the list of
     * artists if it isn't in it yet.
     *
     * @param artist  The artist name
     * @param trackId The id of the track
     *
     * @return {@code True} if the collections were modified {@code False} otherwise
     */
    boolean addArtistTrack(String artist, int trackId) {
        if (! artistsTracks.containsKey(artist))
            Platform.runLater(() -> {
                artistsList.add(artist);
                FXCollections.sort(artistsList);
            });
        return artistsTracks.put(artist, trackId);
    }

    /**
     * Unlink a track of an artist and removes it of the list of
     * artists if it isn't related to any tracks.
     *
     * @param artist  The artist name
     * @param trackId The id of the track
     *
     * @return {@code True} if the collections were modified {@code False} otherwise
     */
    boolean removeArtistTrack(String artist, int trackId) {
        boolean removed;
        removed = artistsTracks.remove(artist, trackId);
        if (! artistsTracks.containsKey(artist))
            Platform.runLater(() -> artistsList.remove(artist));
        return removed;
    }

    boolean contains(String artist) {
        return artistsTracks.containsKey(artist);
    }

    void clear() {
        artistsTracks.clear();
        artistsList.clear();
    }

    ImmutableSet<String> getArtistAlbums(String artist) {
        synchronized (artistsTracks) {
            return artistsTracks.get(artist).stream()
                                .map(trackId -> {
                                    Track track = tracksLibrary.getTrack(trackId).get();
                                    return track.getAlbum().isEmpty() ? UNK_ALBUM : track.getAlbum();
                                }).collect(ImmutableSet.toImmutableSet());
        }
    }

    List<Integer> getRandomListOfArtistTracks(String artist) {
        List<Integer> randomArtistTracks = new ArrayList<>(artistsTracks.get(artist));
        synchronized (artistsTracks) {
            Collections.shuffle(randomArtistTracks);
        }
        return randomArtistTracks;
    }

    ListProperty<String> artistsListProperty() {
        return artistsListProperty;
    }
}