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
 * Copyright (C) 2005, 2006 Octavio Calleya
 */

package com.musicott;

import com.musicott.model.*;
import com.musicott.view.*;
import javafx.application.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

import static com.musicott.view.MusicottView.*;

/**
 * @author Octavio Calleya
 *
 */
public class StageDemon {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private final Image COVER_IMAGE = new Image(getClass().getResourceAsStream(DEFAULT_COVER_IMAGE));

	private Stage mainStage, editStage, progressStage, preferencesStage;
	
	private EditController editController;
	private RootController rootController;
	private NavigationController navigationController;
	private PlayQueueController playQueueController;
	private PreferencesController preferencesController;
	private PlayerController playerController;
	
	private static StageDemon instance;
	private static ErrorDemon errorDemon;
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	private HostServices hostServices;
	
	private StageDemon() {}
	
	public static StageDemon getInstance() {
		if(instance == null) {
			instance = new StageDemon();
			errorDemon = ErrorDemon.getInstance();
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
	
	protected void setNavigationController(NavigationController navigationController) {
		this.navigationController = navigationController;
	}
	
	public NavigationController getNavigationController() {
		return navigationController;
	}
	
	protected void setPlayQueueController(PlayQueueController playQueueController) {
		this.playQueueController = playQueueController;
	}
	
	public PlayQueueController getPlayQueueController() {
		return playQueueController;
	}
	
	protected void setPlayerController(PlayerController playerController) {
		this.playerController = playerController;
	}
	
	public PlayerController getPlayerController() {
		return playerController;
	}
	
	public PreferencesController getPreferencesController() {
		return preferencesController;
	}
	
	public void setApplicationHostServices(HostServices hostServices) {
		this.hostServices= hostServices;
	}
	
	public HostServices getApplicationHostServices() {
		return hostServices;
	}

	public Image getDefaultCoverImage() {
		return COVER_IMAGE;
	}

	public void editTracks() {
		ObservableList<Entry<Integer, Track>> trackSelection = rootController.getSelectedItems();
		if(trackSelection != null & !trackSelection.isEmpty()) {
			if(trackSelection.size() > 1) {
				Alert alert = createAlert("", "Are you sure you want to edit multiple files?", "", AlertType.CONFIRMATION);
				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK) {
					openStage(EDIT_LAYOUT);
					LOG.debug("Opened edit stage for various tracks");
				}
				else
					alert.close();
			}
			else {
				openStage(EDIT_LAYOUT);
				LOG.debug("Opened edit stage for a single track");
			}
		}
	}
	
	public void deleteTracks() {
		ObservableList<Map.Entry<Integer, Track>> trackSelection = rootController.getSelectedItems();
		if(trackSelection != null && !trackSelection.isEmpty()) {
			int numDeletedTracks = trackSelection.size();
			Alert alert = createAlert("", "Delete "+numDeletedTracks+" files from Musicott?", "", AlertType.CONFIRMATION);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				new Thread(() -> {
					musicLibrary.removeTracks(trackSelection.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
					Platform.runLater(() -> closeIndeterminatedProgressScene());
				}).start();
				openIndeterminatedProgressScene();
			}
			else
				alert.close();
		}
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
	
	public Alert createAlert(String title, String header, String content, AlertType type) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.initModality(Modality.APPLICATION_MODAL);
		alert.initOwner(mainStage);
		return alert;
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
		if(stageToOpen != null) 
			stageToOpen.showAndWait();
	}
	
	private Stage initStage(String layout, String title) {
		Stage newStage;
		try {
			newStage = new Stage();
			newStage.setTitle(title);
			FXMLLoader loader = new FXMLLoader ();
			loader.setLocation(getClass().getResource(layout));
			AnchorPane baseLayout = loader.load();
			
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
			errorDemon.showErrorDialog("Error opening " + layout, null, e);
			newStage = null;
		}
		return newStage;
	}
}