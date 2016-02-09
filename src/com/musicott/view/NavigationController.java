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

import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Playlist;
import com.musicott.view.custom.NavigationMenuListView;
import com.musicott.view.custom.PlaylistTreeView;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * @author Octavio Calleya
 *
 */
public class NavigationController {

	public static final String ALL_SONGS_MODE = "All songs";
	
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
	
	private SceneManager sc = SceneManager.getInstance();
	private MusicLibrary ml = MusicLibrary.getInstance();
	
	private NavigationMenuListView navigationMenuListView;
	private PlaylistTreeView playlistTreeView;
	private ListProperty<String> selectedMenuProperty;
	private ObservableList<String> selectedMenu;

	public NavigationController() {}
	
	@FXML
	public void initialize() {
		playlistTreeView = new PlaylistTreeView();
		playlistTreeView.getSelectionModel().selectedItemProperty().addListener(listener -> {
			navigationMenuListView.getSelectionModel().clearAndSelect(-1);
			sc.getRootController().showTableInfoPane(true);
			if(playlistTreeView.getSelectedPlaylist() != null)
				sc.getRootController().updatePlaylistInfo(playlistTreeView.getSelectedPlaylist());				
		});
		
		navigationMenuListView = new NavigationMenuListView();
		navigationMenuListView.setItems(FXCollections.observableArrayList(ALL_SONGS_MODE));
		
		selectedMenu = navigationMenuListView.getSelectionModel().getSelectedItems();
		selectedMenuProperty = new SimpleListProperty<>();
		selectedMenuProperty.bind(new SimpleObjectProperty<>(selectedMenu));
		
		newPlaylistButton.setOnMouseClicked(e -> {
			sc.getRootController().setNewPlaylistMode();
			playlistTreeView.getSelectionModel().clearAndSelect(-1);
		});

		navigationVBox.getChildren().add(1, navigationMenuListView);
		playlistsVBox.getChildren().add(1, playlistTreeView);
		taskProgressBar.visibleProperty().bind(Bindings.createBooleanBinding(() -> taskProgressBar.progressProperty().isEqualTo(0).not().get(), taskProgressBar.progressProperty()));
		taskProgressBar.setProgress(0);
		
		VBox.setVgrow(playlistTreeView, Priority.ALWAYS);
		VBox.setVgrow(navigationVBox, Priority.ALWAYS);
	}	

	public void setStatusProgress(double progress) {
		taskProgressBar.setProgress(progress);
	}	
	
	public void setStatusMessage(String message) {
		statusLabel.setText(message);
	}
	
	public ListProperty<String> selectedMenuProperty() {
		return selectedMenuProperty;
	}
	
	public Playlist getSelectedPlaylist() {
		return playlistTreeView.getSelectedPlaylist();
	}
	
	public void deleteSelectedPlaylist() {
		playlistTreeView.deletePlaylist();
	}
	
	public void addPlaylist(Playlist playlist) {
		playlistTreeView.addPlaylist(playlist);
	}
	
	public void showMode(String mode) {
		if(mode.equals(ALL_SONGS_MODE))
			ml.showMode(ALL_SONGS_MODE);
		Platform.runLater(() -> sc.getRootController().showTableInfoPane(false));
		playlistTreeView.getSelectionModel().clearAndSelect(-1);
		navigationMenuListView.getSelectionModel().select(mode);
	}
}