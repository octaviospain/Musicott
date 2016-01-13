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
 */

package com.musicott.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;

import com.musicott.SceneManager;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/**
 * @author Octavio Calleya
 *
 */
public class ErrorHandler {

	private static ErrorHandler instance;
	private HostServices hostServices;
	private Stage mainStage;
	private Alert alert;
	private FlowPane helpContentTextFP;

	private ErrorHandler() {
		Label text = new Label("Help Musicott to fix this kind of bugs. Report this error at");
		Hyperlink githubIssuesLink = new Hyperlink("https://github.com/octaviospain/Musicott/issues");
		githubIssuesLink.setOnAction(event -> hostServices.showDocument(githubIssuesLink.getText()));
		helpContentTextFP = new FlowPane();
		helpContentTextFP.getChildren().addAll(text, githubIssuesLink);
	}
	
	public static ErrorHandler getInstance() {
		if(instance == null)
			instance = new ErrorHandler();
		return instance;
	}
	
	public void setApplicationHostServices(HostServices hostServices) {
		this.hostServices= hostServices;
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
	
	public synchronized void showErrorDialog(String message, String content, Exception exception, Scene alertStage) {
		Platform.runLater(() -> {
			alert = createAlert(message, content, alertStage);
			if(exception != null)
				addExpandableStackTrace(exception);
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
			alert = createAlert(message, content, getMainStage().getScene());
			alert.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/lastfm-logo.png"))));
			alert.show();
		});
	}
	
	private Stage getMainStage() {
		if(mainStage == null)
			mainStage = SceneManager.getInstance().getMainStage();
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
	
	private String buildStackTraceContentText(Exception... exceptions) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		for(Exception e: exceptions)
			e.printStackTrace(pw);
		return sw.toString();
	}
	
	private void addExpandableStackTrace(Exception... exceptions) {
		TextArea textArea = new TextArea(buildStackTraceContentText(exceptions));
		textArea.setEditable(false);
		textArea.setWrapText(true);
		
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(new Label("The exception stacktrace was:"), 0, 0);
		expContent.add(textArea, 0, 1);
		alert.getDialogPane().setExpandableContent(expContent);
	}
	
	private void addExpandableErrorMessages(Collection<String> messages) {
		TextArea textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setWrapText(true);
		for(String s: messages)
			textArea.appendText(s+"\n");
		
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(new Label("The exception stacktrace was:"), 0, 0);
		expContent.add(textArea, 0, 1);
		alert.getDialogPane().setExpandableContent(expContent);		
	}
}