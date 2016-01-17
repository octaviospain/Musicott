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

package com.musicott;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.view.EditController;
import com.musicott.view.PlayQueueController;
import com.musicott.view.PreferencesController;
import com.musicott.view.RootController;
import com.musicott.model.Track;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @author Octavio Calleya
 *
 */
public class SceneManager {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	protected static final String LAYOUTS_PATH = "/view/";
	protected static final String ROOT_LAYOUT = "RootLayout.fxml";
	protected static final String PRELOADER_LAYOUT = "PreloaderPromptLayout.fxml";
	protected static final String EDIT_LAYOUT = "EditLayout.fxml";
	protected static final String PLAYQUEUE_LAYOUT = "PlayQueueLayout.fxml";
	protected static final String PROGRESS_LAYOUT = "ProgressLayout.fxml";
	protected static final String PREFERENCES_LAYOUT = "PreferencesLayout.fxml";
	
	private Stage mainStage, editStage, progressStage, preferencesStage;
	
	private EditController editController;
	private RootController rootController;
	private PlayQueueController playQueueController;
	private PreferencesController preferencesController;
	
	private static volatile SceneManager instance;
	private static ErrorHandler errorHandler;
	
	private List<Track> tracksToEdit;
	
	private SceneManager() {
	}
	
	public static SceneManager getInstance() {
		if(instance == null) {
			instance = new SceneManager();
			errorHandler = ErrorHandler.getInstance();
		}
		return instance;
	}
	
	protected void setMainStage(Stage mainStage) {
		this.mainStage = mainStage;
	}
	
	public Stage getMainStage() {
		return mainStage;
	}
	
	protected Stage getPreferencesStage() {
		return preferencesStage;
	}
	
	protected void setRootController(RootController rootController) {
		this.rootController = rootController;
	}
	
	public RootController getRootController() {
		return rootController;
	}
	
	protected void setPlayQueueController(PlayQueueController playQueueController) {
		this.playQueueController = playQueueController;
	}
	
	public PlayQueueController getPlayQueueController() {
		return this.playQueueController;
	}
	
	public PreferencesController getPreferencesController() {
		return this.preferencesController;
	}
	
	public void openEditScene(List<Track> selection) {
		tracksToEdit = selection;
		openStage(EDIT_LAYOUT);
	}

	public void openPreferencesScene() {
		openStage(PREFERENCES_LAYOUT);
	}	
	
	public void openIndeterminatedProgressScene() {
		openStage(PROGRESS_LAYOUT);
	}
	
	public void closeIndeterminatedProgressScene() {
		progressStage.close();
	}
	
	private void openStage(String layout) {
		Stage stageToOpen = null;
		switch(layout) {
			case EDIT_LAYOUT:
				stageToOpen = editStage = editStage == null ? initStage(layout, "Edit") : editStage; break;
			case PREFERENCES_LAYOUT:
				stageToOpen = preferencesStage = preferencesStage == null ? initStage(layout, "Preferences") : preferencesStage; break; 
			case PROGRESS_LAYOUT:
				stageToOpen = progressStage = progressStage == null ? initStage(layout, "") : progressStage; break;
		}
		if(stageToOpen != null) {
			if(layout.equals(EDIT_LAYOUT))
				editController.setSelection(tracksToEdit);
			else if(layout.equals(PREFERENCES_LAYOUT))
				preferencesController.load();
			stageToOpen.showAndWait();
		}
	}
	
	private Stage initStage(String layout, String title) {
		Stage newStage;
		try {
			newStage = new Stage();
			newStage.setTitle(title);
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource(LAYOUTS_PATH + layout));
			AnchorPane baseLayout = (AnchorPane) loader.load();
			
			if(layout.equals(PROGRESS_LAYOUT))
				newStage.setOnCloseRequest(event -> event.consume());
			else if(layout.equals(EDIT_LAYOUT)) {
				editController = (EditController) loader.getController();
				editController.setStage(newStage);
			} else if(layout.equals(PREFERENCES_LAYOUT)) {
				preferencesController = (PreferencesController) loader.getController();
				preferencesController.setStage(newStage);
			}
			
			Scene newScene = new Scene(baseLayout);
			newStage.initModality(Modality.APPLICATION_MODAL);
			newStage.initOwner(newScene.getWindow());
			newStage.setScene(newScene);
			newStage.setResizable(false);
		} catch(IOException e) {
			LOG.error("Error opening " + layout, e);
			errorHandler.showErrorDialog("Error opening " + layout, null, e);
			newStage = null;
		}
		return newStage;
	}
}