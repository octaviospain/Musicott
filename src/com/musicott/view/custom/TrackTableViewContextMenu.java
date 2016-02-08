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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.musicott.model.MusicLibrary;
import com.musicott.model.Playlist;
import com.musicott.player.PlayerFacade;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Context menu to be shown on the track table
 * 
 * @author Octavio Calleya
 *
 */
public class TrackTableViewContextMenu extends ContextMenu {
	
	private MusicottScene musicottScene;
	private MusicLibrary ml = MusicLibrary.getInstance();
	
	private Menu cmAddToPlaylist;
	private MenuItem cmPlay, cmEdit, cmDelete, cmAddToQueue, cmDeleteFromPlaylist;
//	private ObservableList<Map.Entry<Integer, Track>> trackSelection;
	
	public TrackTableViewContextMenu(MusicottScene scene) {
		super();
		musicottScene = scene;
		
		cmPlay = new MenuItem("Play");
		cmPlay.setOnAction(event -> {
			if(!musicottScene.getTrackSelection().isEmpty())
				PlayerFacade.getInstance().addTracks(musicottScene.getTrackSelection().stream().map(Map.Entry::getKey).collect(Collectors.toList()), true);
		});
		cmEdit = new MenuItem("Edit");
		cmEdit.setOnAction(event -> musicottScene.doEdit());
		cmDelete = new MenuItem("Delete");
		cmDelete.setOnAction(event -> musicottScene.doDelete());
		cmAddToQueue = new MenuItem("Add to Play Queue");
		cmAddToQueue.setOnAction(event -> {
			if(!musicottScene.getTrackSelection().isEmpty())
				PlayerFacade.getInstance().addTracks(musicottScene.getTrackSelection().stream().map(Map.Entry::getKey).collect(Collectors.toList()), false);
		});
		cmDeleteFromPlaylist = new MenuItem("Delete from playlist");
		cmDeleteFromPlaylist.setId("cmDeleteFromPlaylist");
		cmDeleteFromPlaylist.setOnAction(event -> musicottScene.deleteFromPlaylist());
		cmAddToPlaylist = new Menu("Add to Playlist");
		
		getItems().addAll(cmPlay, cmEdit, cmDelete, cmAddToQueue, new SeparatorMenuItem(), cmDeleteFromPlaylist, cmAddToPlaylist);
	}
	
	@Override
	public void show(Node anchor, double screenX, double screenY) {
		List<MenuItem> playlistsMI = new ArrayList<>();
		for(Playlist pl: ml.getPlaylists()) {
			String playListName = pl.getName();
			MenuItem playlistItem = new MenuItem(playListName);
			playlistItem.setOnAction(e -> {
				if(!musicottScene.getTrackSelection().isEmpty())
					ml.addToPlaylist(playListName, musicottScene.getTrackSelection().stream().map(Map.Entry::getKey).collect(Collectors.toList()));
			});
			playlistsMI.add(playlistItem);
		}
		cmAddToPlaylist.getItems().clear();
		cmAddToPlaylist.getItems().addAll(playlistsMI);
		super.show(anchor, screenX, screenY);
	}
}