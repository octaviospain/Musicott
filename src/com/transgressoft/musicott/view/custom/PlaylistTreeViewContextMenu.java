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
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package com.transgressoft.musicott.view.custom;

import com.google.inject.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.view.*;
import javafx.scene.control.*;

/**
 * Context menu to be shown on the playlist pane
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class PlaylistTreeViewContextMenu extends ContextMenu {

    @Inject
    public PlaylistTreeViewContextMenu(@RootCtrl RootController rootCtrl, @NavigationCtrl NavigationController navCtrl) {
        super();
        MenuItem addPlaylist = new MenuItem("Add new playlist");
        addPlaylist.setOnAction(e -> rootCtrl.enterNewPlaylistName(false));

        MenuItem deletePlaylist = new MenuItem("Delete playlist");
        deletePlaylist.setOnAction(e -> navCtrl.deleteSelectedPlaylist());
        getItems().addAll(addPlaylist, deletePlaylist);
    }
}