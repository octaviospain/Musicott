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

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.view.*;
import javafx.application.*;
import javafx.concurrent.*;
import javafx.util.*;

import java.util.*;

/**
 * Base class of parse tasks of import music into the application.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 * @since 0.9.2-b
 */
public abstract class BaseParseTask extends Task<Void> {

    protected Map<Integer, Track> parsedTracks;
    protected Collection<String> parseErrors;
    protected long startMillis;

    protected StageDemon stageDemon = StageDemon.getInstance();
    protected MusicLibrary musicLibrary = MusicLibrary.getInstance();
    protected ErrorDemon errorDemon = ErrorDemon.getInstance();
    protected NavigationController navigationController = stageDemon.getNavigationController();

    public synchronized void updateProgressTask() {
        int parsedItems = getNumberOfParsedItemsAndIncrement();
        int totalItemsToParse = getTotalItemsToParse();
        double progress = (double) parsedItems / totalItemsToParse;
        String statusMessage = Integer.toString(parsedItems) + " / " + Integer.toString(totalItemsToParse);
        Platform.runLater(() -> updateTaskProgressOnView(progress, statusMessage));
    }

    protected abstract int getNumberOfParsedItemsAndIncrement();

    protected abstract int getTotalItemsToParse();

    /**
     * Updates on the view the progress and a message of the task.
     * Should be called on the JavaFX Application Thread.
     */
    protected void updateTaskProgressOnView(double progress, String message) {
        stageDemon.getNavigationController().setStatusProgress(progress);
        stageDemon.getNavigationController().setStatusMessage(message);
    }

    protected abstract void addResultsToMusicLibrary();

    protected void computeAndShowElapsedTime() {
        long endMillis = System.currentTimeMillis() - startMillis;
        double totalTaskTime = Duration.millis(endMillis).toSeconds();
        if (totalTaskTime > 60)
            totalTaskTime = Duration.millis(endMillis).toMinutes();
        String statusMessage = parsedTracks.size() + " in " + totalTaskTime + " mins";
        Platform.runLater(() -> updateTaskProgressOnView(0.0, statusMessage));
    }
}