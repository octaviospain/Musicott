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

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

/**
 * Context menu to be shown on the playlist pane
 * 
 * @author Octavio Calleya
 *
 */
public class PlaylistTreeViewContextMenu extends ContextMenu {
	
	private SceneManager sc = SceneManager.getInstance();
	
	private MenuItem addPlaylist;
	private MenuItem deletePlaylist;

	public PlaylistTreeViewContextMenu() {
		super();
		
		addPlaylist = new MenuItem("Add new playlist");		
		addPlaylist.setOnAction(e -> sc.getRootController().setNewPlaylistMode());

		deletePlaylist = new MenuItem("Delete playlist");
		deletePlaylist.setOnAction(e -> sc.getNavigationController().deleteSelectedPlaylist());
		getItems().addAll(addPlaylist, deletePlaylist);
	}
}
