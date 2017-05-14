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

package com.transgressoft.musicott.tasks.parse.itunes;

import com.google.inject.*;
import com.google.inject.assistedinject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.view.*;
import com.worldsworstsoftware.itunes.*;
import javafx.application.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * This class extends from {@link RecursiveTask} so it can be used inside a
 * {@link ForkJoinPool} in order to perform the parse of a collection of
 * {@link ItunesPlaylist} objects into instances of the system's {@link Playlist}.
 *
 * If it receives more than a certain amount of items to parse, the task is forked
 * with partitions of the items collection and their results joined after their completion.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.9.2-b
 */
public class ItunesPlaylistParseAction extends PlaylistParseAction {

    private static final int MAX_PLAYLISTS_TO_PARSE_PER_ACTION = 250;
    private static final int NUMBER_OF_PARTITIONS = 2;

    private final StageDemon stageDemon;
    private final PlaylistsLibrary playlistsLibrary;

    @Inject
    private PlaylistFactory playlistFactory;
    @Inject
    private ParseActionFactory parseActionFactory;
    private Playlist ROOT_PLAYLIST;
    private transient Map<Integer, Track> itunesIdToMusicottTrackMap;

    /**
     * Constructor of {@link ItunesTracksParseAction}
     *
     * @param playlistsLibrary           The {@link PlaylistsLibrary} singleton instance
     * @param rootPlaylist               The root {@link Playlist} reference
     * @param itunesPlaylistsToParse     The {@link List} of {@link ItunesPlaylist} obects to parse
     * @param itunesIdToMusicottTrackMap The {@link Map} between itunes' tracks id's and system's tracks id's
     * @param parentTask                 The reference to the parent {@link BaseParseTask} that called this action
     */
    @Inject
    public ItunesPlaylistParseAction(PlaylistsLibrary playlistsLibrary, StageDemon stageDemon,
            @RootPlaylist Playlist rootPlaylist, @Assisted List<ItunesPlaylist> itunesPlaylistsToParse,
            @Assisted Map<Integer, Track> itunesIdToMusicottTrackMap, @Assisted BaseParseTask parentTask) {
        super(itunesPlaylistsToParse, parentTask);
        this.stageDemon = stageDemon;
        this.playlistsLibrary = playlistsLibrary;
        this.itunesIdToMusicottTrackMap = itunesIdToMusicottTrackMap;
        ROOT_PLAYLIST = rootPlaylist;
    }

    @Override
    protected PlaylistsParseResult compute() {
        if (itemsToParse.size() > MAX_PLAYLISTS_TO_PARSE_PER_ACTION)
            forkIntoSubActions();
        else {
            itemsToParse.forEach(this::parseItem);
            parsedPlaylists.forEach(playlist -> {
                if (playlistsLibrary.containsPlaylistName(playlist.getName()))
                    addTracksToExistingPlaylist(playlist);
                else {
                    NavigationController navigationController = stageDemon.getNavigationController();
                    Platform.runLater(() -> navigationController.addNewPlaylist(ROOT_PLAYLIST, playlist, false));
                    playlistsLibrary.addPlaylist(ROOT_PLAYLIST, playlist);
                }
            });
        }
        return new PlaylistsParseResult(parsedPlaylists);
    }

    private void addTracksToExistingPlaylist(Playlist parsedPlaylist) {
        Set<Playlist> playlists = playlistsLibrary.getPlaylistsTree().nodes();
        synchronized (playlistsLibrary) {
            boolean found = false;
            Iterator<Playlist> iterator = playlists.iterator();
            while (!found && iterator.hasNext()) {

                Playlist next = iterator.next();
                if (next.equals(parsedPlaylist)) {
                    next.addTracks(parsedPlaylist.getTracks());
                    found = true;
                }
            }
        }
    }

    @Override
    protected ItunesPlaylistParseAction parseActionMapper(List<ItunesPlaylist> subItems) {
        return parseActionFactory.create(subItems, itunesIdToMusicottTrackMap, parentTask);
    }

    @Override
    protected int getNumberOfPartitions() {
        return NUMBER_OF_PARTITIONS;
    }

    /**
     * Parse the itunes playlists into {@link Playlist} objects.
     */
    @Override
    @SuppressWarnings ("unchecked")
    public void parseItem(ItunesPlaylist itunesPlaylist) {
        Playlist playlist = playlistFactory.create(itunesPlaylist.getName(), false);
        List<ItunesTrack> itunesTracksList = itunesPlaylist.getPlaylistItems();
        List<Integer> playlistTracksIds = getPlaylistTracksAlreadyParsed(itunesTracksList);
        playlist.addTracks(playlistTracksIds);

        if (! playlist.isEmpty())
            parsedPlaylists.add(playlist);

        LOG.debug("Parsed iTunes playlist {}", playlist.getName());
        parentTask.updateProgressTask();
    }

    /**
     * Retrieves a {@link List} of identification integers of {@link Track}
     * instances that have been already parsed from the ItunesLibrary so those tracks
     * could be mapped into the parsed playlists instead of parse them again.
     *
     * @param itunesTracks The {@code List} of {@link ItunesTrack} instances
     *
     * @return The {@code List} of integers that are the ids of the {@code parsedTracks}
     */
    private List<Integer> getPlaylistTracksAlreadyParsed(List<ItunesTrack> itunesTracks) {
        return itunesTracks.stream()
                           .filter(itunesTrack -> itunesIdToMusicottTrackMap.containsKey(itunesTrack.getTrackID()))
                           .map(itunesTrack -> itunesIdToMusicottTrackMap.get(itunesTrack.getTrackID()).getTrackId())
                           .collect(Collectors.toList());
    }
}
