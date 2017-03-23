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
import javafx.beans.property.*;
import javafx.scene.control.*;

import java.util.*;
import java.util.stream.*;

/**
 * Class that extends from a {@link TreeView} representing a list of
 * {@link Playlist} items, which some of them are folders and could have other
 * playlists inside of them.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 */
public class PlaylistTreeView extends TreeView<Playlist> {

    private Map<Playlist, TreeItem<Playlist>> playlistsItemsMap;
    private TreeItem<Playlist> root;
    private ObjectProperty<Optional<Playlist>> selectedPlaylistProperty;

    private StageDemon stageDemon = StageDemon.getInstance();
    private MusicLibrary musicLibrary = MusicLibrary.getInstance();

    public PlaylistTreeView() {
        super();
        playlistsItemsMap = new HashMap<>();
        root = new TreeItem<>();
        setRoot(root);
        setShowRoot(false);
        setEditable(true);
        setPrefHeight(USE_COMPUTED_SIZE);
        setPrefWidth(USE_COMPUTED_SIZE);
        setId("playlistTreeView");
        setCellFactory(cell -> new PlaylistTreeCell());

        selectedPlaylistProperty = new SimpleObjectProperty<>(this, "selected playlist", Optional.empty());
        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null)
                selectedPlaylistProperty.set(Optional.of(newItem.getValue()));
            else
                selectedPlaylistProperty.set(Optional.empty());
        });
        selectedPlaylistProperty.addListener((obs, oldSelectedPlaylist, newSelectedPlaylist) ->
            newSelectedPlaylist.ifPresent(playlist -> {
                musicLibrary.showPlaylist(playlist);
                stageDemon.getNavigationController().setNavigationMode(NavigationMode.PLAYLIST);
            })
        );

        setContextMenu(new PlaylistTreeViewContextMenu());
        createPlaylistsItems();
    }

    /**
     * Initializes the {@link TreeView} with all the playlists.
     */
    private void createPlaylistsItems() {
        synchronized (musicLibrary.playlists.getPlaylists()) {
            musicLibrary.playlists.getPlaylists().forEach(playlist -> {
                if (playlist.isFolder()) {
                    TreeItem<Playlist> folderItem = new TreeItem<>(playlist);
                    playlist.getContainedPlaylists()
                            .forEach(childPlaylist -> addPlaylistItem(folderItem, childPlaylist));

                    addPlaylistItem(root, playlist);
                    root.getChildren().add(folderItem);
                }
                addPlaylistItem(root, playlist);
            });
        }
    }

    private void addPlaylistItem(TreeItem<Playlist> root, Playlist playlist) {
        TreeItem<Playlist> item = new TreeItem<>(playlist);
        root.getChildren().add(item);
        playlistsItemsMap.put(playlist, item);
    }

    public void selectPlaylist(Playlist playlist) {
        getSelectionModel().select(playlistsItemsMap.get(playlist));
    }

    /**
     * Adds a new {@link TreeItem} given a {@link Playlist}.
     *
     * @param newPlaylist The playlist value of the {@code TreeItem}
     */
    public void addPlaylist(Playlist newPlaylist, boolean selectAfter) {
        addPlaylistItem(root, newPlaylist);
        if (selectAfter)
            selectPlaylist(newPlaylist);
    }

    /**
     * Adds a new {@link TreeItem} given a {@link Playlist} that has to be
     * included in a folder {@code Playlist}.
     *
     * @param folder           The folder playlist
     * @param newPlaylistChild The playlist to add to the folder playlist
     */
    public void addPlaylistChild(Playlist folder, Playlist newPlaylistChild) {
        TreeItem<Playlist> folderTreeItem = root.getChildren().stream()
                                                .filter(child -> child.getValue().equals(folder))
                                                .findFirst().get();

        addPlaylistItem(folderTreeItem, newPlaylistChild);
        folder.getContainedPlaylists().add(newPlaylistChild);
        selectPlaylist(newPlaylistChild);
    }

    /**
     * Deletes the {@link TreeItem} that has the value of the selected {@link Playlist}
     */
    public void deletePlaylist() {
        Optional<Playlist> selectedItem = selectedPlaylistProperty.getValue();
        selectedItem.ifPresent(selectedPlaylist -> {
            musicLibrary.playlists.deletePlaylist(selectedPlaylist);
            boolean removed = root.getChildren().removeIf(treeItem -> treeItem.getValue().equals(selectedPlaylist));
            playlistsItemsMap.remove(selectedPlaylist);
            if (! removed)
                deletePlaylistInSomeFolder(selectedPlaylist);

            if (root.getChildren().isEmpty())
                stageDemon.getNavigationController().setNavigationMode(NavigationMode.ALL_TRACKS);
        });
    }

    /**
     * Deletes the {@link TreeItem} that has the value of the given {@link Playlist}
     * that is a child of some {@code TreeItem} of the root, a folder playlist;
     * and the playlist from the the folder.
     *
     * @param playlistToDelete The playlist to delete
     */
    private void deletePlaylistInSomeFolder(Playlist playlistToDelete) {
        List<TreeItem<Playlist>> notEmptyFolders = root.getChildren().stream()
                                                       .filter(playlist -> ! playlist.getChildren().isEmpty())
                                                       .collect(Collectors.toList());

        for (TreeItem<Playlist> playlistTreeItem : notEmptyFolders) {
            ListIterator<TreeItem<Playlist>> childrenIterator = playlistTreeItem.getChildren().listIterator();
            while (childrenIterator.hasNext()) {
                if (childrenIterator.next().getValue().equals(playlistToDelete)) {
                    childrenIterator.remove();
                    playlistsItemsMap.remove(playlistToDelete);
                    break;
                }
            }
        }
    }

    public ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty() {
        return selectedPlaylistProperty;
    }
}
