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

import com.musicott.SceneManager;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;

/**
 * @author Octavio Calleya
 *
 */
public class ProgressImportController {

	@FXML
	private BorderPane pane;
	@FXML
	private ProgressBar pBar;
	@FXML
	private Button cancelTaskButton;
	private Task<?> task;
	
	public ProgressImportController() {
	}
	
	@FXML
	private void initialize() {
		pBar = new ProgressBar();
		pBar.setId("pBar");
		pBar.setPrefSize(470.0, 20.0);
		pane.setCenter(pBar);
	}
	
	public void hideCancelButton() {
		cancelTaskButton.setVisible(false);
	}
	
	public void runTask() {
		Thread t = new Thread(task);
		t.setDaemon(true);
		t.start();
	}
	
	public void cancelTask() {
		task.cancel();
		SceneManager.getInstance().closeImportScene();
	}
	
	public void setTask(Task<?> task) {
		this.task = task;
		pBar.progressProperty().bind(this.task.progressProperty());
	}
	
	public void setIndeterminate() {
		initialize();
	}
}