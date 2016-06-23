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

package com.musicott;

import javafx.application.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;
import java.util.*;

/**
 * @author Octavio Calleya
 */
public class ErrorDemon {

	private static ErrorDemon instance;
	private Stage mainStage, preferencesStage;
	private Alert alert;
	private FlowPane helpContentTextFP;
	private TextArea textArea;
	private GridPane expandableArea;

	private ErrorDemon() {
		Label text = new Label("Help Musicott to fix this kind of bugs. Report this error at");
		Hyperlink githubIssuesLink = new Hyperlink("https://github.com/octaviospain/Musicott/issues");
		githubIssuesLink.setOnAction(event -> StageDemon.getInstance().getApplicationHostServices().showDocument(githubIssuesLink.getText()));
		helpContentTextFP = new FlowPane ();
		helpContentTextFP.getChildren().addAll(text, githubIssuesLink);
	}

	public static ErrorDemon getInstance() {
		if(instance == null)
			instance = new ErrorDemon();
		return instance;
	}

	public synchronized void showErrorDialog(String message) {
		showErrorDialog(message, "");
	}

	public synchronized void showErrorDialog(String message, String content) {
		showErrorDialog(message, content, null);
	}

	public synchronized void showErrorDialog(String message, String content, Exception exception) {
		showErrorDialog(message, content, exception, getMainStage().getScene());
	}

	public synchronized void showErrorDialog(String message, String content, Exception exception, Scene alertScene) {
		Platform.runLater(() -> {
			alert = createAlert(message, content, alertScene);
			if(exception != null) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				exception.printStackTrace(pw);
				List<String> singleErrorList = new ArrayList<String>();
				singleErrorList.add(sw.toString());
				addExpandableErrorMessages(singleErrorList);
			}
			alert.showAndWait();
		});
	}

	public synchronized void showExpandableErrorsDialog(String message, String content, Collection<String> errors) {
		Platform.runLater(() -> {
			alert = createAlert(message, content, getMainStage().getScene());
			if(errors != null)
				addExpandableErrorMessages(errors);
			alert.showAndWait();
		});
	}

	public synchronized void showLastFMErrorDialog(String message, String content) {
		Platform.runLater(() -> {
			preferencesStage = StageDemon.getInstance().getPreferencesStage();
			Scene sceneWhereShow = preferencesStage != null && preferencesStage.isShowing() ? preferencesStage.getScene() : getMainStage().getScene();
			alert = createAlert(message, content, sceneWhereShow);
			alert.setGraphic(new ImageView (new Image(getClass().getResourceAsStream("/images/lastfm-logo.png"))));
			alert.show();
		});
	}

	private Stage getMainStage() {
		if(mainStage == null)
			mainStage = StageDemon.getInstance().getMainStage();
		return mainStage;
	}

	private Alert createAlert(String message, String content, Scene ownerScene) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(message == null ? "Error" : message);
		alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());
		if(content == null)
			alert.getDialogPane().contentProperty().set(helpContentTextFP);
		else
			alert.setContentText(content);
		if(ownerScene != null)
			alert.initOwner(ownerScene.getWindow());
		else {
			Scene errorScene = new Scene(new AnchorPane(), 800, 300);
			alert.initOwner(errorScene.getWindow());
		}
		return alert;
	}

	private void addExpandableErrorMessages(Collection<String> messages) {
		textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setWrapText(true);
		for(String s: messages)
			textArea.appendText(s + "\n");
		expandableArea = new GridPane();
		expandableArea.setMaxWidth(Double.MAX_VALUE);
		expandableArea.add(new Label("The exception stacktrace was:"), 0, 0);
		expandableArea.add(textArea, 0, 1);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);
		alert.getDialogPane().setExpandableContent(expandableArea);
	}
}
