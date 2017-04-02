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

import com.transgressoft.musicott.model.*;
import javafx.beans.value.*;
import javafx.css.*;
import javafx.scene.control.*;
import javafx.scene.input.*;

import java.util.*;

import static com.transgressoft.musicott.view.custom.TrackTableRow.*;

/**
 * Custom {@link TreeCell} that define the style of his {@link Playlist}
 * managed by pseudo classes.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.9
 */
public class PlaylistTreeCell extends TreeCell<Playlist> {

    private static final DataFormat PLAYLIST_NAME_MIME_TYPE = new DataFormat("application/playlist-name");
    private static final String dragOverStyle = "-fx-effect: dropshadow(one-pass-box, rgb(99, 255, 109), 1, 1.0, 0, 0);";
    private static final String dragOverRootPlaylistStyle = "-fx-border-color: rgb(99, 255, 109);" +
                                                            "-fx-border-width: 1px;";

    private final PseudoClass playlist = PseudoClass.getPseudoClass("playlist");
    private final PseudoClass playlistSelected = PseudoClass.getPseudoClass("playlist-selected");
    private final PseudoClass folder = PseudoClass.getPseudoClass("folder");
    private final PseudoClass folderSelected = PseudoClass.getPseudoClass("folder-selected");

    public PlaylistTreeCell() {
        super();
        setOnMouseClicked(this::doubleClickOnPlaylistHandler);

        ChangeListener<Boolean> isFolderListener = (obs, oldPlaylistIsFolder, newPlaylistIsFolder) -> {
            boolean isFolder = newPlaylistIsFolder;
            boolean isSelected = selectedProperty().get();
            updatePseudoClassesStates(isFolder, isSelected);
        };

        ChangeListener<Boolean> isSelectedListener = (obs, oldValueIsSelected, newValueIsSelected) -> {
            boolean isFolder = itemProperty().getValue().isFolder();
            boolean isSelected = newValueIsSelected;
            updatePseudoClassesStates(isFolder, isSelected);
        };

        itemProperty().addListener((obs, oldPlaylist, newPlaylist) -> {
            if (oldPlaylist != null) {
                textProperty().unbind();
                setText("");
                oldPlaylist.isFolderProperty().removeListener(isFolderListener);
                selectedProperty().removeListener(isSelectedListener);
            }

            if (newPlaylist != null) {
                textProperty().bind(newPlaylist.nameProperty());
                newPlaylist.isFolderProperty().addListener(isFolderListener);
                selectedProperty().addListener(isSelectedListener);

                updatePseudoClassesStates(newPlaylist.isFolder(), selectedProperty().get());
            }
            else
                disablePseudoClassesStates();
        });

        setOnDragOver(event -> {
            if (getItem() != null && ! getItem().isFolder()) {
                event.acceptTransferModes(TransferMode.ANY);
                event.consume();
            }
        });
        setOnDragOver(this::onDragOver);
        setOnDragDropped(this::onDragDropped);
        setOnDragExited(this::onDragExited);
        setOnDragDetected(this::onDragDetected);
    }

    private void doubleClickOnPlaylistHandler(MouseEvent event) {
        Playlist thisPlaylist = getItem();
        if (event.getClickCount() == 2 && thisPlaylist != null && ! thisPlaylist.isEmpty() && ! thisPlaylist.isFolder())
            MusicLibrary.getInstance().playPlaylistRandomly(getItem());
    }

    private void updatePseudoClassesStates(boolean isFolder, boolean isSelected) {
        pseudoClassStateChanged(folder, isFolder && ! isSelected);
        pseudoClassStateChanged(folderSelected, isFolder && isSelected);
        pseudoClassStateChanged(playlist, ! isFolder && ! isSelected);
        pseudoClassStateChanged(playlistSelected, ! isFolder && isSelected);
    }

    private void disablePseudoClassesStates() {
        pseudoClassStateChanged(folder, false);
        pseudoClassStateChanged(folderSelected, false);
        pseudoClassStateChanged(playlist, false);
        pseudoClassStateChanged(playlistSelected, false);
    }

    private void onDragOver(DragEvent event) {
        if (getItem() != null) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            setStyle(dragOverStyle);
            setOpacity(0.9);
        }
        else
            if (event.getDragboard().hasContent(PLAYLIST_NAME_MIME_TYPE)) {
                getTreeView().setStyle(dragOverRootPlaylistStyle);
            }
        event.consume();
    }

    private void onDragExited(DragEvent event) {
        setStyle("");
        getTreeView().setStyle("");
        setOpacity(1.0);
        event.consume();
    }

    private void onDragDropped(DragEvent event) {
        Dragboard dragBoard = event.getDragboard();
        if (dragBoard.hasContent(TRACK_IDS_MIME_TYPE) && isValidTracksDragDropped()) {
            List<Integer> selectedTracksIds = (List<Integer>) dragBoard.getContent(TRACK_IDS_MIME_TYPE);
            getItem().addTracks(selectedTracksIds);
        }
        else
            if (dragBoard.hasContent(PLAYLIST_NAME_MIME_TYPE) && getItem().isFolder()) {
                String playlistName = (String) dragBoard.getContent(PLAYLIST_NAME_MIME_TYPE);
                ((PlaylistTreeView) getTreeView()).movePlaylist(playlistName, getItem());
            }
        event.consume();
    }

    private void onDragDetected(MouseEvent event) {
        if (getItem() != null) {
            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
            dragboard.setDragView(snapshot(null, null));
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.put(PLAYLIST_NAME_MIME_TYPE, getItem().getName());
            dragboard.setContent(clipboardContent);
        }
        event.consume();
    }

    /**
     * @return {@code True} if this playlist is not a folder, and if it's
     *          not the selected one. {@code False} otherwise.
     */
    private boolean isValidTracksDragDropped() {
        Playlist treeCellPlaylist = getItem();
        boolean validDrag = treeCellPlaylist != null &&! treeCellPlaylist.isFolder();
        if (validDrag) {
            PlaylistTreeView playlistTreeView = (PlaylistTreeView) getTreeView();
            Optional<Playlist> selectedPlaylist = playlistTreeView.selectedPlaylistProperty().get();
            validDrag = ! (selectedPlaylist.isPresent() && selectedPlaylist.get().equals(treeCellPlaylist));
        }
        return validDrag;
    }
}
