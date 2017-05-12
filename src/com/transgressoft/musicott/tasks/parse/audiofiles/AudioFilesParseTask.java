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
import com.google.inject.assistedinject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.transgressoft.musicott.util.factories.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Extends from {@link BaseParseTask} to perform the operation of import several
 * audio files and add them to the {@link MusicLibrary}.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class AudioFilesParseTask extends BaseParseTask {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final PlayerFacade player;
    private final List<File> filesToParse;
    private final boolean playAtTheEnd;

    private Map<Integer, Track> parsedTracks;
    private int currentParsedFiles;
    private int totalFilesToParse;

    @Inject
    private ParseActionFactory parseActionFactory;

    @Inject
    public AudioFilesParseTask(ErrorDemon errorDemon, StageDemon stageDemon, PlayerFacade playerFacade,
            @Assisted List<File> files, @Assisted boolean playAtTheEnd) {
        super(errorDemon, stageDemon);
        player = playerFacade;
        filesToParse = files;
        this.playAtTheEnd = playAtTheEnd;
        currentParsedFiles = 0;
        totalFilesToParse = filesToParse.size();
    }

    @Override
    public int getNumberOfParsedItemsAndIncrement() {
        return ++ currentParsedFiles;
    }

    @Override
    public int getTotalItemsToParse() {
        return totalFilesToParse;
    }

    @Override
    protected Void call() {
        startMillis = System.currentTimeMillis();
        LOG.debug("Starting file importing");
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        AudioFilesParseAction action = parseActionFactory.create(filesToParse, this);
        BaseParseResult<Map<Integer, Track>> result = forkJoinPool.invoke(action);
        parsedTracks = result.getParsedResults();
        parseErrors = result.getParseErrors();
        return null;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        updateMessage("Parse succeeded");
        LOG.info("Parse task completed");
        computeAndShowElapsedTime(parsedTracks.size());

        if (! parseErrors.isEmpty())
            errorDemon.showExpandableErrorsDialog("Errors importing files", "", parseErrors);
        if (playAtTheEnd)
            player.addTracksToPlayQueue(parsedTracks.values(), true);
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        updateMessage("Parse cancelled");
        LOG.info("Parse task cancelled");
    }
}
