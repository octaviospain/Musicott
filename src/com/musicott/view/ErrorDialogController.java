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

package com.musicott.view;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import com.musicott.error.Error;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ParseException;

/**
 * @author Octavio Calleya
 *
 */
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