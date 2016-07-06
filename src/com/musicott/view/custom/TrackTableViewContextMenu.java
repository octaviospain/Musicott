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
import com.musicott.player.*;
import com.musicott.view.*;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

/**
 * Context menu to be shown on the {@link TrackTableView}.
 * 
 * @author Octavio Calleya
 * @version 0.9
 */
public class TrackTableViewContextMenu extends ContextMenu {
	
	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	
	private Menu addToPlaylistMenu;
	private MenuItem playMenuItem;
	private MenuItem editMenuItem;
	private MenuItem deleteMenuItem;
	private MenuItem addToQueueMenuItem;
	private MenuItem deleteFromPlaylistMenuItem;
	private ObservableList<Entry<Integer, Track>> trackSelection;

	private List<MenuItem> playlistsInMenu;
	
	public TrackTableViewContextMenu(ObservableList<Entry<Integer, Track>> tableSelection) {
		super();
		playlistsInMenu = new ArrayList<>();
		trackSelection = tableSelection;
		playMenuItem = new MenuItem("Play");
		playMenuItem.setOnAction(event -> {
			if(!trackSelection.isEmpty())
				PlayerFacade.getInstance().addTracksToPlayQueue(trackSelectionIds(), true);
		});
		editMenuItem = new MenuItem("Edit");
		editMenuItem.setOnAction(event -> stageDemon.editTracks());
		deleteMenuItem = new MenuItem("Delete");
		deleteMenuItem.setOnAction(event -> stageDemon.deleteTracks());
		addToQueueMenuItem = new MenuItem("Add to Play Queue");
		addToQueueMenuItem.setOnAction(event -> {
			if(!trackSelection.isEmpty())
				PlayerFacade.getInstance().addTracksToPlayQueue(trackSelectionIds(), false);
		});
		deleteFromPlaylistMenuItem = new MenuItem("Delete from playlist");
		deleteFromPlaylistMenuItem.setId("deleteFromPlaylistMenuItem");
		NavigationController navigationController = stageDemon.getNavigationController();
		deleteFromPlaylistMenuItem.setOnAction(event -> {
			if(!trackSelection.isEmpty()) {
				Playlist selectedPlaylist = navigationController.selectedPlaylistProperty().getValue().getValue();
				musicLibrary.deleteFromPlaylist(selectedPlaylist, trackSelectionIds());
			}
		});
		addToPlaylistMenu = new Menu("Add to Playlist");
		
		ReadOnlyObjectProperty<TreeItem<Playlist>> selectedPlaylist = navigationController.selectedPlaylistProperty();
		deleteFromPlaylistMenuItem.disableProperty().bind
				(Bindings.createBooleanBinding(() -> selectedPlaylist.getValue() == null, selectedPlaylist));
	
		getItems().addAll(playMenuItem, editMenuItem, deleteMenuItem, addToQueueMenuItem, new SeparatorMenuItem());
		getItems().addAll(deleteFromPlaylistMenuItem, addToPlaylistMenu);
	}

	@Override
	public void show(Node anchor, double screenX, double screenY) {
		playlistsInMenu.clear();

		musicLibrary.getPlaylists().forEach(playlist -> {
			if(!playlist.isFolder())
				addPlaylistToMenuList(playlist);
			else
				playlist.getContainedPlaylists().forEach(this::addPlaylistToMenuList);
		});

		addToPlaylistMenu.getItems().clear();
		addToPlaylistMenu.getItems().addAll(playlistsInMenu);
		super.show(anchor, screenX, screenY);
	}

	private void addPlaylistToMenuList(Playlist playlist) {
		String playListName = playlist.getName();
		MenuItem playlistItem = new MenuItem(playListName);
		playlistItem.setOnAction(e -> {
			if(!trackSelection.isEmpty())
				musicLibrary.addToPlaylist(playListName, trackSelectionIds());
		});
		playlistsInMenu.add(playlistItem);
	}

	private List<Integer> trackSelectionIds() {
		return trackSelection.stream().map(Entry::getKey).collect(Collectors.toList());
	}
}
