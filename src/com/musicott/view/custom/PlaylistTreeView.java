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

import static com.musicott.view.custom.MusicottScene.ALL_SONGS_MODE;

import com.musicott.model.MusicLibrary;
import com.musicott.model.Playlist;

import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;

/**
 * @author Octavio Calleya
 *
 */
public class PlaylistTreeView extends TreeView<Playlist> {
	
	private MusicottScene musicottScene;
	private MusicLibrary ml = MusicLibrary.getInstance();
	
	private TreeItem<Playlist> root;
	private PlaylistTreeViewContextMenu contextMenu;
	private ObservableList<TreeItem<Playlist>> selectedPlaylist;
	
	public PlaylistTreeView(MusicottScene scene) {
		super();
		musicottScene = scene;
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
		getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		selectedPlaylist = getSelectionModel().getSelectedItems();
		getSelectionModel().selectedItemProperty().addListener(listener -> ml.showPlaylist(getSelectedPlaylist()));
		
		contextMenu = new PlaylistTreeViewContextMenu(musicottScene);
		setContextMenu(contextMenu);
		
		for(Playlist pl: ml.getPlaylists())
			root.getChildren().add(new TreeItem<Playlist>(pl));
	}
	
	public Playlist getSelectedPlaylist() {
		return selectedPlaylist.get(0) == null ? null : selectedPlaylist.get(0).getValue();
	}
	
	protected void addPlaylist(Playlist newPlaylist) {
		TreeItem<Playlist> newItem = new TreeItem<>(newPlaylist);
		root.getChildren().add(newItem);
		getSelectionModel().select(newItem);
	}
	
	protected void deletePlaylist() {
		Playlist selected = getSelectedPlaylist();
		if(selected != null) {
			ml.removePlaylist(getSelectedPlaylist());
			int playlistToDeleteIndex = getSelectionModel().getSelectedIndex();
			root.getChildren().removeIf(treeItem -> treeItem.getValue().equals(selected));
			if(playlistToDeleteIndex == 0 && root.getChildren().size() == 0)
				musicottScene.showMode(ALL_SONGS_MODE);
			else
				getSelectionModel().selectFirst();
		}
	}
	
	private class PlaylistTreeCell extends TreeCell<Playlist> {
		
		@Override
		public void updateItem(Playlist p, boolean empty) {
			super.updateItem(p, empty);
			if(!empty) {
				textProperty().bind(p.nameProperty());
			}
			else {
				textProperty().unbind();
				setText("");
			}
		}
	}
}