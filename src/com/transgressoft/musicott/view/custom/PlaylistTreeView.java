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

package com.transgressoft.musicott.view.custom;

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import javafx.beans.property.*;
import javafx.scene.control.*;

import java.util.*;
import java.util.stream.*;

/**
 * Class that extends from a {@link TreeView} representing a list of
 * {@link Playlist} items, which some of them are folders and could have other
 * playlists inside of them.
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 */
public class PlaylistTreeView extends TreeView<Playlist> {

	private TreeItem<Playlist> root;
	private PlaylistTreeViewContextMenu contextMenu;
	private ObjectProperty<Optional<Playlist>> selectedPlaylistProperty;

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
		setCellFactory(PlaylistTreeCell::new);

		selectedPlaylistProperty = new SimpleObjectProperty<>(this, "selected playlist", Optional.empty());
		getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
			if (newItem != null)
				selectedPlaylistProperty.set(Optional.of(newItem.getValue()));
			else
				selectedPlaylistProperty.set(Optional.empty());
		});
		selectedPlaylistProperty.addListener((obs, oldSelectedPlaylist, newSelectedPlaylist) -> {
			if (newSelectedPlaylist.isPresent()) {
				Playlist playlist = newSelectedPlaylist.get();
				playlist.showTracksOnTable();
				stageDemon.getNavigationController().setNavigationMode(NavigationMode.PLAYLIST);
			}
		});

		contextMenu = new PlaylistTreeViewContextMenu();
		setContextMenu(contextMenu);
		createPlaylistsItems();
	}

	/**
	 * Initializes the {@link TreeView} with all the playlists.
	 */
	private void createPlaylistsItems() {
		synchronized (musicLibrary.getPlaylists()) {
			musicLibrary.getPlaylists().forEach(playlist -> {
				if (playlist.isFolder()) {
					TreeItem<Playlist> folderItem = new TreeItem<>(playlist);
					playlist.getContainedPlaylists()
							.forEach(childPlaylist -> folderItem.getChildren().add(new TreeItem<>(childPlaylist)));

					root.getChildren().add(folderItem);
				}
				else
					root.getChildren().add(new TreeItem<>(playlist));
			});
		}
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
	 * @param folder           The folder playlist
	 * @param newPlaylistChild The playlist to add to the folder playlist
	 */
	public void addPlaylistChild(Playlist folder, Playlist newPlaylistChild) {
		TreeItem<Playlist> folderTreeItem = root.getChildren().stream().filter(child -> child.getValue().equals
				(folder))
												.findFirst().get();

		TreeItem<Playlist> newPlaylistItem = new TreeItem<>(newPlaylistChild);
		folderTreeItem.getChildren().add(newPlaylistItem);

		folder.getContainedPlaylists().add(newPlaylistChild);
		getSelectionModel().select(newPlaylistItem);
	}

	/**
	 * Deletes the {@link TreeItem} that has the value of the selected {@link Playlist}
	 */
	public void deletePlaylist() {
		Optional<Playlist> selectedItem = selectedPlaylistProperty.getValue();
		selectedItem.ifPresent(selectedPlaylist -> {
			musicLibrary.deletePlaylist(selectedPlaylist);
			boolean removed = root.getChildren().removeIf(treeItem -> treeItem.getValue().equals(selectedPlaylist));

			if (! removed)
				deletePlaylistInSomeFolder(selectedPlaylist);

			if (root.getChildren().isEmpty())
				stageDemon.getNavigationController().setNavigationMode(NavigationMode.ALL_TRACKS);
		});
	}

	/**
	 * Deletes the {@link TreeItem} that has the value of the given {@link Playlist}
	 * that is a child of some <tt>TreeItem</tt> of the root, a folder playlist;
	 * and the playlist from the the folder.
	 *
	 * @param playlistToDelete The playlist to delete
	 */
	private void deletePlaylistInSomeFolder(Playlist playlistToDelete) {
		List<TreeItem<Playlist>> notEmptyFolders = root.getChildren().stream()
													   .filter(playlist -> ! playlist.getChildren().isEmpty())
													   .collect(Collectors.toList());
	search:
		for (TreeItem<Playlist> playlistTreeItem : notEmptyFolders) {
			ListIterator<TreeItem<Playlist>> childrenIterator = playlistTreeItem.getChildren().listIterator();
			while (childrenIterator.hasNext()) {
				if (childrenIterator.next().getValue().equals(playlistToDelete)) {
					childrenIterator.remove();
					break search;
				}
			}
		}
	}

	public ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty() {
		return selectedPlaylistProperty;
	}
}
