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
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

/**
 * Context menu to be shown on the track table
 * 
 * @author Octavio Calleya
 *
 */
public class TrackTableViewContextMenu extends ContextMenu {
	
	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	
	private Menu cmAddToPlaylist;
	private MenuItem cmPlay, cmEdit, cmDelete, cmAddToQueue, cmDeleteFromPlaylist;
	private ObservableList<Entry<Integer, Track>> trackSelection;

	private List<MenuItem> playlistsInMenu;
	
	public TrackTableViewContextMenu(ObservableList<Map.Entry<Integer, Track>> tableSelection) {
		super();
		playlistsInMenu = new ArrayList<>();
		trackSelection = tableSelection;
		cmPlay = new MenuItem("Play");
		cmPlay.setOnAction(event -> {
			if(!trackSelection.isEmpty())
				PlayerFacade.getInstance().addTracks(trackSelection.stream().map(Map.Entry::getKey).collect(Collectors.toList()), true);
		});
		cmEdit = new MenuItem("Edit");
		cmEdit.setOnAction(event -> stageDemon.editTracks());
		cmDelete = new MenuItem("Delete");
		cmDelete.setOnAction(event -> stageDemon.deleteTracks());
		cmAddToQueue = new MenuItem("Add to Play Queue");
		cmAddToQueue.setOnAction(event -> {
			if(!trackSelection.isEmpty())
				PlayerFacade.getInstance().addTracks(trackSelection.stream().map(Map.Entry::getKey).collect(Collectors.toList()), false);
		});
		cmDeleteFromPlaylist = new MenuItem("Delete from playlist");
		cmDeleteFromPlaylist.setId("cmDeleteFromPlaylist");
		cmDeleteFromPlaylist.setOnAction(event -> {
			ObservableList<Map.Entry<Integer, Track>> trackSelection = stageDemon.getRootController().getSelectedItems();
			if(!trackSelection.isEmpty()) {
				Playlist selectedPlaylist = stageDemon.getNavigationController().selectedPlaylistProperty().getValue().getValue();
				musicLibrary.removeFromPlaylist(selectedPlaylist, trackSelection.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
			}
		});
		cmAddToPlaylist = new Menu("Add to Playlist");
		
		ReadOnlyObjectProperty<TreeItem<Playlist>> selectedPlaylist = StageDemon.getInstance().getNavigationController().selectedPlaylistProperty();
		cmDeleteFromPlaylist.disableProperty().bind(Bindings.createBooleanBinding(() -> selectedPlaylist.getValue() == null, selectedPlaylist));
	
		getItems().addAll(cmPlay, cmEdit, cmDelete, cmAddToQueue, new SeparatorMenuItem(), cmDeleteFromPlaylist, cmAddToPlaylist);
	}
	
	protected MenuItem getDeleteFromPlaylistMI() {
		return cmDeleteFromPlaylist;
	}
	
	@Override
	public void show(Node anchor, double screenX, double screenY) {
		playlistsInMenu.clear();

		for(Playlist playlist: musicLibrary.getPlaylists())
			if(!playlist.isFolder())
				addPlaylistToMenuList(playlist);
			else
				playlist.getContainedPlaylists().forEach(childPlaylist -> addPlaylistToMenuList(childPlaylist));

		cmAddToPlaylist.getItems().clear();
		cmAddToPlaylist.getItems().addAll(playlistsInMenu);
		super.show(anchor, screenX, screenY);
	}

	private void addPlaylistToMenuList(Playlist playlist) {
		String playListName = playlist.getName();
		MenuItem playlistItem = new MenuItem(playListName);
		playlistItem.setOnAction(e -> {
			if(!trackSelection.isEmpty())
				musicLibrary.addToPlaylist(playListName, trackSelection.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
		});
		playlistsInMenu.add(playlistItem);
	}
}
