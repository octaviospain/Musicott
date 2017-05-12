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

import com.google.inject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.tests.*;
import com.transgressoft.musicott.util.*;
import javafx.application.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.testfx.api.*;
import org.testfx.framework.junit5.*;
import org.testfx.util.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.view.MusicottController.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
public class ErrorDialogControllerTest extends JavaFxTestBase {

    static final String defaultErrorContent = "Improve Musicott reporting this error on github.";

    ErrorDialogController errorAlertController;
    Button okButton;
    Label titleLabel;

    @Start
    public void start(Stage stage) throws Exception {
        loader = new FXMLControllerLoader(ErrorDialogControllerTest.class.getResource(ERROR_ALERT_LAYOUT), null,
                                          new FXGuiceInjectionBuilderFactory(injector), injector);

        Parent root = loader.load();
        errorAlertController = loader.getController();
        stage.setScene(new Scene(root));
        stage.show();
        stage.toFront();
    }

    @BeforeAll
    public static void beforeAll() {
        injector = Guice.createInjector(binder -> binder.bind(StageDemon.class).toInstance(mock(StageDemon.class)));
    }

    @BeforeEach
    void beforeEachTest(FxRobot fxRobot) throws Exception {
        okButton = find(fxRobot, "#okButton");
        titleLabel = find(fxRobot, "#errorTitle");
    }

    @Test
    @DisplayName ("Single error message")
    void errorDialogMessageText() throws Exception {
        Platform.runLater(() -> errorAlertController.prepareDialog("Error message", null, null));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("Error message", errorAlertController.getErrorTitle());
        assertEquals(defaultErrorContent, errorAlertController.getErrorContent());
        assertEquals("", errorAlertController.getDetailsAreaText());
    }

    @Test
    @DisplayName ("Error message with content message")
    void errorDialogMessageWithContentTest() throws Exception {
        Platform.runLater(() -> errorAlertController.prepareDialog("Error message", "Content message", null));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("Error message", errorAlertController.getErrorTitle());
        assertEquals("Content message", errorAlertController.getErrorContent());
        assertEquals("", errorAlertController.getDetailsAreaText());
    }

    @Test
    @DisplayName ("Error message with exception")
    void errorDialogMessageWithContentAndExceptionTest() throws Exception {
        IllegalArgumentException exception = new IllegalArgumentException("Test exception");
        StackTraceElement[] stackTraceSample = new StackTraceElement[]{
                new StackTraceElement(getClass().getName(), "method", "file", 31415)};
        exception.setStackTrace(stackTraceSample);

        Platform.runLater(() ->  errorAlertController.prepareDialog("Error message", null, exception));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("Error message", errorAlertController.getErrorTitle());
        assertEquals(defaultErrorContent, errorAlertController.getErrorContent());

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);

        assertEquals(stringWriter.toString() + "\n", errorAlertController.getDetailsAreaText());
    }

    @Test
    @DisplayName ("Error message with error collection")
    void errorDialogWithErrorCollection() throws Exception {
        List<String> errors = Arrays.asList("Error 1", "Error 2", "Error 3");
        Platform.runLater(
                () -> errorAlertController.prepareDialogWithMessages("Error message", "Content message", errors));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("Error message", errorAlertController.getErrorTitle());
        assertEquals("Content message", errorAlertController.getErrorContent());
        assertEquals("Error 1\nError 2\nError 3\n", errorAlertController.getDetailsAreaText());
    }

    @Test
    @DisplayName ("Lastfm error message")
    void lastFmErrorDialog() throws Exception {
        Platform.runLater(() -> errorAlertController.prepareLastFmDialog("LastFm error message", null));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("LastFm error message", errorAlertController.getErrorTitle());
        assertEquals(defaultErrorContent, errorAlertController.getErrorContent());
        assertEquals("", errorAlertController.getDetailsAreaText());
    }
}