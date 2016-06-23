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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.musicott.view;

import com.musicott.*;
import com.musicott.model.*;
import com.musicott.util.*;
import com.musicott.view.custom.*;
import javafx.beans.binding.*;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import org.slf4j.*;

import java.util.*;
import java.util.Map.*;

/**
 * @author Octavio Calleya
 * @version 0.9
 */
public class RootController implements MusicottController {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	@FXML
	private BorderPane rootBorderPane, tableBorderPane, contentBorderLayout;
	@FXML
	private ImageView playlistCover;
	@FXML
	private Label playlistTracksNumberLabel, playlistSizeLabel, playlistTitleLabel;
	@FXML
	private HBox tableInfoHBox;
	@FXML
	private VBox playlistInfoVBox, headerVBox;
	private VBox navigationPaneVBox;
	private TrackTableView trackTable;
	private TextField playlistTitleTextField;
	private FilteredList<Entry<Integer, Track>> filteredTracks;

	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	
	public RootController() {}
	
	@FXML
	public void initialize() {		
		navigationPaneVBox = (VBox) contentBorderLayout.getLeft();
		
		playlistTitleTextField = new TextField();
		playlistTitleTextField.setPrefWidth(150);
		playlistTitleTextField.setPrefHeight(25);
		playlistTitleTextField.setPadding(new Insets (-10, 0, -10, 0));
		playlistTitleTextField.setFont(new Font ("System", 20));
		VBox.setMargin(playlistTitleTextField, new Insets(30, 0, 5, 15));
		
		playlistTracksNumberLabel.textProperty().bind(Bindings.createStringBinding(() -> musicLibrary.showingTracksProperty().sizeProperty().get()+" songs", musicLibrary.showingTracksProperty()));
		playlistSizeLabel.textProperty().bind(Bindings.createStringBinding(() -> {
			String sizeString = Utils.byteSizeString(musicLibrary.showingTracksProperty().stream().mapToLong(t -> (long)t.getValue().getSize()).sum(), 2);
			if(sizeString.equals("0 B"))
				sizeString = "";
			return sizeString; 
		},  musicLibrary.showingTracksProperty()));
		playlistTitleLabel.textProperty().bind(playlistTitleTextField.textProperty());
		playlistTitleLabel.setOnMouseClicked(event -> {
			if(event.getClickCount() == 2) {	// 2 clicks = edit playlist name
				putPlaylistTextField();
				playlistTitleTextField.setOnKeyPressed(e -> {
					Playlist playlist = stageDemon.getNavigationController().selectedPlaylistProperty().getValue().getValue();
					String newTitle = playlistTitleTextField.getText();
					if(e.getCode() == KeyCode.ENTER && newTitle.equals(playlist.getName())) {
						removePlaylistTextField();
						event.consume();
					} else if(e.getCode() == KeyCode.ENTER && !newTitle.equals("") && !musicLibrary.containsPlaylist(newTitle)) {
						playlist.setName(newTitle);
						musicLibrary.saveLibrary(false, false, true);
						removePlaylistTextField();
						event.consume();
					}
				});
			}
		});
		
		trackTable = new TrackTableView();
		tableBorderPane.setCenter(trackTable);	
		
		// Binds the text typed on the search text field to the items shown on the table
		ObservableList<Entry<Integer, Track>> tracks = musicLibrary.showingTracksProperty().get();
		filteredTracks = new FilteredList<>(tracks, predicate -> true);
		stageDemon.getPlayerController().searchTextProperty().addListener((observable, oldText, newText) -> {
			filteredTracks.setPredicate(trackEntry -> {
				boolean result = true;
				Track track = trackEntry.getValue();
				if(newText != null && !newText.isEmpty()) {
					if(track.getName().toLowerCase().contains(newText.toLowerCase()) ||
					   track.getArtist().toLowerCase().contains(newText.toLowerCase()) ||
					   track.getLabel().toLowerCase().contains(newText.toLowerCase()) ||
					   track.getGenre().toLowerCase().contains(newText.toLowerCase()) ||
					   track.getAlbum().toLowerCase().contains(newText.toLowerCase()))
						result = true;
					else	
						result = false;
				}
				return result;
			});
		});
		SortedList<Map.Entry<Integer, Track>> sortedTracks = new SortedList<>(filteredTracks);
		sortedTracks.comparatorProperty().bind(trackTable.comparatorProperty());
		trackTable.setItems(sortedTracks);
	}
	
	public ObservableList<Map.Entry<Integer, Track>> getSelectedItems() {
		return trackTable.getSelectionModel().getSelectedItems();
	}

	/**
	 * Updates the view with the selected playlist info in the table info pane
	 *
	 * @param playlist
	 */
	public void updatePlaylistInfo(Playlist playlist) {
		playlistTitleTextField.setText(playlist.getName());
		playlistCover.imageProperty().bind(playlist.playlistCoverProperty());
		removePlaylistTextField();
	}

	/**
	 * Handles the creation of a new playlist
	 *
	 * @param isFolder
	 */
	public void setNewPlaylistMode(boolean isFolder) {
		LOG.debug("Editing playlist name");
		musicLibrary.clearShowingTracks();
		putPlaylistTextField();
		Playlist newPlaylist = new Playlist("", isFolder);
		playlistTitleTextField.setText("");
		playlistCover.imageProperty().bind(newPlaylist.playlistCoverProperty());

		playlistTitleTextField.setOnKeyPressed(event -> {
			String newTitle = playlistTitleTextField.getText();

			if(event.getCode() == KeyCode.ENTER && !newTitle.isEmpty() && !musicLibrary.containsPlaylist(newTitle)) {
				newPlaylist.setName(newTitle);

				stageDemon.getNavigationController().addNewPlaylist(newPlaylist);

				removePlaylistTextField();
				event.consume();
			}
		});
	}
	
	/**
	 * Shows the upper table info pane
	 */
	public void showTableInfoPane() {
		if(!tableBorderPane.getChildren().contains(tableInfoHBox)) {
			tableBorderPane.setTop(tableInfoHBox);
			LOG.debug("Showing info pane");
		}
	}

	/**
	 * Hides the upper table info pane
	 */
	public void hideTableInfoPane() {
		if(tableBorderPane.getChildren().contains(tableInfoHBox)) {
			tableBorderPane.getChildren().remove(tableInfoHBox);
			LOG.debug("Hiding info pane");
		}
	}
	
	/**
	 * Shows the left navigation pane
	 */
	public void showNavigationPane() {
		if(!contentBorderLayout.getChildren().contains(navigationPaneVBox)) {
			contentBorderLayout.setLeft(navigationPaneVBox);
			LOG.debug("Showing navigation pane");
		}
	}

	/**
	 * Hides the left navigation pane
	 */
	public void hideNavigationPane() {
		if(contentBorderLayout.getLeft().equals(navigationPaneVBox)) {
			contentBorderLayout.getChildren().remove(navigationPaneVBox);
			LOG.debug("Showing navigation pane");
		}
	}
	
	/**
	 * Puts a text field to edit the name of the playlist
	 */
	private void putPlaylistTextField() {
		showTableInfoPane();
		if(!playlistInfoVBox.getChildren().contains(playlistTitleTextField)) {
			playlistInfoVBox.getChildren().remove(playlistTitleLabel);
			playlistInfoVBox.getChildren().add(0, playlistTitleTextField);
			playlistTitleTextField.requestFocus();
		}
	}

	/**
	 * Removes the text field and shows the label with the title of the selected or entered playlist
	 */
	private void removePlaylistTextField() {
		showTableInfoPane();
		if(!playlistInfoVBox.getChildren().contains(playlistTitleLabel)) {
			playlistInfoVBox.getChildren().remove(playlistTitleTextField);
			playlistInfoVBox.getChildren().add(0, playlistTitleLabel);
		}
	}
}
