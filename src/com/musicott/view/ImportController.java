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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.task.TaskPoolManager;
import com.musicott.util.Utils;

import javafx.application.Platform;
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
public class ImportController {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	@FXML
	private Button openButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Button importButton;
	@FXML
	private Label alsoAddLabel;
	@FXML
	private Label descriptionLabel;
	@FXML
	private Label infoLabel;
	@FXML
	private CheckBox cbM4a;
	@FXML
	private CheckBox cbWav;
	@FXML
	private CheckBox cbFlac;
	
	private Stage importStage;
	private FileFilter formatFilter;
	private File folder;
	private List<File> files;
	private String folderString;
	private Thread findFilesThread;
	private volatile ThreadStopFlag stop;
	
	public ImportController() {
	}
	
	@FXML
	private void initialize() {
		formatFilter = file -> {
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
		cbM4a.selectedProperty().addListener(observable -> {if(folder!= null) updateInfo();});
		cbWav.selectedProperty().addListener(observable -> {if(folder!= null) updateInfo();});
		cbFlac.selectedProperty().addListener(observable -> {if(folder!= null) updateInfo();});
		stop = new ThreadStopFlag();
	}
	
	public void setStage(Stage stage) {
		importStage = stage;
	}
	
	private void clear() {
		folder = null;
		infoLabel.setText("");
		importButton.setDisable(true);
		importStage.close();
		if(findFilesThread.isAlive())
			stop.flagStop();
	}
	
	private void updateInfo() {
		folderString = ".../"+folder.getParentFile().getName()+"/"+folder.getName();
		infoLabel.setText("Counting Files...");
		if(!importButton.isDisabled())
			importButton.setDisable(true);
		if(findFilesThread != null && findFilesThread.isAlive())
			stop.flagStop();
		stop.reset();
		findFilesThread = new Thread(() -> {
			files = Utils.getAllFilesInFolder(folder, formatFilter, 0, stop);
			Platform.runLater(() -> {
				if(!stop.stop()) {
					infoLabel.setText(""+files.size()+" files in "+folderString);
					importButton.setDisable(false);
				}
			});
		}, "Find files Thread");
		findFilesThread.start();
	}
	
	@FXML
	private void doImport() {
		TaskPoolManager.getInstance().parseFiles(files);
		SceneManager.getInstance().getRootController().setStatusMessage("Importing files");
		clear();
	}
	
	@FXML
	private void doOpen() {
		LOG.debug("Choosing folder to being imported");
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Choose folder");
		folder = chooser.showDialog(importStage);
		if(folder != null)
			updateInfo();
	}
	
	@FXML
	private void doCancel() {
		clear();
		LOG.info("Import cancelled");
	}

	public class ThreadStopFlag {
		private boolean stop = false;
		
		public void flagStop() {
			stop = true;
		}
		
		public void reset() {
			stop = false;
		}
		
		public boolean stop() {
			return stop;
		}
	}
}