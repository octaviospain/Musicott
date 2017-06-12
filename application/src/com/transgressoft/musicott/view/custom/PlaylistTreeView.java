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

import com.google.common.graph.*;
import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import javafx.beans.property.*;
import javafx.scene.control.*;

import java.util.*;

import static org.fxmisc.easybind.EasyBind.*;

/**
 * Class that extends from a {@link TreeView} representing a list of
 * {@link Playlist} items, which some of them are folders and could have other
 * playlists inside of them.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 */
@Singleton
public class PlaylistTreeView extends TreeView<Playlist> {

    private final PlaylistsLibrary playlistsLibrary;
    private final Map<Playlist, TreeItem<Playlist>> playlistsItemsMap = new HashMap<>();

    private ObjectProperty<Optional<Playlist>> selectedPlaylistProperty;

    @Inject
    public PlaylistTreeView(PlaylistsLibrary playlistsLibrary, TracksLibrary tracksLibrary,
            @RootPlaylist Playlist rootPlaylist, Injector injector) {
        this.playlistsLibrary = playlistsLibrary;
        createPlaylistsItems(rootPlaylist);
        setShowRoot(false);
        setEditable(true);
        setPrefHeight(USE_COMPUTED_SIZE);
        setPrefWidth(USE_COMPUTED_SIZE);
        setId("playlistTreeViewContextMenu");
        setCellFactory(cell -> injector.getInstance(PlaylistTreeCell.class));

        selectedPlaylistProperty = new SimpleObjectProperty<>(this, "selected playlist", Optional.empty());
        subscribe(selectedPlaylistProperty,
                  playlist -> playlist.ifPresent(tracksLibrary::showPlaylist));

        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        subscribe(getSelectionModel().selectedItemProperty(), newItem -> {
            if (newItem != null)
                selectedPlaylistProperty.set(Optional.of(newItem.getValue()));
            else
                selectedPlaylistProperty.set(Optional.empty());
        });
    }

    /**
     * Initializes the {@link TreeView} with all the playlists.
     *
     * @param rootPlaylist The root {@link Playlist} injected object
     */
    private void createPlaylistsItems(Playlist rootPlaylist) {
        TreeItem<Playlist> rootItem = new TreeItem<>(rootPlaylist);
        setRoot(rootItem);
        playlistsItemsMap.put(rootPlaylist, rootItem);
        Graph<Playlist> playlistsGraph = playlistsLibrary.getPlaylistsTree();
        addPlaylistsToFolder(rootPlaylist, playlistsGraph.successors(rootPlaylist));
    }

    /**
     * Adds several {@link Playlist}s to the view, given the folder
     * playlist where they belong to.
     *
     * @param parent       The parent folder playlist where to add the playlists
     * @param newPlaylists The playlists to add to the {@link TreeItem}
     */
    public void addPlaylistsToFolder(Playlist parent, Set<Playlist> newPlaylists) {
        newPlaylists.forEach(playlistChild -> {
            parent.addPlaylistChild(playlistChild);
            addPlaylistToItemsMap(playlistsItemsMap.get(parent), playlistChild);
            if (playlistChild.isFolder())
                addPlaylistsToFolder(playlistChild, playlistChild.getContainedPlaylists());
        });
    }

    private void addPlaylistToItemsMap(TreeItem<Playlist> parentPlaylistItem, Playlist playlist) {
        TreeItem<Playlist> item = new TreeItem<>(playlist);
        parentPlaylistItem.getChildren().add(item);
        parentPlaylistItem.getChildren().sort(Comparator.comparing(a -> a.getValue().getName()));
        playlistsItemsMap.put(playlist, item);
    }

    public void selectPlaylist(Playlist playlist) {
        getSelectionModel().select(playlistsItemsMap.get(playlist));
    }

    public void clearAndSelect(int index) {
        getSelectionModel().clearAndSelect(index);
    }

    public void movePlaylist(String movedPlaylistName, Playlist targetFolder) {
        Playlist movedPlaylist = getPlaylistFromName(movedPlaylistName);
        TreeItem<Playlist> movedPlaylistItem = playlistsItemsMap.get(movedPlaylist);
        Playlist parentMovedPlaylist = playlistsLibrary.getParentPlaylist(movedPlaylist);
        TreeItem<Playlist> parentMovedPlaylistItem = playlistsItemsMap.get(parentMovedPlaylist);
        TreeItem<Playlist> targetPlaylistItem = playlistsItemsMap.get(targetFolder);

        parentMovedPlaylistItem.getChildren().remove(movedPlaylistItem);
        playlistsItemsMap.remove(movedPlaylist, movedPlaylistItem);
        addPlaylistToItemsMap(targetPlaylistItem, movedPlaylist);
        if (movedPlaylist.isFolder())
            addPlaylistsToFolder(movedPlaylist, movedPlaylist.getContainedPlaylists());
        playlistsLibrary.movePlaylist(movedPlaylist, targetFolder);
        selectPlaylist(movedPlaylist);
    }

    private Playlist getPlaylistFromName(String playlistName) {
        return playlistsItemsMap.entrySet().stream()
                                .filter(entry -> entry.getKey().getName().equals(playlistName))
                                .findFirst().get().getKey();
    }

    /**
     * Deletes the {@link TreeItem} that has the value of the selected {@link Playlist}
     */
    public void deletePlaylist(Playlist playlist) {
        TreeItem<Playlist> playlistItem = playlistsItemsMap.get(playlist);
        Playlist parent = playlistsLibrary.getParentPlaylist(playlist);
        TreeItem<Playlist> parentItem = playlistsItemsMap.get(parent);
        parentItem.getChildren().remove(playlistItem);
        playlistsItemsMap.remove(playlist);
    }

    @Inject
    public void setPlaylistTreeViewContextMenu(PlaylistTreeViewContextMenu contextMenu) {
        setContextMenu(contextMenu);
    }

    public ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty() {
        return selectedPlaylistProperty;
    }
}
