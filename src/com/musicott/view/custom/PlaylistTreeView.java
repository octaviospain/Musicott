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

package com.musicott.view.custom;

import com.musicott.*;
import com.musicott.model.*;
import javafx.collections.*;
import javafx.scene.control.*;

import java.util.*;

/**
 * @author Octavio Calleya
 */
public class PlaylistTreeView extends TreeView<Playlist> {
	
	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	
	private TreeItem<Playlist> root;
	private PlaylistTreeViewContextMenu contextMenu;
	private ObservableList<TreeItem<Playlist>> selectedPlaylist;

	public PlaylistTreeView() {
		super();
		root = new TreeItem<>();
		setRoot(root);
		setShowRoot(false);
		setEditable(true);
		setPrefHeight(USE_COMPUTED_SIZE);
		setPrefWidth(USE_COMPUTED_SIZE);
		setId("playlistTreeView");
		setCellFactory(treeView -> new PlaylistTreeCell());

		getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		selectedPlaylist = getSelectionModel().getSelectedItems();
		getSelectionModel().selectedItemProperty().addListener(l -> musicLibrary.showPlaylist(getSelectedPlaylist()));
		
		contextMenu = new PlaylistTreeViewContextMenu();
		setContextMenu(contextMenu);
		List<Playlist> allChilds = new ArrayList<>();
		for(Playlist playlist: musicLibrary.getPlaylists()) {
			if(playlist.isFolder()) {
				List<Playlist> childPlaylists = playlist.getContainedPlaylists();
				allChilds.addAll(childPlaylists);
				TreeItem<Playlist> folderItem = new TreeItem<>(playlist);

				for(Playlist childPlaylist: childPlaylists)
					folderItem.getChildren().add(new TreeItem<>(childPlaylist));
				root.getChildren().add(folderItem);
			}
			else {
				root.getChildren().add(new TreeItem<>(playlist));
			}
		}
	}
	
	public Playlist getSelectedPlaylist() {
		return selectedPlaylist.get(0) == null ? null : selectedPlaylist.get(0).getValue();
	}
	
	public void addPlaylist(Playlist newPlaylist) {
		TreeItem<Playlist> newItem = new TreeItem<>(newPlaylist);
		root.getChildren().add(newItem);
		getSelectionModel().select(newItem);
	}
	
	public void addPlaylistChild(Playlist folder, Playlist newPlaylistChild) {
		TreeItem<Playlist> newPlaylistItem = null;
		for(TreeItem<Playlist> playlistTreeItem: root.getChildren()) {
			if(playlistTreeItem.getValue().equals(folder)) {
				newPlaylistItem  = new TreeItem<>(newPlaylistChild);
				playlistTreeItem.getChildren().add(newPlaylistItem);
			}
		}
		folder.getContainedPlaylists().add(newPlaylistChild);
		getSelectionModel().select(newPlaylistItem);
	}
	
	public void deletePlaylist() {
		Playlist selected = getSelectedPlaylist();
		if(selected != null) {
			musicLibrary.removePlaylist(getSelectedPlaylist());
			int playlistToDeleteIndex = getSelectionModel().getSelectedIndex();
			root.getChildren().removeIf(treeItem -> treeItem.getValue().equals(selected));
			if(playlistToDeleteIndex == 0 && root.getChildren().isEmpty())
				stageDemon.getNavigationController().showMode(NavigationMode.ALL_SONGS_MODE);
			else
				getSelectionModel().selectFirst();
		}
	}
}
