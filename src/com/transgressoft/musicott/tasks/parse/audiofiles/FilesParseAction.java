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

package com.transgressoft.musicott.tasks.parse.audiofiles;

import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.transgressoft.musicott.util.guice.factories.*;

import java.io.*;
import java.util.*;

/**
 * This class extends from {@link BaseParseAction} specifying its type parameters
 * so it can be used to parse a {@link File} producing a {@link Map} in which contains
 * the track ids of the parsed tracks mapped to the tracks.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public abstract class FilesParseAction
        extends BaseParseAction<File, Map<Integer, Track>, BaseParseResult<Map<Integer, Track>>> {

    protected transient Map<Integer, Track> parsedTracks;

    @Inject
    private ParseActionFactory parseActionFactory;

    public FilesParseAction(List<File> itemsToParse, BaseParseTask parentTask) {
        super(itemsToParse, parentTask);
        parsedTracks = new HashMap<>();
    }

    @Override
    protected BaseParseAction<File, Map<Integer, Track>, BaseParseResult<Map<Integer, Track>>> parseActionMapper(
            List<File> subItems) {
        return parseActionFactory.create(subItems, parentTask);
    }

    @Override
    protected void joinPartialResults(BaseParseResult<Map<Integer, Track>> partialResult) {
        parsedTracks.putAll(partialResult.getParsedResults());
        parseErrors.addAll(partialResult.getParseErrors());
    }
}
