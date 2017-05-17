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

import com.google.common.graph.*;
import com.google.inject.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.util.guice.annotations.*;

import java.util.AbstractMap.*;
import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

/**
 * Class that isolates the operations over the collection of {@link Playlist}s
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
@Singleton
public class PlaylistsLibrary {

    private final TaskDemon taskDemon;
    private final TracksLibrary tracksLibrary;
    private final Playlist ROOT_PLAYLIST;

    private MutableGraph<Playlist> playlistsTree = GraphBuilder.directed().build();
    private Random random = new Random();

    @Inject
    public PlaylistsLibrary(@RootPlaylist Playlist rootPlaylist, TracksLibrary tracksLibrary,
            TaskDemon taskDemon) {
        this.tracksLibrary = tracksLibrary;
        this.taskDemon = taskDemon;
        ROOT_PLAYLIST = rootPlaylist;
        playlistsTree.addNode(ROOT_PLAYLIST);
    }

    public synchronized Graph<Playlist> getPlaylistsTree() {
        return playlistsTree;
    }

    public synchronized void addPlaylist(Playlist parent, Playlist playlist) {
        playlistsTree.putEdge(parent, playlist);
        taskDemon.saveLibrary(false, false, true);
    }

    public synchronized void addPlaylistToRoot(Playlist playlist) {
        addPlaylist(ROOT_PLAYLIST, playlist);
    }

    public synchronized void addPlaylistsRecursively(Playlist parent, Set<Playlist> playlists) {
        playlists.forEach(childPlaylist -> {
            addPlaylist(parent, childPlaylist);
            if (childPlaylist.isFolder())
                addPlaylistsRecursively(childPlaylist, childPlaylist.getContainedPlaylists());
        });
    }

    public synchronized void deletePlaylist(Playlist playlist) {
        Playlist parent = getParentPlaylist(playlist);
        playlistsTree.removeEdge(getParentPlaylist(playlist), playlist);
        playlistsTree.removeNode(playlist);
        parent.getContainedPlaylists().remove(playlist);
        taskDemon.saveLibrary(false, false, true);
    }

    public synchronized void movePlaylist(Playlist movedPlaylist, Playlist targetFolder) {
        Playlist parentOfMovedPlaylist = getParentPlaylist(movedPlaylist);
        playlistsTree.removeEdge(parentOfMovedPlaylist, movedPlaylist);
        parentOfMovedPlaylist.getContainedPlaylists().remove(movedPlaylist);
        playlistsTree.putEdge(targetFolder, movedPlaylist);
        targetFolder.getContainedPlaylists().add(movedPlaylist);
        taskDemon.saveLibrary(false, false, true);
    }

    synchronized void removeFromPlaylists(Collection<Track> tracks) {
        List<Integer> trackIds = tracks.stream().map(Track::getTrackId).collect(Collectors.toList());
        playlistsTree.nodes().stream()
                     .filter(playlist -> ! playlist.isFolder())
                     .forEach(playlist -> playlist.removeTracks(trackIds));
    }

    public synchronized boolean containsPlaylistName(String playlistName) {
        return playlistsTree.nodes().stream().anyMatch(playlist -> playlist.getName().equals(playlistName));
    }

    public synchronized boolean isEmpty() {
        return playlistsTree.nodes().size() == 1  && playlistsTree.nodes().contains(ROOT_PLAYLIST);
    }

    synchronized void clearPlaylists() {
        playlistsTree.nodes().stream().filter(playlist -> ! playlist.isFolder()).forEach(Playlist::clearTracks);
    }

    public synchronized void showPlaylist(Playlist playlist) {
        tracksLibrary.showingTrackEntriesProperty().clear();
        List<Entry<Integer, Track>> tracksUnderPlaylist = getTrackEntriesUnderPlaylist(playlist);
        tracksLibrary.showingTrackEntriesProperty().addAll(tracksUnderPlaylist);
    }

    private List<Entry<Integer, Track>> getTrackEntriesUnderPlaylist(Playlist playlist) {
        return playlist.getTracks().stream()
                       .map(i -> new SimpleEntry<>(i, tracksLibrary.getTrack(i).get()))
                       .collect(Collectors.toList());
    }

    synchronized List<Track> getRandomSortedTrackList(Playlist playlist) {
        List<Integer> randomSortedList = new ArrayList<>(playlist.getTracks());
        Collections.shuffle(randomSortedList, random);
        return randomSortedList.stream()
                               .map(trackId -> tracksLibrary.getTrack(trackId).get())
                               .collect(Collectors.toList());
    }

    /**
     * Returns the parent playlist of a given one. That is, the playlist folder that contains
     * a playlist. As we know that every possible selectable playlist on the view has
     * an unique parent playlist, return the first one of the {@code Set}
     *
     * @param playlist The given {@link Playlist} object
     * @return The playlist in which the given one is contained
     *
     * @throws NullPointerException If the given playlist is not in a folder, something is wrong here
     * @throws IllegalArgumentException If the given playlist is not in the tree, something is wrong here
     */
    public synchronized Playlist getParentPlaylist(Playlist playlist) {
        return playlistsTree.predecessors(playlist).stream().findFirst().get();
    }
}
