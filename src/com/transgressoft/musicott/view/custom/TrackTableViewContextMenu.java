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

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.view.*;
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
 * @version 0.9.2-b
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
        trackSelection = tableSelection;
        playlistsInMenu = new ArrayList<>();
        addToPlaylistMenu = new Menu("Add to playlist");

        playMenuItem = new MenuItem("Play");
        playMenuItem.setOnAction(event -> {
            if (! trackSelection.isEmpty())
                PlayerFacade.getInstance().addTracksToPlayQueue(trackSelectionIds(), true);
        });

        editMenuItem = new MenuItem("Edit");
        editMenuItem.setOnAction(event -> stageDemon.editTracks());

        deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(event -> stageDemon.deleteTracks());

        addToQueueMenuItem = new MenuItem("Add to play Queue");
        addToQueueMenuItem.setOnAction(event -> {
            if (! trackSelection.isEmpty())
                PlayerFacade.getInstance().addTracksToPlayQueue(trackSelectionIds(), false);
        });

        deleteFromPlaylistMenuItem = new MenuItem("Delete from playlist");
        deleteFromPlaylistMenuItem.setId("deleteFromPlaylistMenuItem");

        NavigationController navigationController = stageDemon.getNavigationController();
        deleteFromPlaylistMenuItem.setOnAction(event -> {
            if (! trackSelection.isEmpty()) {
                Optional<Playlist> selectedPlaylist = navigationController.selectedPlaylistProperty().getValue();
                selectedPlaylist.ifPresent(playlist -> playlist.removeTracks(trackSelectionIds()));
            }
        });

        ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty = navigationController
                .selectedPlaylistProperty();
        deleteFromPlaylistMenuItem.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> selectedPlaylistProperty.getValue().isPresent() && ! selectedPlaylistProperty.getValue().get()
                                                                                                   .isFolder(),
                selectedPlaylistProperty));

        getItems().addAll(playMenuItem, editMenuItem, deleteMenuItem, addToQueueMenuItem, new SeparatorMenuItem());
        getItems().addAll(deleteFromPlaylistMenuItem, addToPlaylistMenu);
    }

    private List<Integer> trackSelectionIds() {
        return trackSelection.stream().map(Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public void show(Node anchor, double screenX, double screenY) {
        playlistsInMenu.clear();

        musicLibrary.getPlaylists().forEach(playlist -> {
            if (! playlist.isFolder())
                addPlaylistToMenuList(playlist);
            else
                playlist.getContainedPlaylists().forEach(this::addPlaylistToMenuList);
        });

        addToPlaylistMenu.getItems().clear();
        addToPlaylistMenu.getItems().addAll(playlistsInMenu);
        super.show(anchor, screenX, screenY);
    }

    private void addPlaylistToMenuList(Playlist playlist) {
        MenuItem playlistMenuItem = new MenuItem(playlist.getName());
        playlistMenuItem.setOnAction(event -> {
            if (! trackSelection.isEmpty())
                playlist.addTracks(trackSelectionIds());
        });
        playlistsInMenu.add(playlistMenuItem);
    }
}
