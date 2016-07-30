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
import javafx.scene.control.*;

/**
 * Context menu to be shown on the playlist pane
 *
 * @author Octavio Calleya
 * @version 0.9-b
 */
public class PlaylistTreeViewContextMenu extends ContextMenu {

	private StageDemon stageDemon = StageDemon.getInstance();

	private MenuItem addPlaylist;
	private MenuItem deletePlaylist;

	public PlaylistTreeViewContextMenu() {
		super();

		addPlaylist = new MenuItem("Add new playlist");
		addPlaylist.setOnAction(e -> stageDemon.getRootController().enterNewPlaylistName(false));

		deletePlaylist = new MenuItem("Delete playlist");
		deletePlaylist.setOnAction(e -> stageDemon.getNavigationController().deleteSelectedPlaylist());
		getItems().addAll(addPlaylist, deletePlaylist);
	}
}
