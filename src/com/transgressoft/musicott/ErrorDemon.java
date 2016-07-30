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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.transgressoft.musicott;

import javafx.application.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.view.MusicottController.*;

/**
 * Singleton class that handles the exceptions, showing an error message,
 * the stack trace, or a text area with several error messages
 *
 * @author Octavio Calleya
 * @version 0.9-b
 */
public class ErrorDemon {

	private static final double ALERT_WIDTH = 800;
	private static final double ALERT_HEIGHT = 300;

	private static ErrorDemon instance;
	private Stage mainStage;
	private Alert alert;
	private FlowPane helpContentTextFlowPane;

	private ErrorDemon() {
		Label text = new Label("Help Musicott to fix this kind of bugs. Report this error at");
		Hyperlink githubIssuesLink = new Hyperlink("https://github.com/octaviospain/Musicott/issues");
		HostServices hostServices = StageDemon.getInstance().getApplicationHostServices();
		githubIssuesLink.setOnAction(event -> hostServices.showDocument(githubIssuesLink.getText()));
		helpContentTextFlowPane = new FlowPane();
		helpContentTextFlowPane.getChildren().addAll(text, githubIssuesLink);
	}

	public static ErrorDemon getInstance() {
		if (instance == null)
			instance = new ErrorDemon();
		return instance;
	}

	/**
	 * Shows an error dialog with a message
	 *
	 * @param message The message to be shown
	 */
	public synchronized void showErrorDialog(String message) {
		showErrorDialog(message, "");
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
		showErrorDialog(message, content, exception, getMainStage().getScene());
	}

	/**
	 * Shows an error dialog with a message, a content text,
	 * and the stack trace of a exception. The {@link Dialog} will be
	 * placed on a given {@link Scene}.
	 *
	 * @param message    The message to be shown
	 * @param content    The content text of the error dialog
	 * @param exception  The exception error
	 * @param alertScene The <tt>Scene</tt> where the <tt>Dialog</tt> will be placed
	 */
	public synchronized void showErrorDialog(String message, String content, Exception exception, Scene alertScene) {
		Platform.runLater(() -> {
			alert = createErrorAlert(message, content, alertScene);
			if (exception != null) {
				StringWriter string = new StringWriter();
				PrintWriter printWriter = new PrintWriter(string);
				exception.printStackTrace(printWriter);
				List<String> singleErrorList = new ArrayList<>();
				singleErrorList.add(string.toString());
				addExpandableErrorMessages(singleErrorList);
			}
			alert.showAndWait();
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
			alert = createErrorAlert(message, content, getMainStage().getScene());
			if (errors != null)
				addExpandableErrorMessages(errors);
			alert.showAndWait();
		});
	}

	/**
	 * Inserts an expandable {@link TextArea} with a collection of <tt>String</tt> messages
	 * to the error dialog
	 *
	 * @param messages The collection of messages
	 */
	private void addExpandableErrorMessages(Collection<String> messages) {
		TextArea textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setWrapText(true);
		for (String s : messages)
			textArea.appendText(s + "\n");
		GridPane expandableArea = new GridPane();
		expandableArea.setMaxWidth(Double.MAX_VALUE);
		expandableArea.add(new Label("The exception stacktrace was:"), 0, 0);
		expandableArea.add(textArea, 0, 1);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);
		alert.getDialogPane().setExpandableContent(expandableArea);
	}

	/**
	 * Shows an error dialog with the logo of LastFM
	 *
	 * @param message The message of the error
	 * @param content The content text of the error dialog
	 */
	public synchronized void showLastFmErrorDialog(String message, String content) {
		Platform.runLater(() -> {
			Scene sceneWhereShow = getMainStage().getScene();
			alert = createErrorAlert(message, content, sceneWhereShow);
			alert.setGraphic(new ImageView(new Image(getClass().getResourceAsStream(LASTFM_LOGO))));
			alert.show();
		});
	}

	/**
	 * Private getter to get the main {@link Stage} of the application,
	 * where the error dialog would be shown unless other is specified.
	 *
	 * @return The primary <tt>Stage</tt> of the application
	 */
	private Stage getMainStage() {
		if (mainStage == null)
			mainStage = StageDemon.getInstance().getMainStage();
		return mainStage;
	}

	/**
	 * Creates an error {@link Alert} given a message and the content text to be shown
	 * in the description.  The {@link Dialog} will be placed on a given {@link Scene}.
	 *
	 * @param message    The message of the error
	 * @param content    The content text of the error dialog
	 * @param ownerScene The <tt>Scene</tt> where the <tt>Dialog</tt> will be placed
	 *
	 * @return An {@link Alert} object
	 */
	private Alert createErrorAlert(String message, String content, Scene ownerScene) {
		alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(message == null ? "Error" : message);
		alert.getDialogPane().getStylesheets().add(getClass().getResource(DIALOG_STYLE).toExternalForm());
		if (content == null)
			alert.getDialogPane().contentProperty().set(helpContentTextFlowPane);
		else
			alert.setContentText(content);
		if (ownerScene != null)
			alert.initOwner(ownerScene.getWindow());
		else {
			Scene errorScene = new Scene(new AnchorPane(), ALERT_WIDTH, ALERT_HEIGHT);
			alert.initOwner(errorScene.getWindow());
		}
		return alert;
	}
}
