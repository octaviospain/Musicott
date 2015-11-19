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

package com.musicott.view;

import com.musicott.task.TaskPoolManager;
import static com.musicott.task.ItunesImportTask.*;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * @author Octavio Calleya
 *
 */
public class ItunesImportController {

	@FXML
	private Label itunesFileLabel;
	@FXML
	private Button openButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Button okButton;
	@FXML
	private CheckBox importPolicyCheckBox;
	@FXML
	private CheckBox playCountCheckBox;
	
	private Stage itunesImportStage;
	private TaskPoolManager tpm;
	private File itunesXMLFile;
	private int importPolicy;
	private String defaultText;
	private Scanner scnr;
	
	public ItunesImportController() {
	}
	
	@FXML
	private void initialize() {
		defaultText = "Select 'iTunes Music Library.xml' file";
		tpm = TaskPoolManager.getInstance();
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Open file..");
		chooser.getExtensionFilters().add(new ExtensionFilter("xml files (*.xml)", "*.xml"));
		openButton.setOnMouseClicked(event -> {
			okButton.setDisable(true);
			itunesXMLFile = chooser.showOpenDialog(itunesImportStage);
			if(itunesXMLFile != null) {
				Thread xmlValidatorThread = new Thread(() -> {
					if(isValidItunesXML())
						Platform.runLater(() -> validXML());
					else
						Platform.runLater(() -> invalidXML());
				}, "Itunes xml validator thread");
				xmlValidatorThread.start();
			}
		});
	}
	
	public void setStage(Stage stage) {
		itunesImportStage = stage;
		itunesImportStage.setOnCloseRequest(event -> doCancel());
	}
	
	private void validXML() {
		String pathText = itunesXMLFile.toString();
		if(pathText.length() > 35)
			pathText = ".."+pathText.substring(pathText.length() - 35);
		itunesFileLabel.setText(pathText);
		okButton.setDisable(false);
	}
	
	private void invalidXML() {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("");
		alert.setHeaderText("Error");
		alert.setContentText("The seleted xml file is not valid");
		alert.showAndWait();
	}
	
	private boolean isValidItunesXML() {
		boolean valid = true;
		try {
			scnr = new Scanner(itunesXMLFile);
			scnr.useDelimiter(Pattern.compile(">"));
			if(!(scnr.hasNextLine() && scnr.nextLine().contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")))
					valid = false;
			if(!(scnr.hasNextLine() && scnr.nextLine().contains("<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">")))
					valid = false;
		} catch (IOException e) {
			valid = false;
		}
		return valid;
	}
	
	@FXML
	private void doOK() {
		if(!importPolicyCheckBox.isSelected())
			importPolicy = HOLD_METADATA_POLICY;
		else
			importPolicy = HOLD_ITUNES_DATA_POLICY;
		tpm.parseItunesLibrary(itunesXMLFile.toString(), importPolicy, false, playCountCheckBox.isSelected());
		itunesImportStage.close();
	}
	
	@FXML
	private void doCancel() {
		itunesFileLabel.setText(defaultText);
		okButton.setDisable(true);
		itunesImportStage.close();
	}
}