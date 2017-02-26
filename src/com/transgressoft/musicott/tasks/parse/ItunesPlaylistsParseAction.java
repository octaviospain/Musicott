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

package com.transgressoft.musicott.tasks.parse;

import com.google.common.collect.*;
import com.transgressoft.musicott.model.*;
import com.worldsworstsoftware.itunes.*;
import org.slf4j.*;

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
 * @version 0.9.1-b
 * @since 0.9.2-b
 */
public class ItunesPlaylistsParseAction extends RecursiveTask<ParseResult<List<Playlist>>> {

    private static final int MAX_PLAYLISTS_TO_PARSE_PER_ACTION = 250;
    private static final int NUMBER_OF_ITEMS_PARTITIONS = 2;
    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private List<ItunesPlaylist> itunesPlaylistsToParse;
    private Map<Integer, Integer> itunesIdToMusicottIdMap;
    private List<Playlist> parsedPlaylists;
    private BaseParseTask parentTask;

    /**
     * Constructor of {@link ItunesTracksParseAction}
     *
     * @param itunesPlaylistsToParse  The {@link List} of {@link ItunesPlaylist} obects to parse
     * @param itunesIdToMusicottIdMap The {@link Map} between itunes' tracks id's and system's tracks id's
     * @param parentTask              The reference to the parent {@link BaseParseTask} that called this action
     */
    public ItunesPlaylistsParseAction(List<ItunesPlaylist> itunesPlaylistsToParse,
            Map<Integer, Integer> itunesIdToMusicottIdMap, BaseParseTask parentTask) {
        this.itunesPlaylistsToParse = itunesPlaylistsToParse;
        this.itunesIdToMusicottIdMap = itunesIdToMusicottIdMap;
        this.parentTask = parentTask;
        parsedPlaylists = new ArrayList<>();
    }

    @Override
    protected ParseResult<List<Playlist>> compute() {
        if (itunesPlaylistsToParse.size() > MAX_PLAYLISTS_TO_PARSE_PER_ACTION) {
            int subListsSize = itunesPlaylistsToParse.size() / NUMBER_OF_ITEMS_PARTITIONS;
            ImmutableList<ItunesPlaylistsParseAction> subActions = Lists.partition(itunesPlaylistsToParse, subListsSize)
                .stream().map(sublist -> new ItunesPlaylistsParseAction(sublist, itunesIdToMusicottIdMap, parentTask))
                .collect(ImmutableList.toImmutableList());

            subActions.forEach(ItunesPlaylistsParseAction::fork);
            subActions.forEach(action -> parsedPlaylists.addAll(action.join().getParsedItems()));
        }
        else
            itunesPlaylistsToParse.forEach(this::parsePlaylist);
        return new ParseResult<>(parsedPlaylists);
    }

    /**
     * Parse the itunes playlists into {@link Playlist} objects.
     */
    @SuppressWarnings ("unchecked")
    private void parsePlaylist(ItunesPlaylist itunesPlaylist) {
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
     * @param itunesTracks The <tt>List</tt> of {@link ItunesTrack} instances
     *
     * @return The <tt>List</tt> of integers that are the ids of the <tt>parsedTracks</tt>
     */
    private List<Integer> getPlaylistTracksAlreadyParsed(List<ItunesTrack> itunesTracks) {
        return itunesTracks.stream()
                           .filter(itunesTrack -> itunesIdToMusicottIdMap.containsKey(itunesTrack.getTrackID()))
                           .map(itunesTrack -> itunesIdToMusicottIdMap.get(itunesTrack.getTrackID()))
                           .collect(Collectors.toList());
    }
}