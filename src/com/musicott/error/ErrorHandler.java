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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.musicott.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.musicott.SceneManager;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
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
	private Stage mainStage;
	private Map<ErrorType,Stack<Exception>> mapExceptions;
	private Alert alert;

	private ErrorHandler() {
		mapExceptions = new HashMap<ErrorType,Stack<Exception>>();
		mainStage = SceneManager.getInstance().getApplication().getStage();
	}
	
	public static ErrorHandler getInstance() {
		if(instance == null)
			instance = new ErrorHandler();
		return instance;
	}
	
	public void addError(Exception ex, ErrorType type) {
		if(mapExceptions.containsKey(type))
			mapExceptions.get(type).push(ex);
		else {
			Stack<Exception> stack = new Stack<Exception>();
			stack.push(ex);
			mapExceptions.put(type, stack);
		}
	}
	
	public boolean hasErrors(ErrorType type) {
		boolean has = false;
		if(!mapExceptions.isEmpty() && mapExceptions.containsKey(type) && !mapExceptions.get(type).isEmpty())
			has = true;
		return has;
	}

	public Stack<Exception> getErrors(ErrorType type){
		return mapExceptions.get(type);
	}
	
	public void showErrorDialog(ErrorType type) {
		showErrorDialog(mainStage.getScene(), type);
	}
	
	public void showErrorDialog(Scene ownerScene, ErrorType type) {
		alert = new Alert(AlertType.ERROR);
		alert.initOwner(ownerScene.getWindow());
		alert.setTitle("Error");
		alert.setHeaderText("Error");
		switch(type) {
			case COMMON:
				if(mapExceptions.get(type).size() == 1 && mapExceptions.get(type).peek() instanceof CommonException)
					alert.getDialogPane().contentProperty().set(new Label(mapExceptions.get(type).pop().getMessage()));
				else
					setExpandable(getErrors(type));
				break;
			case METADATA:
				alert.setHeaderText("Error(s) writing metadata on file(s)");
				setExpandable(getErrors(type));
				break;
			case PARSE:
				alert.setHeaderText("Error(s) parsing file(s)");
				setExpandable(getErrors(type));
				break;
			case FATAL:
				alert.setHeaderText("Fatal error");
				setExpandable(getErrors(type));
				Label text = new Label(" Please help Musicott to fix this kind of bugs. Report this error at");
				Hyperlink githubIssuesLink = new Hyperlink("https://github.com/octaviospain/Musicott/issues");
				githubIssuesLink.setOnAction(event -> {
					SceneManager.getInstance().getApplication().getHostServices().showDocument(githubIssuesLink.getText());
				});
				FlowPane fp = new FlowPane();
				fp.getChildren().addAll(text, githubIssuesLink);
				alert.getDialogPane().contentProperty().set(fp);
				break;
		}
		alert.showAndWait();
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