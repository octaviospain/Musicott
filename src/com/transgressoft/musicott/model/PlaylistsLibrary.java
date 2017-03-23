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

import com.transgressoft.musicott.tasks.*;

import java.util.*;
import java.util.stream.*;

/**
 * Class that isolates the operations over the collection of {@link Playlist}s
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public class PlaylistsLibrary {

    private final TaskDemon taskDemon = TaskDemon.getInstance();
    final List<Playlist> playlists = new ArrayList<>();
    private Random random = new Random();

    public synchronized void addPlaylist(Playlist playlist) {
        playlists.add(playlist);
        taskDemon.saveLibrary(false, false, true);
    }

    public synchronized void addPlaylists(List<Playlist> newPlaylists) {
        playlists.addAll(newPlaylists);
    }

    public synchronized void deletePlaylist(Playlist playlist) {
        boolean removed = playlists.remove(playlist);
        if (! removed) {
            List<Playlist> folders = playlists.stream().filter(Playlist::isFolder).collect(Collectors.toList());
            deletePlaylistFromFolders(playlist, folders);
        }
        taskDemon.saveLibrary(false, false, true);
    }

    private void deletePlaylistFromFolders(Playlist playlist, List<Playlist> folders) {
        for (Playlist folder : folders) {
            ListIterator<Playlist> folderChildsIterator = folder.getContainedPlaylists().listIterator();
            while (folderChildsIterator.hasNext())
                if (folderChildsIterator.next().equals(playlist)) {
                    folderChildsIterator.remove();
                    break;
                }
        }
    }

    synchronized void removeFromPlaylists(int trackId) {
        playlists.stream().filter(playlist -> ! playlist.isFolder())
                 .forEach(playlist -> playlist.removeTracks(Collections.singletonList(trackId)));
        taskDemon.saveLibrary(false, false, true);
    }

    public synchronized boolean containsPlaylist(Playlist playlist) {
        return playlists.contains(playlist);
    }

    synchronized void clearPlaylists() {
        playlists.stream().filter(playlist -> ! playlist.isFolder()).forEach(Playlist::clearTracks);
    }

    synchronized List<Integer> getRandomSortedList(Playlist playlist) {
        List<Integer> randomSortedList = new ArrayList<>(playlist.getTracks());
        Collections.shuffle(randomSortedList, random);
        return randomSortedList;
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }
}