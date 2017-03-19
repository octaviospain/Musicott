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

/**
 * This class extends from {@link BaseParseAction} specifying its type parameters
 * so it can be used to parse a collection of {@link ItunesTrack}s producing
 * an {@link ItunesParseResult} that contains the resulting {@link Track} instances
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public abstract class ItunesParseAction extends BaseParseAction<ItunesTrack, Map<Integer, Track>, ItunesParseResult> {

    protected Map<Integer, Track> parsedTracks;
    protected Map<Integer, Integer> itunesIdToMusicottIdMap;
    protected List<String> notFoundFiles;

    public ItunesParseAction(List<ItunesTrack> itemsToParse, BaseParseTask parentTask) {
        super(itemsToParse, parentTask);
        parsedTracks = new HashMap<>();
        itunesIdToMusicottIdMap = new HashMap<>();
        notFoundFiles = new ArrayList<>();
    }

    @Override
    protected <T extends BaseParseResult<Map<Integer, Track>>> void joinPartialResults(T partialResult) {
        parsedTracks.putAll(partialResult.getParsedResults());
        parseErrors.addAll(partialResult.getParseErrors());
        ItunesParseResult itunesParseResult = (ItunesParseResult) partialResult;
        itunesIdToMusicottIdMap.putAll(itunesParseResult.getItunesIdToMusicottIdMap());
        notFoundFiles.addAll(itunesParseResult.getNotFoundFiles());
    }
}