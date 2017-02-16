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

package com.transgressoft.musicott.view;

import com.transgressoft.musicott.tests.*;
import javafx.application.*;
import javafx.stage.*;
import org.junit.Test;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
public class ErrorDialogControllerTest extends JavaFxTestBase {

    private ErrorDialogController errorAlertController;
    private String defaultErrorContent = "Improve Musicott reporting this error on github.";

    @Override
    public void start(Stage stage) throws Exception {
        testStage = stage;
        layout = MusicottController.ERROR_ALERT_LAYOUT;
        super.start(stage);
    }

    @Override
    @BeforeEach
    public void beforeEachTest() throws Exception {
        super.beforeEachTest();
        errorAlertController = (ErrorDialogController) controller;
    }

    @Test
    public void errorDialogMessageText() throws Exception {
        Platform.runLater(() -> errorAlertController.prepareDialog("Error message", null, null));

        Thread.sleep(200);

        assertEquals("Error message", errorAlertController.getErrorTitle());
        assertEquals(defaultErrorContent, errorAlertController.getErrorContent());
        assertEquals("", errorAlertController.getDetailsAreaText());
    }

    @Test
    public void errorDialogMessageWithContentTest() throws Exception {
        Platform.runLater(() -> errorAlertController.prepareDialog("Error message", "Content message", null));

        Thread.sleep(200);
        assertEquals("Error message", errorAlertController.getErrorTitle());
        assertEquals("Content message", errorAlertController.getErrorContent());
        assertEquals("", errorAlertController.getDetailsAreaText());
    }

    @Test
    public void errorDialogMessageWithContentAndExceptionTest() throws Exception {
        IllegalArgumentException exception = new IllegalArgumentException("Test exception");
        StackTraceElement[] stackTraceSample = new StackTraceElement[]{new StackTraceElement(getClass().getName(), "method", "file", 31415)};
        exception.setStackTrace(stackTraceSample);

        Platform.runLater(() -> errorAlertController.prepareDialog("Error message", null, exception));

        Thread.sleep(200);
        assertEquals("Error message", errorAlertController.getErrorTitle());
        assertEquals(defaultErrorContent, errorAlertController.getErrorContent());

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);

        assertEquals(stringWriter.toString() + "\n", errorAlertController.getDetailsAreaText());
    }

    @Test
    public void errorDialogWithErrorCollection() throws Exception {
        List<String> errors = Arrays.asList("Error 1", "Error 2", "Error 3");
        Platform.runLater(() -> errorAlertController.prepareDialogWithMessages("Error message", "Content message", errors));

        Thread.sleep(200);
        assertEquals("Error message", errorAlertController.getErrorTitle());
        assertEquals("Content message", errorAlertController.getErrorContent());
        assertEquals("Error 1\nError 2\nError 3\n", errorAlertController.getDetailsAreaText());
    }

    @Test
    public void lastFmErrorDialog() throws Exception {
        Platform.runLater(() -> errorAlertController.prepareLastFmDialog("LastFm error message", null));

        Thread.sleep(200);
        assertEquals("LastFm error message", errorAlertController.getErrorTitle());
        assertEquals(defaultErrorContent, errorAlertController.getErrorContent());
        assertEquals("", errorAlertController.getDetailsAreaText());
    }
}