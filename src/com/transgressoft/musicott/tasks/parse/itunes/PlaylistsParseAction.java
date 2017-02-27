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

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.worldsworstsoftware.itunes.*;

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
 * @version 0.9.2-b
 * @since 0.9.2-b
 */
public class PlaylistsParseAction extends ItunesPlaylistsParseAction {

    private static final int MAX_PLAYLISTS_TO_PARSE_PER_ACTION = 250;
    private static final int NUMBER_OF_PARTITIONS = 2;

    private Map<Integer, Integer> itunesIdToMusicottIdMap;

    /**
     * Constructor of {@link ItunesTracksParseAction}
     *
     * @param itunesPlaylistsToParse  The {@link List} of {@link ItunesPlaylist} obects to parse
     * @param itunesIdToMusicottIdMap The {@link Map} between itunes' tracks id's and system's tracks id's
     * @param parentTask              The reference to the parent {@link BaseParseTask} that called this action
     */
    public PlaylistsParseAction(List<ItunesPlaylist> itunesPlaylistsToParse,
            Map<Integer, Integer> itunesIdToMusicottIdMap, BaseParseTask parentTask) {
        super(itunesPlaylistsToParse, parentTask);
        this.itunesIdToMusicottIdMap = itunesIdToMusicottIdMap;
    }

    @Override
    protected PlaylistsParseResult compute() {
        if (itemsToParse.size() > MAX_PLAYLISTS_TO_PARSE_PER_ACTION)
            forkIntoSubActions();
        else
            itemsToParse.forEach(this::parseItem);
        return new PlaylistsParseResult(parsedPlaylists);
    }

    @Override
    protected BaseParseAction<ItunesPlaylist, List<Playlist>, BaseParseResult<List<Playlist>>> parseActionMapper(List<ItunesPlaylist> subItems) {
        return new PlaylistsParseAction(subItems, itunesIdToMusicottIdMap, parentTask);
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
    protected void parseItem(ItunesPlaylist itunesPlaylist) {
        Playlist playlist = new Playlist(itunesPlaylist.getName(), false);
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
                           .filter(itunesTrack -> itunesIdToMusicottIdMap.containsKey(itunesTrack.getTrackID()))
                           .map(itunesTrack -> itunesIdToMusicottIdMap.get(itunesTrack.getTrackID()))
                           .collect(Collectors.toList());
    }
}