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

import com.transgressoft.musicott.model.*;

import java.util.*;

/**
 * Extends from {@link ParseResult} adding a {@link Map} of the associated itunes ids
 * to system's track's ids, and a list of not found files during the {@link ItunesTracksParseAction}
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 * @since 0.9.2-b
 */
public class ItunesParseResult extends ParseResult<Map<Integer, Track>> {

    private Map<Integer, Integer> itunesIdToMusicottIdMap;
    private List<String> notFoundFiles;

    public ItunesParseResult(Map<Integer, Track> parsedTracks, Collection<String> parseErrors,
            Map<Integer, Integer> itunesIdToMusicottIdMap, List<String> notFoundFiles) {
        super(parsedTracks, parseErrors);
        this.itunesIdToMusicottIdMap = itunesIdToMusicottIdMap;
        this.notFoundFiles = notFoundFiles;
    }

    public Map<Integer, Integer> getItunesIdToMusicottIdMap() {
        return itunesIdToMusicottIdMap;
    }

    public List<String> getNotFoundFiles() {
        return notFoundFiles;
    }
}