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

import java.io.File;
import java.io.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.task.ParseFolderTask;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * @author Octavio Calleya
 *
 */
public class ImportCollectionController {
	
	private final Logger LOG = LoggerFactory.getLogger(ImportCollectionController.class.getName());
	
	@FXML
	private Button openButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Button importButton;
	@FXML
	private Label folderLabel;
	@FXML
	private Label alsoAddLabel;
	@FXML
	private Label infoLabel;
	@FXML
	private CheckBox cbM4a;
	@FXML
	private CheckBox cbWav;
	@FXML
	private CheckBox cbFlac;
	
	private Stage importStage;

	private File folder;
	
	public ImportCollectionController() {
	}
	
	@FXML
	private void initialize() {
	}
	
	public void setStage(Stage stage) {
		importStage = stage;
	}
	
	@FXML
	private void doImport() {
		FileFilter formatFilter = file -> {
			int pos = file.getName().lastIndexOf(".");
			String format = file.getName().substring(pos+1);
			if((cbM4a.isSelected() && format.equals("m4a")) ||
				cbWav.isSelected() && format.equals("wav") ||
				cbFlac.isSelected() && format.equals("flac") ||
				format.equals("mp3"))
				return true;
			else
				return false;
		};
		ParseFolderTask task = new ParseFolderTask(folder, formatFilter);
		SceneManager.getInstance().showImportProgressScene(task, false);
	}
	
	@FXML
	private void doOpen() {
		LOG.info("Choosing folder to being imported");
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Choose folder");
		folder = chooser.showDialog(importStage);
		if(folder != null) {
			folderLabel.setText(".../"+folder.getParentFile().getName()+"/"+folder.getName());
			importButton.setDisable(false);
		}
	}
	
	@FXML
	private void doCancel() {
		importStage.close();
		LOG.info("Import cancelled");
	}
}