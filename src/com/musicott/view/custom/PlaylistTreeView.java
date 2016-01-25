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

package com.musicott.view.custom;

import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Playlist;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;

/**
 * @author Octavio Calleya
 *
 */
public class PlaylistTreeView extends TreeView<Playlist> {
	
	private TreeItem<Playlist> root;
	private MusicLibrary ml;
	private Playlist selectedPlaylist;
	
	public PlaylistTreeView() {
		super();
		ml = MusicLibrary.getInstance();
		root = new TreeItem<Playlist>();
		setRoot(root);
		setShowRoot(false);
		setEditable(true);
		AnchorPane.setTopAnchor(this, 0.0);
		AnchorPane.setRightAnchor(this, 0.0);
		AnchorPane.setBottomAnchor(this, 0.0);
		AnchorPane.setLeftAnchor(this, 0.0);
		setPrefHeight(USE_COMPUTED_SIZE);
		setPrefWidth(USE_COMPUTED_SIZE);
		setId("playlistTreeView");
		setCellFactory(p -> new PlaylistTreeCell());
		getSelectionModel().selectedItemProperty().addListener(listener -> {
			selectedPlaylist = getSelectionModel().getSelectedItem().getValue();
			if(selectedPlaylist.getName().equals("Recently Added"))
				ml.setShowingPlaylist(null);
			else
				ml.setShowingPlaylist(selectedPlaylist);
			SceneManager.getInstance().getRootController().updatePlaylistInfo(selectedPlaylist);
		});
		for(Playlist pl: ml.getPlaylists())
			root.getChildren().add(new TreeItem<Playlist>(pl));
	}
	
	public Playlist getSelectedPlaylist() {
		return this.selectedPlaylist;
	}
	
	private class PlaylistTreeCell extends TreeCell<Playlist> {
		
		@Override
		public void updateItem(Playlist p, boolean empty) {
			super.updateItem(p, empty);
			if(!empty) {
				setText(p.getName());
			} else {
				setText(null);
			}
		}
	}
}