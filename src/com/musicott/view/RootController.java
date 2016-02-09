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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Playlist;
import com.musicott.model.Track;
import com.musicott.util.Utils;
import com.musicott.view.custom.TrackTableView;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * @author Octavio Calleya
 *
 */
public class RootController {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	public static final String ALL_SONGS_MODE = "All songs";
	public final Image DEFAULT_COVER_IMAGE = new Image(getClass().getResourceAsStream("/images/default-cover-image.png"));
	
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

	private SceneManager sc = SceneManager.getInstance();
	private MusicLibrary ml = MusicLibrary.getInstance();
	
	public RootController() {}
	
	@FXML
	public void initialize() {		
		navigationPaneVBox = (VBox) contentBorderLayout.getLeft();
		
		playlistTitleTextField = new TextField();
		playlistTitleTextField.setPrefWidth(150);
		playlistTitleTextField.setPrefHeight(25);
		playlistTitleTextField.setPadding(new Insets(-10, 0, -10, 0));
		playlistTitleTextField.setFont(new Font("System", 20));		
		VBox.setMargin(playlistTitleTextField, new Insets(30, 0, 5, 15));
		
		playlistTracksNumberLabel.textProperty().bind(Bindings.createStringBinding(() -> ml.getShowingTracksProperty().sizeProperty().get()+" songs", ml.getShowingTracksProperty()));
		playlistSizeLabel.textProperty().bind(Bindings.createStringBinding(() -> {
			String sizeString = Utils.byteSizeString(ml.getShowingTracksProperty().stream().mapToLong(t -> (long)t.getValue().getSize()).sum(), 2);
			if(sizeString.equals("0 B"))
				sizeString = "";
			return sizeString; 
		},  ml.getShowingTracksProperty()));
		playlistTitleLabel.textProperty().bind(playlistTitleTextField.textProperty());
		playlistTitleLabel.setOnMouseClicked(event -> {
			if(event.getClickCount() == 2) {	// 2 clicks = edit playlist name
				putPlaylistTextField(true);
				playlistTitleTextField.setOnKeyPressed(e -> {
					Playlist playlist = sc.getNavigationController().getSelectedPlaylist();
					String newTitle = playlistTitleTextField.getText();
					if(e.getCode() == KeyCode.ENTER && newTitle.equals(playlist.getName())) {
						putPlaylistTextField(false);
						event.consume();
					} else if(e.getCode() == KeyCode.ENTER && !newTitle.equals("") && !ml.containsPlaylist(newTitle)) {
						playlist.setName(newTitle);
						ml.saveLibrary(false, false, true);
						putPlaylistTextField(false);
						event.consume();
					}
				});
			}
		});
		
		trackTable = new TrackTableView();
		tableBorderPane.setCenter(trackTable);		
	}
	
	public ObservableList<Map.Entry<Integer, Track>> getSelectedItems() {
		return trackTable.getSelectionModel().getSelectedItems();
	}
	
	public void setStatusMessage(String message) {

	}
	
	public void setStatusProgress(double progress) {

	}
	
	/**
	 * Show/Hide the upper table info pane
	 * @param show
	 */
	public void showTableInfoPane(boolean show) {
		if(show && !tableBorderPane.getChildren().contains(tableInfoHBox)) {
			tableBorderPane.setTop(tableInfoHBox);
			LOG.debug("Showing info pane");
		} else if(!show && tableBorderPane.getChildren().contains(tableInfoHBox)) {
			tableBorderPane.getChildren().remove(tableInfoHBox);
			LOG.debug("Hiding info pane");
		}
	}
	
	/**
	 * Show/Hide the left navigation pane
	 * @param show
	 */
	public void showNavigationPane(boolean show) {
		if(show && !contentBorderLayout.getChildren().contains(navigationPaneVBox)) {
			contentBorderLayout.setLeft(navigationPaneVBox);
			LOG.debug("Showing navigation pane");
		} else if(!show && contentBorderLayout.getChildren().contains(navigationPaneVBox)) {
			contentBorderLayout.getChildren().remove(navigationPaneVBox);
			LOG.debug("Showing navigation pane");
		}
	}
	
	/**
	 * Updates the view with the selected playlist info
	 * @param playlist
	 */
	public void updatePlaylistInfo(Playlist playlist) {
		playlistTitleTextField.setText(playlist.getName());
		playlistCover.imageProperty().bind(playlist.playlistCoverProperty());
		putPlaylistTextField(false);
	}

	/**
	 * Handles the creation of a new playlist
	 */
	public void setNewPlaylistMode() {
		LOG.debug("Editing playlist name");
		ml.clearShowingTracks();
		showTableInfoPane(true);
		putPlaylistTextField(true);
		Playlist newPlaylist = new Playlist("");
		playlistTitleTextField.setText("");
		playlistCover.imageProperty().bind(newPlaylist.playlistCoverProperty());
		playlistTitleTextField.setOnKeyPressed(event -> {
			String newTitle = playlistTitleTextField.getText();
			if(event.getCode() == KeyCode.ENTER && !newTitle.equals("") && !ml.containsPlaylist(newTitle)) {
				newPlaylist.setName(newTitle);
				ml.addPlaylist(newPlaylist);
				sc.getNavigationController().addPlaylist(newPlaylist);
				putPlaylistTextField(false);
				event.consume();
			}
		});
	}
	
	/**
	 * Puts a text field to edit the name of the playlist if <tt>editPlaylistTitle</tt> is true,
	 * otherwise shows the label with the title of the selected or entered playlist
	 * 
	 * @param editPlaylistTitle
	 */
	private void putPlaylistTextField(boolean editPlaylistTitle) {
		showTableInfoPane(true);
		if(editPlaylistTitle && !playlistInfoVBox.getChildren().contains(playlistTitleTextField)) {
			playlistInfoVBox.getChildren().remove(playlistTitleLabel);
			playlistInfoVBox.getChildren().add(0, playlistTitleTextField);
			playlistTitleTextField.requestFocus();
		} else if(!editPlaylistTitle && !playlistInfoVBox.getChildren().contains(playlistTitleLabel)) {
			playlistInfoVBox.getChildren().remove(playlistTitleTextField);
			playlistInfoVBox.getChildren().add(0, playlistTitleLabel);			
		}
	}
}