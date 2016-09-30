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

package com.transgressoft.musicott.view;

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.event.*;
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
 * @version 0.9.1-b
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
	private GridPane playlistInfoGridPane;
	@FXML
	private VBox navigationPaneVBox;
	private TrackTableView trackTable;
	private TextField playlistTitleTextField;
	private List<Track> selectedTracks;
	private ListProperty<Map.Entry<Integer, Track>> showingTracksProperty;
	private ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty;

	private BooleanProperty showingNavigationPaneProperty;
	private BooleanProperty showingTableInfoPaneProperty;

	private EventHandler<KeyEvent> changePlaylistNameTextFieldHandler = changePlaylistNameTextFieldHandler();

	@FXML
	public void initialize() {
		selectedTracks = new ArrayList<>();
		showingTracksProperty = musicLibrary.showingTracksProperty();
		selectedPlaylistProperty = stageDemon.getNavigationController().selectedPlaylistProperty();
		selectedPlaylistProperty.addListener(
				(obs, oldSelected, newSelected) -> newSelected.ifPresent(this::updateShowingInfoWithPlaylist));

		showingNavigationPaneProperty = new SimpleBooleanProperty(this, "showing navigation pane", true);
		showingTableInfoPaneProperty = new SimpleBooleanProperty(this, "showing table info pane", true);
		initializeInfoPaneFields();

		trackTable = new TrackTableView();
		tableBorderPane.setCenter(trackTable);

		// Binding of the text typed on the search text field to the items shown on the table
		ObservableList<Entry<Integer, Track>> tracks = showingTracksProperty.get();
		FilteredList<Entry<Integer, Track>> filteredTracks = new FilteredList<>(tracks, predicate -> true);

		StringProperty searchTextProperty = stageDemon.getPlayerController().searchTextProperty();
		searchTextProperty.addListener((observable, oldText, newText) -> filteredTracks
				.setPredicate(findTracksContainingTextPredicate(newText)));

		SortedList<Map.Entry<Integer, Track>> sortedTracks = new SortedList<>(filteredTracks);
		sortedTracks.comparatorProperty().bind(trackTable.comparatorProperty());
		trackTable.setItems(sortedTracks);
	}

	/**
	 * Updates the information pane with the selected {@link Playlist}
	 *
	 * @param playlist The selected <tt>Playlist</tt>
	 */
	private void updateShowingInfoWithPlaylist(Playlist playlist) {
		playlistTitleTextField.setText(playlist.getName());
		playlistCover.imageProperty().bind(playlist.playlistCoverProperty());
		removePlaylistTextField();
	}

	private void initializeInfoPaneFields() {
		initializePlaylistTitleTextField();

		playlistTracksNumberLabel.textProperty().bind(Bindings.createStringBinding(
				() -> showingTracksProperty.sizeProperty().get() + " songs", showingTracksProperty));

		playlistSizeLabel.textProperty().bind(Bindings.createStringBinding(() -> {
			long sizeOfAllShowingTracks = showingTracksProperty.stream().mapToLong(
					trackEntry -> (long) trackEntry.getValue().getSize()).sum();

			String sizeString = Utils.byteSizeString(sizeOfAllShowingTracks, 2);
			if ("0 B".equals(sizeString))
				sizeString = "";
			return sizeString;
		}, showingTracksProperty));

		playlistTitleLabel.textProperty().bind(playlistTitleTextField.textProperty());

		playlistTitleLabel.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2)    // double click to edit the playlist name
				placePlaylistTextField();
		});
	}

	private void initializePlaylistTitleTextField() {
		playlistTitleTextField = new TextField();
		playlistTitleTextField.setMaxWidth(150);
		playlistTitleTextField.setPrefHeight(25);
		playlistTitleTextField.setPadding(new Insets(- 10, 0, - 10, 0));
		playlistTitleTextField.setFont(new Font("Avenir", 19));
		VBox.setMargin(playlistTitleTextField, new Insets(30, 0, 5, 15));
		playlistTitleTextField.setOnKeyPressed(changePlaylistNameTextFieldHandler);
	}

	private EventHandler<KeyEvent> changePlaylistNameTextFieldHandler() {
		return event -> {
			if (event.getCode() == KeyCode.ENTER) {
				Playlist playlist = selectedPlaylistProperty.getValue().get();
				String newName = playlistTitleTextField.getText();
				if (isValidPlaylistName(newName) || playlist.getName().equals(newName)) {
					playlist.setName(newName);
					removePlaylistTextField();
					musicLibrary.saveLibrary(false, false, true);
				}
				event.consume();
			}
		};
	}

	/**
	 * Puts a text field to edit the name of the playlist
	 */
	private void placePlaylistTextField() {
		showTableInfoPane();
		if (! playlistInfoGridPane.getChildren().contains(playlistTitleTextField)) {
			playlistInfoGridPane.getChildren().remove(playlistTitleLabel);
			playlistInfoGridPane.add(playlistTitleTextField, 0, 0);
			playlistTitleTextField.requestFocus();
		}
	}

	/**
	 * Removes the text field and shows the label with the title of the selected or entered playlist
	 */
	private void removePlaylistTextField() {
		showTableInfoPane();
		if (! playlistInfoGridPane.getChildren().contains(playlistTitleLabel)) {
			playlistInfoGridPane.getChildren().remove(playlistTitleTextField);
			playlistInfoGridPane.add(playlistTitleLabel, 0, 0);
		}
	}

	/**
	 * Returns a {@link Predicate} that evaluates the match of a given <tt>String</tt> to a given track {@link Entry}
	 *
	 * @param string The <tt>String</tt> to match against the track
	 *
	 * @return The <tt>Predicate</tt>
	 */
	private Predicate<Entry<Integer, Track>> findTracksContainingTextPredicate(String string) {
		return trackEntry -> {
			boolean result = string == null || string.isEmpty();
			if (! result)
				result = trackMatchesString(trackEntry.getValue(), string);
			return result;
		};
	}

	/**
	 * Determines if a track matches a given string by its name, artist, label, genre or album.
	 *
	 * @param track  The {@link Track} to match
	 * @param string The string to match against the <tt>Track</tt>
	 *
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

	/**
	 * Handles the naming of a new playlist placing a {@link TextField} on top
	 * of the playlist label asking the user for the name.
	 *
	 * @param isFolder <tt>true</tt> if the new {@link Playlist} is a folder, <tt>false</tt> otherwise
	 */
	public void enterNewPlaylistName(boolean isFolder) {
		LOG.debug("Editing playlist name");
		musicLibrary.clearShowingTracks();
		placePlaylistTextField();

		Playlist newPlaylist = new Playlist("", isFolder);
		playlistCover.imageProperty().bind(newPlaylist.playlistCoverProperty());

		EventHandler<KeyEvent> newPlaylistNameTextFieldHandler = event -> {
			if (event.getCode() == KeyCode.ENTER) {
				String newPlaylistName = playlistTitleTextField.getText();

				if (isValidPlaylistName(newPlaylistName)) {
					newPlaylist.setName(newPlaylistName);
					removePlaylistTextField();
					stageDemon.getNavigationController().addNewPlaylist(newPlaylist);
					musicLibrary.saveLibrary(false, false, true);
					playlistTitleTextField.setOnKeyPressed(changePlaylistNameTextFieldHandler);
				}
				event.consume();
			}
		};
		playlistTitleTextField.clear();
		playlistTitleTextField.setOnKeyPressed(newPlaylistNameTextFieldHandler);
	}

	/**
	 * Ensures that a string for a playlist is valid, checking if
	 * it is empty, or another playlist has the same name.
	 *
	 * @param newName The name of the playlist to check
	 *
	 * @return <tt>true</tt> if its a valid name, <tt>false</tt> otherwise
	 */
	private boolean isValidPlaylistName(String newName) {
		Playlist blankPlaylist = new Playlist(newName, false);
		return ! newName.isEmpty() && ! musicLibrary.containsPlaylist(blankPlaylist);
	}

	/**
	 * Shows the upper table info pane
	 */
	public void showTableInfoPane() {
		if (! tableBorderPane.getChildren().contains(tableInfoHBox)) {
			tableBorderPane.setTop(tableInfoHBox);
			showingTableInfoPaneProperty.set(true);
			LOG.debug("Showing info pane");
		}
	}

	/**
	 * Hides the upper table info pane
	 */
	public void hideTableInfoPane() {
		if (tableBorderPane.getChildren().contains(tableInfoHBox)) {
			tableBorderPane.getChildren().remove(tableInfoHBox);
			showingTableInfoPaneProperty.set(false);
			LOG.debug("Hiding info pane");
		}
	}

	/**
	 * Shows the left navigation pane
	 */
	public void showNavigationPane() {
		if (! contentBorderLayout.getChildren().equals(navigationPaneVBox)) {
			contentBorderLayout.setLeft(navigationPaneVBox);
			showingNavigationPaneProperty.set(true);
			LOG.debug("Showing navigation pane");
		}
	}

	/**
	 * Hides the left navigation pane
	 */
	public void hideNavigationPane() {
		if (contentBorderLayout.getChildren().contains(navigationPaneVBox)) {
			contentBorderLayout.getChildren().remove(navigationPaneVBox);
			showingNavigationPaneProperty.set(false);
			LOG.debug("Showing navigation pane");
		}
	}

	public void setNavigationPane(VBox navigationPaneVBox) {
		this.navigationPaneVBox = navigationPaneVBox;
	}

	public List<Track> getSelectedTracks() {
		selectedTracks.clear();
		List<Entry<Integer, Track>> selectedItems = trackTable.getSelectionModel().getSelectedItems();
		if (selectedItems != null)
			selectedItems.forEach(entry -> {
				if (entry != null)
					selectedTracks.add(entry.getValue());
			});

		return selectedTracks;
	}

	public ReadOnlyBooleanProperty showNavigationPaneProperty() {
		return showingNavigationPaneProperty;
	}

	public ReadOnlyBooleanProperty showTableInfoPaneProperty() {
		return showingTableInfoPaneProperty;
	}
}
