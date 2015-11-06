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
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;

import javafx.application.HostServices;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	private HostServices hostServices;
	private static volatile ErrorHandler instance;
	private SceneManager sc;
	private Stage mainStage;
	private Map<ErrorType,Stack<Exception>> mapExceptions;
	private Alert alert;
	private String dialogStyle;

	private ErrorHandler() {
		sc = SceneManager.getInstance();
		mapExceptions = new HashMap<ErrorType,Stack<Exception>>();
		dialogStyle = "-fx-focus-color: rgb(73, 73, 73);" +
				  "-fx-faint-focus-color: transparent;" +
				  "-fx-background-color: transparent, transparent, transparent, -fx-body-color;" +
				  "-fx-background-radius: 0, 0, 0, 0";
	}
	
	public static ErrorHandler getInstance() {
		if(instance == null)
			instance = new ErrorHandler();
		return instance;
	}
	
	public void setApplicationHostServices(HostServices hostServices) {
		this.hostServices= hostServices;
	}
	
	public void addError(Exception ex, ErrorType type) {
		LOG.debug("Error handled:", ex);
		if(mapExceptions.containsKey(type))
			mapExceptions.get(type).push(ex);
		else {
			Stack<Exception> stack = new Stack<Exception>();
			stack.push(ex);
			mapExceptions.put(type, stack);
		}
	}
	
	public boolean hasErrors(ErrorType... type) {
		if(!mapExceptions.isEmpty())
			for(ErrorType et: type)
				if(mapExceptions.containsKey(et) && !mapExceptions.get(et).isEmpty())
					return true;
		return false;
	}

	public Stack<Exception> getErrors(ErrorType type){
		return mapExceptions.get(type);
	}
	
	public void showErrorDialog(ErrorType type) {
		if(mainStage == null)
			mainStage = sc.getMainStage();
		showErrorDialog(mainStage.getScene(), type);
	}
	
	public void showErrorDialog(Scene ownerScene, ErrorType... types) {
		alert = new Alert(AlertType.ERROR);
		alert.getDialogPane().setStyle(dialogStyle);
		if(ownerScene != null)
			alert.initOwner(ownerScene.getWindow());
		else {
			Scene errorScene = new Scene(new AnchorPane(),800,300);
			alert.initOwner(errorScene.getWindow());			
		}
		alert.setTitle("Error");
		alert.setHeaderText("Error");
		for(ErrorType et: types) {
			switch(et) {
				case COMMON:
					if(mapExceptions.get(et).size() == 1 && mapExceptions.get(et).peek() instanceof CommonException)
						alert.getDialogPane().contentProperty().set(new Label(mapExceptions.get(et).pop().getMessage()));
					else
						setExpandable(getErrors(et));
					break;
				case METADATA:
					alert.setHeaderText("Error(s) writing metadata on file(s)");
					setExpandable(getErrors(et));
					break;
				case PARSE:
					alert.setHeaderText("Error(s) parsing file(s)");
					setExpandable(getErrors(et));
					break;
				case FATAL:
					alert.setHeaderText("Fatal error");
					setExpandable(getErrors(et));
					Label text = new Label(" Please help Musicott to fix this kind of bugs. Report this error at");
					Hyperlink githubIssuesLink = new Hyperlink("https://github.com/octaviospain/Musicott/issues");
					githubIssuesLink.setOnAction(event -> hostServices.showDocument(githubIssuesLink.getText()));
					FlowPane fp = new FlowPane();
					fp.getChildren().addAll(text, githubIssuesLink);
					alert.getDialogPane().contentProperty().set(fp);
					break;
			}
			LOG.info("Showing error dialog type {}", et);
			alert.showAndWait();
		}
	}
	
	private void setExpandable(Stack<Exception> exceptions) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		while(!exceptions.isEmpty())
			exceptions.pop().printStackTrace(pw);

		TextArea textArea = new TextArea(sw.toString());
		textArea.setEditable(false);
		textArea.setWrapText(true);

		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(new Label("The exception stacktrace was:"), 0, 0);
		expContent.add(textArea, 0, 1);
		alert.getDialogPane().setExpandableContent(expContent);	
	}
}