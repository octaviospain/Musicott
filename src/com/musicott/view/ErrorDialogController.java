package com.musicott.view;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import com.musicott.error.Error;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ParseException;

public class ErrorDialogController {
	
	private Alert alert;
	
	public ErrorDialogController(Error type) {
		switch(type) {
		case IMPORT_ERROR:
			alert = new Alert(AlertType.WARNING);
			alert.setTitle("");
			alert.setHeaderText("Errors founded while importing");
			alert.setContentText("");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			for(ParseException ex: ErrorHandler.getInstance().getParseExceptions())
				ex.printStackTrace(pw);

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

			// Set expandable Exception into the dialog pane.
			alert.getDialogPane().setExpandableContent(expContent);
			break;
		}
	}
	
	public void showDialog() {
		alert.showAndWait();
	}
}