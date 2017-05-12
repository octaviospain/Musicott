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

package com.transgressoft.musicott;

import com.google.inject.*;
import com.transgressoft.musicott.view.*;
import javafx.application.*;
import javafx.stage.Stage;

import java.util.*;

/**
 * Singleton class that handles the exceptions, showing an error message,
 * the stack trace, or a text area with several error messages
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
@Singleton
public class ErrorDemon {

    private Provider<StageDemon> stageDemon;
    private ErrorDialogController errorAlertController;
    private Stage alertStage;

    @Inject
    public ErrorDemon(Provider<StageDemon> stageDemon) {
        this.stageDemon = stageDemon;
    }

    void setErrorAlertStage(Stage stage) {
        alertStage = stage;
    }

    void setErrorAlertController(ErrorDialogController errorAlertController) {
        this.errorAlertController = errorAlertController;
    }

    /**
     * Shows an error dialog with a message
     *
     * @param message The message to be shown
     */
    public synchronized void showErrorDialog(String message) {
        showErrorDialog(message, null);
    }

    /**
     * Shows an error dialog with a message and a content text
     *
     * @param message The message to be shown
     * @param content The content text of the error dialog
     */
    public synchronized void showErrorDialog(String message, String content) {
        showErrorDialog(message, content, null);
    }

    /**
     * Shows an error dialog with a message, a content text,
     * and the stack trace of a exception
     *
     * @param message   The message to be shown
     * @param content   The content text of the error dialog
     * @param exception The exception error
     */
    public synchronized void showErrorDialog(String message, String content, Exception exception) {
        Platform.runLater(() -> {
            errorAlertController.prepareDialog(message, content, exception);
            stageDemon.get().showStage(alertStage);
        });
    }

    /**
     * Shows an error dialog with a message and a content text, and a collection of
     * error messages inside an expandable text area.
     *
     * @param message The message to be shown
     * @param content The content text of the error dialog
     * @param errors  The collection of error messages to be shown in the expandable area
     */
    public synchronized void showExpandableErrorsDialog(String message, String content, Collection<String> errors) {
        Platform.runLater(() -> {
            errorAlertController.prepareDialogWithMessages(message, content, errors);
            stageDemon.get().showStage(alertStage);
        });
    }

    /**
     * Shows an error dialog with the logo of LastFM
     *
     * @param message The message of the error
     * @param content The content text of the error dialog
     */
    public synchronized void showLastFmErrorDialog(String message, String content) {
        Platform.runLater(() -> {
            errorAlertController.prepareLastFmDialog(message, content);
            stageDemon.get().showStage(alertStage);
        });
    }
}
