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

import javafx.fxml.FXML;
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
	
	public ItunesImportController() {
	}
	
	@FXML
	private void initialize() {
		tpm = TaskPoolManager.getInstance();
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Open file..");
		chooser.getExtensionFilters().add(new ExtensionFilter("xml files (*.xml)", "*.xml"));
		openButton.setOnMouseClicked(event -> {
			itunesXMLFile = chooser.showOpenDialog(itunesImportStage);
			if(itunesXMLFile != null) {
				itunesFileLabel.setText(itunesXMLFile.toString());
				okButton.setDisable(false);
			}
		});
	}
	
	public void setStage(Stage stage) {
		itunesImportStage = stage;
	}
	
	@FXML
	private void doOK() {
		if(!importPolicyCheckBox.isSelected())
			importPolicy = HOLD_METADATA_POLICY;
		else
			importPolicy = HOLD_ITUNES_DATA_POLICY;
		tpm.parseItunesLibrary(itunesFileLabel.getText(), importPolicy, false, playCountCheckBox.isSelected());
		itunesImportStage.close();
	}
	
	@FXML
	private void doCancel() {
		itunesFileLabel.setText("Select 'iTunes Music Library.xml' file");
		okButton.setDisable(true);
		itunesImportStage.close();
	}
}