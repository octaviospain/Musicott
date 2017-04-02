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
 * so it can be used to parse a collection of {@link ItunesPlaylist}s producing
 * a {@link BaseParseResult} that contains the resulting {@link Playlist} instances
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public abstract class PlaylistParseAction extends BaseParseAction<ItunesPlaylist, List<Playlist>, BaseParseResult<List<Playlist>>> {

    protected transient List<Playlist> parsedPlaylists;

    public PlaylistParseAction(List<ItunesPlaylist> itemsToParse, BaseParseTask parentTask) {
        super(itemsToParse, parentTask);
        parsedPlaylists = new ArrayList<>();
    }

    @Override
    protected void joinPartialResults(BaseParseResult<List<Playlist>> partialResult) {
        parsedPlaylists.addAll(partialResult.getParsedResults());
    }
}