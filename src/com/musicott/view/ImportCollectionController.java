package com.musicott.view;

import java.io.File;

import com.musicott.SceneManager;
import com.musicott.task.ImportTask;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class ImportCollectionController {
	
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
		cbM4a = new CheckBox();
		cbWav = new CheckBox();
		cbFlac = new CheckBox();
	}
	
	public void setStage(Stage stage) {
		importStage = stage;
	}
	
	@FXML
	private void doImport() {
		ImportTask task = new ImportTask(folder,cbM4a.isSelected(),cbWav.isSelected(),cbFlac.isSelected());
		SceneManager.getInstance().showImportProgressScene(task);
	}
	
	@FXML
	private void doOpen() {
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
		Stage stage = (Stage) cancelButton.getScene().getWindow();
	    stage.close();
	}
}