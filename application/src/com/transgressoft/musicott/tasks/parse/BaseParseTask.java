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

import com.transgressoft.musicott.view.*;
import javafx.application.*;
import javafx.concurrent.*;
import javafx.util.*;

import java.text.*;
import java.util.*;

/**
 * Base class of parse tasks of import music into the application.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.10-b
 */
public abstract class BaseParseTask extends Task<Void> implements ParseTask {

    protected ErrorDialogController errorDialog;
    protected NavigationController navigationController;

    protected Collection<String> parseErrors;
    protected long startMillis;

    public BaseParseTask(NavigationController navigationController, ErrorDialogController errorDialog) {
        this.errorDialog = errorDialog;
        this.navigationController = navigationController;
    }

    public void updateProgressTask() {
        int parsedItems = getNumberOfParsedItemsAndIncrement();
        int totalItemsToParse = getTotalItemsToParse();
        double progress = (double) parsedItems / totalItemsToParse;
        String statusMessage = Integer.toString(parsedItems) + " / " + Integer.toString(totalItemsToParse);
        Platform.runLater(() -> updateTaskProgressOnView(progress, statusMessage));
    }

    /**
     * Updates on the view the progress and a message of the task.
     * Should be called on the JavaFX Application Thread.
     */
    protected void updateTaskProgressOnView(double progress, String message) {
        navigationController.setStatusProgress(progress);
        navigationController.setStatusMessage(message);
    }

    protected void computeAndShowElapsedTime(int totalItemsParsed) {
        long endMillis = System.currentTimeMillis() - startMillis;
        Duration totalTaskTime = Duration.millis(endMillis);
        double timeValue = totalTaskTime.toSeconds() > 60 ? totalTaskTime.toMinutes() : totalTaskTime.toSeconds();
        DecimalFormat decimalFormat = new DecimalFormat("#.0");
        String timeString = totalTaskTime.toSeconds() > 60 ? " mins" : " secs";
        timeString = decimalFormat.format(timeValue) + timeString;
        String statusMessage = totalItemsParsed + " items in " + timeString;
        Platform.runLater(() -> updateTaskProgressOnView(0.0, statusMessage));
    }
}
