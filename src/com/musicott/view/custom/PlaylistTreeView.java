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
import javafx.beans.property.*;
import javafx.scene.control.*;

import java.util.*;

/**
 * Class that extends from a {@link TreeView} representing a list of
 * {@link Playlist} items, which some of them are folders and could have other
 * playlists inside of them.
 *
 * @author Octavio Calleya
 * @version 0.9
 */
public class PlaylistTreeView extends TreeView<Playlist> {

	private TreeItem<Playlist> root;
	private PlaylistTreeViewContextMenu contextMenu;
	private ReadOnlyObjectProperty<TreeItem<Playlist>> selectedItemProperty;

	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();

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
		selectedItemProperty = getSelectionModel().selectedItemProperty();
		selectedItemProperty.addListener((obs, oldSelected, newSelected) -> {
			if(newSelected != null)
				musicLibrary.showPlaylist(newSelected.getValue());
		});
		
		contextMenu = new PlaylistTreeViewContextMenu();
		setContextMenu(contextMenu);
		createPlaylistsItems();
	}

	/**
	 * Initializes the {@link TreeView} with all the playlists.
	 */
	private void createPlaylistsItems() {
		musicLibrary.getPlaylists().forEach(playlist -> {
			if(playlist.isFolder()) {
				TreeItem<Playlist> folderItem = new TreeItem<>(playlist);
				playlist.getContainedPlaylists().forEach(childPlaylist ->
					folderItem.getChildren().add(new TreeItem<>(childPlaylist)));

				root.getChildren().add(folderItem);
			}
			else {
				root.getChildren().add(new TreeItem<>(playlist));
			}
		});
	}

	public Optional<Playlist> getSelectedPlaylist() {
		TreeItem<Playlist> selectedPlaylistTreeItem = selectedItemProperty.getValue();
		if(selectedPlaylistTreeItem == null)
			return Optional.empty();
		else
			return Optional.of(selectedPlaylistTreeItem.getValue());
	}

	/**
	 * Adds a new {@link TreeItem} given a {@link Playlist}.
	 *
	 * @param newPlaylist The playlist value of the <tt>TreeItem</tt>
	 */
	public void addPlaylist(Playlist newPlaylist) {
		TreeItem<Playlist> newItem = new TreeItem<>(newPlaylist);
		root.getChildren().add(newItem);
		getSelectionModel().select(newItem);
	}

	/**
	 * Adds a new {@link TreeItem} given a {@link Playlist} that has to be
	 * included in a folder <tt>Playlist</tt>.
	 *
	 * @param folder The folder playlist
	 * @param newPlaylistChild The playlist to add to the folder playlist
	 */
	public void addPlaylistChild(Playlist folder, Playlist newPlaylistChild) {
		TreeItem<Playlist> folderTreeItem = root.getChildren().stream()
				.filter(child -> child.getValue().equals(folder))
				.findFirst().get();

		TreeItem<Playlist> newPlaylistItem = new TreeItem<>(newPlaylistChild);
		folderTreeItem.getChildren().add(newPlaylistItem);

		folder.addPlaylistChild(newPlaylistChild);
		getSelectionModel().select(newPlaylistItem);
	}

	/**
	 * Deletes the {@link TreeItem} that has the value of the selected {@link Playlist}
	 */
	public void deletePlaylist() {
		Optional<Playlist> selected = getSelectedPlaylist();
		selected.ifPresent(selectedPlaylist -> {
			musicLibrary.deletePlaylist(selectedPlaylist);
			boolean removed = root.getChildren().removeIf(treeItem -> treeItem.getValue().equals(selectedPlaylist));

			if(!removed)
				deletePlaylistInSomeFolder(selectedPlaylist);

			selectFirstPlaylistOrAllSongsMode();
		});
	}

	/**
	 * Deletes the {@link TreeItem} that has the value of the given {@link Playlist}
	 * that is a child of some <tt>TreeItem</tt> of the root, a folder playlist.
	 *
	 * @param playlistToDelete The playlist to delete
	 */
	private void deletePlaylistInSomeFolder(Playlist playlistToDelete) {
		root.getChildren().stream().filter(playlist -> !playlist.getChildren().isEmpty())
				.forEach(folder -> {
					ListIterator<TreeItem<Playlist>> childrenIterator = folder.getChildren().listIterator();
					while (childrenIterator.hasNext())
						if (childrenIterator.next().getValue().equals(playlistToDelete))
							childrenIterator.remove();
				});
	}

	/**
	 * Selects the first item on the {@link TreeView}, or if it hasn't any,
	 * changes the view to show all the songs.
	 */
	private void selectFirstPlaylistOrAllSongsMode() {
		int playlistToDeleteIndex = getSelectionModel().getSelectedIndex();
		if(playlistToDeleteIndex == 0 && root.getChildren().isEmpty())
			stageDemon.getNavigationController().setNavigationMode(NavigationMode.ALL_TRACKS);
		else
			getSelectionModel().selectFirst();
	}
}
