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
import javafx.beans.property.*;
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
import java.util.function.*;

/**
 * Controller class of the root layout of the whole application.
 *
 * @author Octavio Calleya
 * @version 0.9
 */
public class RootController implements MusicottController {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	@FXML
	private BorderPane tableBorderPane;
	@FXML
	private BorderPane contentBorderLayout;
	@FXML
	private ImageView playlistCover;
	@FXML
	private Label playlistTracksNumberLabel;
	@FXML
	private Label playlistSizeLabel;
	@FXML
	private Label playlistTitleLabel;
	@FXML
	private HBox tableInfoHBox;
	@FXML
	private VBox playlistInfoVBox;
	@FXML
	private VBox navigationPaneVBox;
	private TrackTableView trackTable;
	private TextField playlistTitleTextField;
	private FilteredList<Entry<Integer, Track>> filteredTracks;
	private ListProperty<Map.Entry<Integer, Track>> showingTracksProperty;
	private ReadOnlyObjectProperty<TreeItem<Playlist>> selectedPlaylistProperty;

	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();

	@FXML
	public void initialize() {
		showingTracksProperty = musicLibrary.showingTracksProperty();
		selectedPlaylistProperty = stageDemon.getNavigationController().selectedPlaylistProperty();
		selectedPlaylistProperty.addListener((obs, oldSelected, newSelected) -> {
					if(newSelected != null)
						updateShowingInfoWithPlaylist(newSelected.getValue());
		});

		initializeInfoPaneFields();
		
		trackTable = new TrackTableView();
		tableBorderPane.setCenter(trackTable);	
		
		// Binding of the text typed on the search text field to the items shown on the table
		ObservableList<Entry<Integer, Track>> tracks = showingTracksProperty.get();
		filteredTracks = new FilteredList<>(tracks, predicate -> true);

		StringProperty searchTextProperty = stageDemon.getPlayerController().searchTextProperty();
		searchTextProperty.addListener((observable, oldText, newText) ->
				filteredTracks.setPredicate(findTracksContainingTextPredicate(newText)));

		SortedList<Map.Entry<Integer, Track>> sortedTracks = new SortedList<>(filteredTracks);
		sortedTracks.comparatorProperty().bind(trackTable.comparatorProperty());
		trackTable.setItems(sortedTracks);
	}

	public void setNavigationPaneVBox(VBox navigationPaneVBox) {
		this.navigationPaneVBox = navigationPaneVBox;
	}

	private void initializeInfoPaneFields() {
		initializePlaylistTitleTextField();

		playlistTracksNumberLabel.textProperty().bind(Bindings.createStringBinding(
				() -> showingTracksProperty.sizeProperty().get() + " songs", showingTracksProperty));

		playlistSizeLabel.textProperty().bind(Bindings.createStringBinding(() -> {
			long sizeOfAllShowingTracks = showingTracksProperty.stream()
					.mapToLong(trackEntry -> (long) trackEntry.getValue().getSize()).sum();

			String sizeString = Utils.byteSizeString(sizeOfAllShowingTracks, 2);
			if("0 B".equals(sizeString))
				sizeString = "";
			return sizeString;
		},  showingTracksProperty));

		playlistTitleLabel.textProperty().bind(playlistTitleTextField.textProperty());

		playlistTitleLabel.setOnMouseClicked(event -> {
			if(event.getClickCount() == 2)	// double click for edit the playlist name
				putPlaylistTextField();
		});
	}

	private void initializePlaylistTitleTextField() {
		playlistTitleTextField = new TextField();
		playlistTitleTextField.setPrefWidth(150);
		playlistTitleTextField.setPrefHeight(25);
		playlistTitleTextField.setPadding(new Insets(-10, 0, -10, 0));
		playlistTitleTextField.setFont(new Font("System", 20));
		VBox.setMargin(playlistTitleTextField, new Insets(30, 0, 5, 15));
		playlistTitleTextField.setOnKeyPressed(event -> {
			KeyCode keyPressed = event.getCode();
			Playlist playlist = selectedPlaylistProperty.getValue().getValue();
			String newTitle = playlistTitleTextField.getText();

			if(keyPressed == KeyCode.ENTER && newTitle.equals(playlist.getName())) {
				removePlaylistTextField();
				event.consume();
			}
			else if(keyPressed == KeyCode.ENTER && !newTitle.isEmpty() && !musicLibrary.containsPlaylist(newTitle)) {
				playlist.setName(newTitle);
				musicLibrary.saveLibrary(false, false, true);
				removePlaylistTextField();
				event.consume();
			}
		});
	}

	/**
	 * Returns a {@link Predicate} that evaluates the match of a given <tt>String</tt> to a given track {@link Entry}
	 *
	 * @param string The <tt>String</tt> to match against the track
	 * @return The <tt>Predicate</tt>
	 */
	private Predicate<Entry<Integer, Track>> findTracksContainingTextPredicate(String string) {
		return trackEntry -> {
			boolean result = string == null || string.isEmpty();
			if(!result)
				result = trackMatchesString(trackEntry.getValue(), string);
			return result;
		};
	}

	/**
	 * Determines if a track matches a given string by its name, artist, label, genre or album.
	 *
	 * @param track The {@link Track} to match
	 * @param string The string to match against the <tt>Track</tt>
	 * @return <tt>true</tt> if the <tt>Track matches</tt>, <tt>false</tt> otherwise
	 */
	private boolean trackMatchesString(Track track, String string) {
		boolean matchesName = track.getName().toLowerCase().contains(string.toLowerCase());
		boolean matchesArtist = track.getArtist().toLowerCase().contains(string.toLowerCase());
		boolean matchesLabel = track.getLabel().toLowerCase().contains(string.toLowerCase());
		boolean matchesGenre = track.getGenre().toLowerCase().contains(string.toLowerCase());
		boolean matchesAlbum = track.getAlbum().toLowerCase().contains(string.toLowerCase());
		return matchesName || matchesArtist || matchesLabel || matchesGenre || matchesAlbum;
	}
	
	public ObservableList<Map.Entry<Integer, Track>> getSelectedItems() {
		return trackTable.getSelectionModel().getSelectedItems();
	}

	/**
	 * Updates the information pane with the selected {@link Playlist}
	 *
	 * @param playlist The selected <tt>Playlist</tt>
	 */
	public void updateShowingInfoWithPlaylist(Playlist playlist) {
		playlistTitleTextField.setText(playlist.getName());
		playlistCover.imageProperty().bind(playlist.playlistCoverProperty());
		removePlaylistTextField();
	}

	/**
	 * Handles the naming of a new playlist placing a {@link TextField} on top
	 * of the playlist label asking the user for the name.
	 *
	 * @param isFolder <tt>true</tt> if the new {@link Playlist} is a folder, <tt>false</tt> otherwise
	 */
	public void enterNewPlaylistName(boolean isFolder) {
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
		if(!contentBorderLayout.getChildren().equals(navigationPaneVBox)) {
			contentBorderLayout.setLeft(navigationPaneVBox);
			LOG.debug("Showing navigation pane");
		}
	}

	/**
	 * Hides the left navigation pane
	 */
	public void hideNavigationPane() {
		if(contentBorderLayout.getChildren().contains(navigationPaneVBox)) {
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
