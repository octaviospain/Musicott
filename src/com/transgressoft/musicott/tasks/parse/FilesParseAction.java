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
import com.transgressoft.musicott.util.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * This class extends from {@link RecursiveTask} so it can be used inside a
 * {@link ForkJoinPool} in order to perform the parse of a collection of
 * audio files into instances of the system's {@link Track}.
 *
 * If it receives more than a certain amount of items to parse, the task is forked
 * with partitions of the items collection and their results joined after their completion.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 * @since 0.9.2-b
 */
public class FilesParseAction extends RecursiveTask<ParseResult<Map<Integer, Track>>> {

    private static final int MAX_FILES_TO_PARSE_PER_ACTION = 250;
    private static final int NUMBER_OF_ITEMS_PARTITIONS = 4;

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private List<File> filesToParse;
    private Map<Integer, Track> parsedTracks;
    private Collection<String> parseErrors;
    private BaseParseTask parentTask;

    /**
     * Constructor of {@link FilesParseAction}
     *
     * @param filesToParse The {@link List} of audio files to parse
     * @param parentTask   The reference to the parent {@link BaseParseTask} that called this action
     */
    public FilesParseAction(List<File> filesToParse, BaseParseTask parentTask) {
        this.filesToParse = filesToParse;
        this.parentTask = parentTask;
        parsedTracks = new HashMap<>();
        parseErrors = new ArrayList<>();
    }

    @Override
    protected ParseResult<Map<Integer, Track>> compute() {
        if (filesToParse.size() > MAX_FILES_TO_PARSE_PER_ACTION) {
            int subListsSize = filesToParse.size() / NUMBER_OF_ITEMS_PARTITIONS;
            ImmutableList<FilesParseAction> subActions = Lists.partition(filesToParse, subListsSize).stream()
                                                              .map(subList -> new FilesParseAction(subList, parentTask))
                                                              .collect(ImmutableList.toImmutableList());

            subActions.forEach(FilesParseAction::fork);
            subActions.forEach(action -> joinPartialResults(action.join()));
            LOG.debug("Forking parse of files into {} sub actions", subActions.size());
        }
        else
            filesToParse.forEach(this::parseFile);

        return new ParseResult<>(parsedTracks, parseErrors);
    }

    private void joinPartialResults(ParseResult<Map<Integer, Track>> partialResult) {
        parsedTracks.putAll(partialResult.getParsedItems());
        parseErrors.addAll(partialResult.getParseErrors());
    }

    private void parseFile(File fileToParse) {
        Optional<Track> currentTrack = parseFileToTrack(fileToParse);
        currentTrack.ifPresent(track -> parsedTracks.put(track.getTrackId(), track));
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
