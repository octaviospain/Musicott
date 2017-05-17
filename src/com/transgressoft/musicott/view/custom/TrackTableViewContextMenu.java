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
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.view.*;
import javafx.beans.property.*;
import javafx.event.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;

import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

/**
 * Context menu to be shown on a {@link TrackTableView}.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class TrackTableViewContextMenu extends ContextMenu {

    private final PlaylistsLibrary playlistsLibrary;

    private RootController rootController;
    private EditController editController;
    private Menu addToPlaylistMenu;
    private MenuItem deleteFromPlaylistMenuItem;
    private List<MenuItem> playlistsInMenu = new ArrayList<>();

    private List<Entry<Integer, Track>> selectedEntries;
    private ListProperty<Entry<Integer, Track>> showingTracksProperty;
    private ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty;

    @Inject
    public TrackTableViewContextMenu(PlaylistsLibrary playlistsLibrary, MusicLibrary musicLibrary,
            PlayerFacade playerFacade) {
        super();
        this.playlistsLibrary = playlistsLibrary;
        addToPlaylistMenu = new Menu("Add to playlist");

        MenuItem playMenuItem = new MenuItem("Play");
        playMenuItem.setOnAction(event -> {
            if (! selectedEntries.isEmpty())
                playerFacade.addTracksToPlayQueue(trackSelection(selectedEntries), true);
        });

        MenuItem editMenuItem = new MenuItem("Edit");
        editMenuItem.setOnAction(event -> {
            if (! selectedEntries.isEmpty())
                editController.editTracks(trackSelection(selectedEntries));
        });

        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(event -> {
            if (! selectedEntries.isEmpty())
                musicLibrary.deleteTracks(trackSelection(selectedEntries));
        });

        MenuItem addToQueueMenuItem = new MenuItem("Add to play queue");
        addToQueueMenuItem.setOnAction(event -> {
            if (! selectedEntries.isEmpty())
                playerFacade.addTracksToPlayQueue(trackSelection(selectedEntries), false);
        });

        deleteFromPlaylistMenuItem = new MenuItem("Delete from playlist");
        deleteFromPlaylistMenuItem.setId("deleteFromPlaylistMenuItem");

        deleteFromPlaylistMenuItem.setOnAction(event -> {
            if (! selectedEntries.isEmpty()) {
                selectedPlaylistProperty.get().get().removeTracks(trackSelectionIds(selectedEntries));
                showingTracksProperty.removeAll(selectedEntries);
            }
        });

        getItems().addAll(playMenuItem, editMenuItem, deleteMenuItem, addToQueueMenuItem, new SeparatorMenuItem());
        getItems().addAll(deleteFromPlaylistMenuItem, addToPlaylistMenu);
    }

    private List<Integer> trackSelectionIds(List<Entry<Integer, Track>> entriesSelection) {
        return entriesSelection.stream().map(Entry::getKey).collect(Collectors.toList());
    }

    private List<Track> trackSelection(List<Entry<Integer, Track>> entriesSelection) {
        return entriesSelection.stream().map(Entry::getValue).collect(Collectors.toList());
    }

    @Override
    public void show(Node anchor, double screenX, double screenY) {
        playlistsInMenu.clear();
        selectedEntries = rootController.getSelectedTracks();
        Optional<Playlist> selectedPlaylist = selectedPlaylistProperty.get();
        if (selectedPlaylist.isPresent() && ! selectedPlaylist.get().isFolder())  {
            playlistsLibrary.getPlaylistsTree().nodes()
                                  .stream().filter(p -> ! p.isFolder())
                                  .forEach(this::addPlaylistToMenuList);

            addToPlaylistMenu.getItems().clear();
            addToPlaylistMenu.getItems().addAll(playlistsInMenu);
            deleteFromPlaylistMenuItem.setVisible(true);
        }
        else
            deleteFromPlaylistMenuItem.setVisible(false);
        super.show(anchor, screenX, screenY);
    }

    private void addPlaylistToMenuList(Playlist playlist) {
        Optional<Playlist> selectedPlaylist = selectedPlaylistProperty.get();
        if (! (selectedPlaylist.isPresent() && selectedPlaylist.get().equals(playlist))) {
            MenuItem playlistMenuItem = new MenuItem(playlist.getName());
            playlistMenuItem.setOnAction(event -> {
                if (! selectedEntries.isEmpty())
                    playlist.addTracks(trackSelectionIds(selectedEntries));
            });
            playlistsInMenu.add(playlistMenuItem);
        }
    }

    @Inject
    public void setRootController(@RootCtrl RootController c) {
        rootController = c;
    }

    @Inject
    public void setEditController(@EditCtrl EditController c) {
        this.editController = c;
    }

    @Inject
    public void setSelectedPlaylistProperty(@SelectedPlaylistProperty ReadOnlyObjectProperty<Optional<Playlist>> p) {
        selectedPlaylistProperty = p;
    }

    @Inject
    public void setShowingTracksProperty(@ShowingTracksProperty ListProperty<Entry<Integer, Track>> p) {
        showingTracksProperty = p;
    }

    public static EventHandler<MouseEvent> showContextMenuEventHandler(Node nodeInWhichShow, ContextMenu contextMenu) {
        return event -> {
            if (event.getButton() == MouseButton.SECONDARY)
                contextMenu.show(nodeInWhichShow, event.getScreenX(), event.getScreenY());
            else if (event.getButton() == MouseButton.PRIMARY && contextMenu.isShowing())
                contextMenu.hide();
        };
    }
}