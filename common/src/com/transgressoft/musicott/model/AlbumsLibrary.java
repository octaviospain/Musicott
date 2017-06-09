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
import com.google.inject.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;

import java.util.*;
import java.util.Map.*;

/**
 * Class that isolates the operations over the collection of albums
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.10-b
 */
@Singleton
public class AlbumsLibrary {

    public static final String UNK_ALBUM = "Unknown album";

    private final Multimap<String, Entry<Integer, Track>> albumsTracks = Multimaps.synchronizedMultimap(HashMultimap.create());
    private final ObservableList<String> albumsList;
    private final ListProperty<String> albumsListProperty;

    @Inject
    public AlbumsLibrary() {
        ObservableSet<String> albums = FXCollections.observableSet(albumsTracks.keySet());
        albumsList = FXCollections.observableArrayList(albums);
        albumsListProperty = new SimpleListProperty<>(this, "albums set");
        albumsListProperty.bind(new SimpleObjectProperty<>(albumsList));
    }

    synchronized boolean addTracks(String album, List<Entry<Integer, Track>> trackEntries) {
        if (! albumsList.contains(album)) {
            Platform.runLater(() -> {
                albumsList.add(album);
                FXCollections.sort(albumsList);
            });
        }
        return albumsTracks.putAll(album, trackEntries);
    }

    synchronized boolean removeTracks(String album, List<Entry<Integer, Track>> trackEntries) {
        boolean removed;
        removed = albumsTracks.get(album).removeAll(trackEntries);
        if (! albumsTracks.containsKey(album)) {
            Platform.runLater(() -> albumsList.remove(album));
        }
        return removed;
    }

    synchronized void clear() {
        albumsTracks.clear();
        albumsListProperty.clear();
    }

    public synchronized void updateTrackAlbums(List<Entry<Integer, Track>> modifiedTracks, Set<String> oldAlbums, String newAlbum) {
        oldAlbums.forEach(album -> removeTracks(album, modifiedTracks));
        addTracks(newAlbum, modifiedTracks);
    }

    synchronized ImmutableMultimap<String, Entry<Integer, Track>> getTracksByAlbum(String artist, Set<String> albums) {
        return ImmutableMultimap.copyOf(
                Multimaps.filterValues(
                        Multimaps.filterKeys(albumsTracks, albums::contains),
                        entry -> entry.getValue().getArtistsInvolved().contains(artist)));
    }

    public ListProperty<String> albumsListProperty() {
        return albumsListProperty;
    }
}
