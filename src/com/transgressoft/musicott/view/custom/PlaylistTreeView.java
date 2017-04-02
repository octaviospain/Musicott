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
import com.transgressoft.musicott.model.*;
import javafx.beans.property.*;
import javafx.scene.control.*;
import org.fxmisc.easybind.*;

import java.util.*;

import static com.transgressoft.musicott.model.PlaylistsLibrary.*;

/**
 * Class that extends from a {@link TreeView} representing a list of
 * {@link Playlist} items, which some of them are folders and could have other
 * playlists inside of them.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class PlaylistTreeView extends TreeView<Playlist> {

    private Map<Playlist, TreeItem<Playlist>> playlistsItemsMap;
    private TreeItem<Playlist> root;
    private ObjectProperty<Optional<Playlist>> selectedPlaylistProperty;

    private MusicLibrary musicLibrary = MusicLibrary.getInstance();

    public PlaylistTreeView() {
        super();
        createPlaylistsItems();
        setRoot(root);
        setShowRoot(false);
        setEditable(true);
        setPrefHeight(USE_COMPUTED_SIZE);
        setPrefWidth(USE_COMPUTED_SIZE);
        setId("playlistTreeView");
        setCellFactory(cell -> new PlaylistTreeCell());
        setContextMenu(new PlaylistTreeViewContextMenu());

        selectedPlaylistProperty = new SimpleObjectProperty<>(this, "selected playlist", Optional.empty());
        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        EasyBind.subscribe(getSelectionModel().selectedItemProperty(), newItem -> {
            if (newItem != null)
                selectedPlaylistProperty.set(Optional.of(newItem.getValue()));
            else
                selectedPlaylistProperty.set(Optional.empty());
        });
    }

    /**
     * Initializes the {@link TreeView} with all the playlists.
     */
    private void createPlaylistsItems() {
        playlistsItemsMap = new HashMap<>();
        root = new TreeItem<>(ROOT_PLAYLIST);
        playlistsItemsMap.put(ROOT_PLAYLIST, root);
        Graph<Playlist> playlistsGraph = musicLibrary.playlists.getPlaylistsTree();
        addPlaylistsToFolder(ROOT_PLAYLIST, playlistsGraph.successors(ROOT_PLAYLIST));
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
            addPlaylistToItemsMap(playlistsItemsMap.get(parent), playlistChild);
            if (playlistChild.isFolder())
                addPlaylistsToFolder(playlistChild, playlistChild.getContainedPlaylists());
        });
    }

    private void addPlaylistToItemsMap(TreeItem<Playlist> parentPlaylistItem, Playlist playlist) {
        TreeItem<Playlist> item = new TreeItem<>(playlist);
        parentPlaylistItem.getChildren().add(item);
        playlistsItemsMap.put(playlist, item);
    }

    public void selectPlaylist(Playlist playlist) {
        getSelectionModel().select(playlistsItemsMap.get(playlist));
    }

    public void movePlaylist(String movedPlaylistName, Playlist targetFolder) {
        Playlist movedPlaylist = getPlaylistFromName(movedPlaylistName);
        TreeItem<Playlist> movedPlaylistItem = playlistsItemsMap.get(movedPlaylist);
        Playlist parentMovedPlaylist = musicLibrary.playlists.getParentPlaylist(movedPlaylist);
        TreeItem<Playlist> parentMovedPlaylistItem = playlistsItemsMap.get(parentMovedPlaylist);
        TreeItem<Playlist> targetPlaylistItem = playlistsItemsMap.get(targetFolder);

        parentMovedPlaylistItem.getChildren().remove(movedPlaylistItem);
        playlistsItemsMap.remove(movedPlaylist, movedPlaylistItem);
        addPlaylistToItemsMap(targetPlaylistItem, movedPlaylist);
        if (movedPlaylist.isFolder())
            addPlaylistsToFolder(movedPlaylist, movedPlaylist.getContainedPlaylists());
        musicLibrary.playlists.movePlaylist(movedPlaylist, targetFolder);
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
        Playlist parent = musicLibrary.playlists.getParentPlaylist(playlist);
        TreeItem<Playlist> parentItem = playlistsItemsMap.get(parent);
        parentItem.getChildren().remove(playlistItem);
        playlistsItemsMap.remove(playlist);
    }

    public ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty() {
        return selectedPlaylistProperty;
    }
}
