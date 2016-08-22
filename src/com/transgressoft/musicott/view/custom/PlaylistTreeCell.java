package com.transgressoft.musicott.view.custom;

import com.transgressoft.musicott.model.*;
import javafx.beans.value.*;
import javafx.css.*;
import javafx.scene.control.*;
import javafx.scene.input.*;

import java.util.*;

import static com.transgressoft.musicott.view.custom.TrackTableRow.*;

/**
 * Custom {@link TreeCell} that isolates the style of his {@link Playlist}
 * managed by pseudo classes
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 * @since 0.9
 */
public class PlaylistTreeCell extends TreeCell<Playlist> {

	private PseudoClass playlist = PseudoClass.getPseudoClass("playlist");
	private PseudoClass playlistSelected = PseudoClass.getPseudoClass("playlist-selected");
	private PseudoClass folder = PseudoClass.getPseudoClass("folder");
	private PseudoClass folderSelected = PseudoClass.getPseudoClass("folder-selected");

	private TreeView<Playlist> playlistTreeView;

	public PlaylistTreeCell(TreeView<Playlist> playlistTreeView) {
		super();
		this.playlistTreeView = playlistTreeView;

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
		setOnDragDropped(this::onDragOverPlaylist);
	}

	private void onDragOverPlaylist(DragEvent event) {
		Playlist treeCellPlaylist = getItem();
		if (! treeCellPlaylist.isFolder()) {
			Dragboard dragBoard = event.getDragboard();
			List<Integer> selectedTracks = (List<Integer>) dragBoard.getContent(TRACK_ID_MIME_TYPE);
			treeCellPlaylist.addTracks(selectedTracks);
			event.consume();
		}
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
}
