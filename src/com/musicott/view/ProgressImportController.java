package com.musicott.view;

import com.musicott.task.ImportTask;

import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;

public class ProgressImportController {

	@FXML
	private BorderPane pane;
	@FXML
	private ProgressBar pBar;
	private ImportTask importTask;
	
	public ProgressImportController() {
	}
	
	@FXML
	private void initialize() {
		pBar = new ProgressBar();
		pBar.setPrefSize(470.0, 20.0);
		pane.setCenter(pBar);
	}
	
	public void runTask() {
		Thread t = new Thread(importTask);
		t.setDaemon(true);
		t.start();
	}
	
	public void setTask(ImportTask task) {
		importTask = task;
		pBar.progressProperty().bind(importTask.progressProperty());
	}
}