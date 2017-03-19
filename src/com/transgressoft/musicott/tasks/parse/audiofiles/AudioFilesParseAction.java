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

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.transgressoft.musicott.util.*;

import java.io.*;
import java.util.*;

/**
 * This class extends from {@link FilesParseAction} in order to perform the
 * parse of a collection of audio files into instances of the system's {@link Track}.
 *
 * If it receives more than a certain amount of items to parse, the task is forked
 * with partitions of the items collection and their results joined after their completion.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 * @since 0.9.2-b
 */
public class AudioFilesParseAction extends FilesParseAction {

    private static final int MAX_FILES_TO_PARSE_PER_ACTION = 250;
    private static final int NUMBER_OF_PARTITIONS = 4;

    /**
     * Constructor of {@link AudioFilesParseAction}
     *
     * @param filesToParse The {@link List} of audio files to parse
     * @param parentTask   The reference to the parent {@link BaseParseTask} that called this action
     */
    public AudioFilesParseAction(List<File> filesToParse, BaseParseTask parentTask) {
        super(filesToParse, parentTask);
    }

    @Override
    protected FilesParseResult compute() {
        if (itemsToParse.size() > MAX_FILES_TO_PARSE_PER_ACTION)
            forkIntoSubActions();
        else {
            itemsToParse.forEach(this::parseItem);
            musicLibrary.addTracks(parsedTracks);
        }

        return new FilesParseResult(parsedTracks, parseErrors);
    }

    @Override
    protected int getNumberOfPartitions() {
        return NUMBER_OF_PARTITIONS;
    }

    @Override
    protected void parseItem(File item) {
        Optional<Track> currentTrack = parseFileToTrack(item);
        currentTrack.ifPresent(track -> {
            if (! musicLibrary.containsTrack(track))
                parsedTracks.put(track.getTrackId(), track);
        });
        parentTask.updateProgressTask();
    }

    private Optional<Track> parseFileToTrack(File file) {
        Optional<Track> newTrack;
        try {
            newTrack = Optional.of(MetadataParser.createTrack(file));
            LOG.debug("Parsed file {}: {}", file, newTrack);
        }
        catch (TrackParseException exception) {
            LOG.error("Error parsing file {}: ", file, exception);
            newTrack = Optional.empty();
            parseErrors.add(file + ": " + exception.getMessage());
        }
        return newTrack;
    }
}