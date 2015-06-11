package com.musicott.view;

import com.musicott.task.TestTask;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;

public class TestViewController {
	@FXML
	private Label textLabel;
	@FXML
	private Button startButton;
	@FXML
	private Button cancelButton;
	@FXML
	private BorderPane bPane;
	@FXML
	private ProgressBar pBar;
	@FXML
	private ProgressIndicator pIndic;
	
	private TestTask theTask;
	
	public TestViewController() {
	}
	
	private void prepareTask() {
		theTask=new TestTask(this);
		pBar.progressProperty().unbind();
		pBar.setProgress(0);
		pBar.progressProperty().bind(theTask.progressProperty());
		pIndic.progressProperty().unbind();
		pIndic.setProgress(0);
		pIndic.progressProperty().bind(theTask.progressProperty());
	}
	
	public void finished() {
		cancelButton.setDisable(true);
		startButton.setDisable(false);
		textLabel.setText(theTask.getMessage());
		prepareTask();
	}
	
	@FXML
	private void initialize() {
		cancelButton.setDisable(true);
		theTask=new TestTask(this);
		pBar=new ProgressBar();
		pIndic=new ProgressIndicator();
		pBar.progressProperty().bind(theTask.progressProperty());
		pIndic.progressProperty().bind(pBar.progressProperty());
		bPane.setTop(pBar);
		bPane.setCenter(pIndic);
	}
	
	@FXML
	private void handleStart() {
		Thread t=new Thread(theTask);
		t.setDaemon(true);
		t.start();
		cancelButton.setDisable(false);
		startButton.setDisable(true);
		textLabel.setText("Running!");
	}

	@FXML
	private void handleCancel() {
		theTask.cancel();
		cancelButton.setDisable(true);
		startButton.setDisable(false);
		textLabel.setText(theTask.getMessage());
		prepareTask();
	}
}