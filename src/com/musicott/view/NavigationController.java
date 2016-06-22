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

package com.musicott.view;

import com.musicott.*;
import com.musicott.model.*;
import com.musicott.view.custom.*;
import javafx.application.*;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.input.KeyCombination.*;
import javafx.scene.layout.*;

/**
 * @author Octavio Calleya
 */
public class NavigationController {

	@FXML
	private AnchorPane rootAnchorPane;
	@FXML
	private VBox navigationPaneVBox, navigationVBox, playlistsVBox;
	@FXML
	private Button newPlaylistButton;
	@FXML
	private ProgressBar taskProgressBar;
	@FXML
	private Label statusLabel;
	private ContextMenu newPlaylistContextMenu;
	
	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	
	private NavigationMenuListView navigationMenuListView;
	private PlaylistTreeView playlistTreeView;

	public NavigationController() {}
	
	@FXML
	public void initialize() {
		playlistTreeView = new PlaylistTreeView();
		playlistTreeView.getSelectionModel().selectedItemProperty().addListener(listener -> {
			navigationMenuListView.getSelectionModel().clearAndSelect(-1);
			stageDemon.getRootController().showTableInfoPane();
			if(playlistTreeView.getSelectedPlaylist() != null)
				stageDemon.getRootController().updatePlaylistInfo(playlistTreeView.getSelectedPlaylist());				
		});
		
		navigationMenuListView = new NavigationMenuListView();
		navigationMenuListView.setItems(FXCollections.observableArrayList(NavigationMode.values()));
		
		newPlaylistContextMenu = new ContextMenu();
		
		String os = System.getProperty ("os.name");
		// Key accelerator. Command down for os x and control down for windows and linux
		Modifier keyModifierOS = os != null && os.startsWith ("Mac") ? KeyCodeCombination.META_DOWN : KeyCodeCombination.CONTROL_DOWN;
		MenuItem newPlaylistMI, newFolderPlaylistMI;
		newPlaylistMI = new MenuItem("New Playlist");
		newPlaylistMI.setAccelerator(new KeyCodeCombination(KeyCode.N, keyModifierOS));
		newPlaylistMI.setOnAction(e -> {
			stageDemon.getRootController().setNewPlaylistMode(false);
			playlistTreeView.getSelectionModel().clearAndSelect(-1);
		});
		newFolderPlaylistMI = new MenuItem("New Playlist Folder");
		newFolderPlaylistMI.setOnAction(e -> {
			stageDemon.getRootController().setNewPlaylistMode(true);
			playlistTreeView.getSelectionModel().clearAndSelect(-1);
		});
		newPlaylistContextMenu.getItems().addAll(newPlaylistMI, newFolderPlaylistMI);
		newPlaylistButton.setContextMenu(newPlaylistContextMenu);
		
		newPlaylistButton.addEventFilter(MouseEvent.MOUSE_CLICKED, e ->
			newPlaylistContextMenu.show(newPlaylistButton, newPlaylistButton.getLayoutX()+150, newPlaylistButton.getLayoutY())
		);
		newPlaylistButton.setContextMenu(newPlaylistContextMenu);
		
		navigationVBox.getChildren().add(1, navigationMenuListView);
		playlistsVBox.getChildren().add(1, playlistTreeView);
		taskProgressBar.visibleProperty().bind(Bindings.createBooleanBinding(() -> taskProgressBar.progressProperty().isEqualTo(0).not().get(), taskProgressBar.progressProperty()));
		taskProgressBar.setProgress(0);
		
		VBox.setVgrow(playlistTreeView, Priority.ALWAYS);
		VBox.setVgrow(navigationVBox, Priority.ALWAYS);
	}	
	
	public ReadOnlyObjectProperty<NavigationMode> selectedMenuProperty() {
		return navigationMenuListView.getSelectionModel().selectedItemProperty();
	}
	
	public ReadOnlyObjectProperty<TreeItem<Playlist>> selectedPlaylistProperty() {
		return playlistTreeView.getSelectionModel().selectedItemProperty();
	}

	public void addNewPlaylist(Playlist newPlaylist) {
		TreeItem<Playlist> selectedPlaylistItem = playlistTreeView.getSelectionModel().selectedItemProperty().get();
		if(selectedPlaylistItem == null)
			playlistTreeView.addPlaylist(newPlaylist);
		else {
			Playlist selectedPlaylist = selectedPlaylistItem.getValue();
			if(selectedPlaylist.isFolder())
				playlistTreeView.addPlaylistChild(selectedPlaylist, newPlaylist);
			else {
				playlistTreeView.addPlaylist(newPlaylist);
				musicLibrary.addPlaylist(newPlaylist);
			}
		}
	}
	
	public void deleteSelectedPlaylist() {
		playlistTreeView.deletePlaylist();
	}	

	public void setStatusProgress(double progress) {
		taskProgressBar.setProgress(progress);
	}	
	
	public void setStatusMessage(String message) {
		statusLabel.setText(message);
	}
	
	public void showMode(NavigationMode mode) {
		if(mode.equals(NavigationMode.ALL_SONGS_MODE))
			musicLibrary.showMode(NavigationMode.ALL_SONGS_MODE);
		Platform.runLater(() -> stageDemon.getRootController().hideTableInfoPane());
		playlistTreeView.getSelectionModel().clearAndSelect(-1);
		navigationMenuListView.getSelectionModel().select(mode);
	}

	public NavigationMode getShowingMode() {

		return null;
	}
}