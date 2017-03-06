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
import javafx.scene.Node;
import javafx.scene.control.*;

import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

/**
 * Context menu to be shown on a {@link TrackTableView}.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 */
public class TrackTableViewContextMenu extends ContextMenu {

    private StageDemon stageDemon = StageDemon.getInstance();
    private MusicLibrary musicLibrary = MusicLibrary.getInstance();

    private Menu addToPlaylistMenu;
    private List<MenuItem> playlistsInMenu;

    private ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty;
    private ListProperty<Entry<Integer, Track>> selectedTracks;

    public TrackTableViewContextMenu() {
        super();
        NavigationController navigationController = stageDemon.getNavigationController();
        selectedPlaylistProperty = navigationController.selectedPlaylistProperty();

        playlistsInMenu = new ArrayList<>();
        addToPlaylistMenu = new Menu("Add to playlist");

        MenuItem playMenuItem = new MenuItem("Play");
        playMenuItem.setOnAction(event -> {
            if (! selectedTracks.isEmpty())
                PlayerFacade.getInstance().addTracksToPlayQueue(trackSelectionIds(selectedTracks), true);
        });

        MenuItem editMenuItem = new MenuItem("Edit");
        editMenuItem.setOnAction(event -> {
            if (! selectedTracks.isEmpty())
                stageDemon.editTracks(selectedTracks.size());
        });

        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(event -> {
            if (! selectedTracks.isEmpty())
                stageDemon.deleteTracks(trackSelectionIds(selectedTracks));
        });

        MenuItem addToQueueMenuItem = new MenuItem("Add to play queue");
        addToQueueMenuItem.setOnAction(event -> {
            if (! selectedTracks.isEmpty())
                PlayerFacade.getInstance().addTracksToPlayQueue(trackSelectionIds(selectedTracks), false);
        });

        MenuItem deleteFromPlaylistMenuItem = new MenuItem("Delete from playlist");
        deleteFromPlaylistMenuItem.setId("deleteFromPlaylistMenuItem");

        deleteFromPlaylistMenuItem.setOnAction(event -> {
            if (! selectedTracks.isEmpty()) {
                selectedPlaylistProperty.getValue().get().removeTracks(trackSelectionIds(selectedTracks));
                musicLibrary.removeFromShowingTracks(selectedTracks);
            }
        });

        deleteFromPlaylistMenuItem.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> selectedPlaylistProperty.getValue().isPresent() &&
                        ! selectedPlaylistProperty.getValue().get().isFolder(),
                selectedPlaylistProperty));

        getItems().addAll(playMenuItem, editMenuItem, deleteMenuItem, addToQueueMenuItem, new SeparatorMenuItem());
        getItems().addAll(deleteFromPlaylistMenuItem, addToPlaylistMenu);
    }

    private List<Integer> trackSelectionIds(List<Entry<Integer, Track>> entriesSelection) {
        return entriesSelection.stream().map(Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public void show(Node anchor, double screenX, double screenY) {
        playlistsInMenu.clear();
        selectedTracks = stageDemon.getRootController().selectedTracksProperty();
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
            if (! selectedTracks.isEmpty()) {
                playlist.addTracks(trackSelectionIds(selectedTracks));
                selectedPlaylistProperty.getValue().ifPresent(selectedPlaylist -> {
                    if (selectedPlaylist.equals(playlist))
                        musicLibrary.addToShowingTracks(selectedTracks);
                });
            }
        });
        playlistsInMenu.add(playlistMenuItem);
    }
}
