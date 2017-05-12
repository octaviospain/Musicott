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

import java.util.*;
import java.util.Map.*;

/**
 * Class that isolates the operations over the collection of albums
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public class AlbumsLibrary {

    public static final String UNK_ALBUM = "Unknown album";

    private Multimap<String, Entry<Integer, Track>> albumsTracks = Multimaps.synchronizedMultimap(HashMultimap.create());

    @Inject
    public AlbumsLibrary() {}

    synchronized boolean addTracks(String album, List<Entry<Integer, Track>> trackEntries) {
        //        if (! albumsLists.contains(album)) {
        //            // TODO update the observable albumLists when implemented album view
        //        }
        return albumsTracks.putAll(album, trackEntries);
    }

    synchronized boolean removeTracks(String album, List<Entry<Integer, Track>> trackEntries) {
        boolean removed;
        removed = albumsTracks.get(album).removeAll(trackEntries);
        if (! albumsTracks.containsKey(album)) {
            // TODO update observable albumList when implemented album view
        }
        return removed;
    }

    synchronized void clear() {
        albumsTracks.clear();
    }

    synchronized ImmutableMultimap<String, Entry<Integer, Track>> getTracksByAlbum(String artist, Set<String> albums) {
        return ImmutableMultimap.copyOf(
                Multimaps.filterValues(
                        Multimaps.filterKeys(albumsTracks, albums::contains),
                        entry -> entry.getValue().getArtistsInvolved().contains(artist)));
    }
}