package com.musicott.view;

import com.musicott.SceneManager;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;

public class ProgressImportController {

	@FXML
	private BorderPane pane;
	@FXML
	private ProgressBar pBar;
	@FXML
	private Button cancelTaskButton;
	private Task<?> importTask;
	
	public ProgressImportController() {
	}
	
	@FXML
	private void initialize() {
		pBar = new ProgressBar();
		pBar.setId("pBar");
		pBar.setPrefSize(470.0, 20.0);
		pane.setCenter(pBar);
	}
	
	public void runTask() {
		Thread t = new Thread(importTask);
		t.setDaemon(true);
		t.start();
	}
	
	public void cancelTask() {
		importTask.cancel();
		SceneManager.getInstance().closeImportScene();
	}
	
	public void setTask(Task<?> task) {
		importTask = task;
		pBar.progressProperty().bind(importTask.progressProperty());
	}
}