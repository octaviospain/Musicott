package com.musicott.view.custom;

import com.musicott.model.*;
import javafx.beans.value.*;
import javafx.css.*;
import javafx.scene.control.*;

/**
 * Custom {@link TreeCell} that isolates the style of his {@link Playlist}
 * managed by pseudo classes
 *
 * @author Octavio Calleya
 * @version 0.9
 * @since 0.9
 */
public class PlaylistTreeCell extends TreeCell<Playlist> {

	private PseudoClass playlist = PseudoClass.getPseudoClass("playlist");
	private PseudoClass playlistSelected = PseudoClass.getPseudoClass("playlist-selected");
	private PseudoClass folder = PseudoClass.getPseudoClass("folder");
	private PseudoClass folderSelected = PseudoClass.getPseudoClass("folder-selected");

	public PlaylistTreeCell() {
		super();

		ChangeListener<Boolean> isFolderListener = (obs, oldPlaylist, newPlaylist) -> {
			boolean isFolder = newPlaylist.booleanValue();
			boolean isSelected = selectedProperty().get();
			updatePseudoClassesStates(isFolder, isSelected);
		};

		ChangeListener<Boolean> isSelectedListener = (obs, oldValue, newValue) -> {
			boolean isFolder = itemProperty().getValue().isFolder();
			boolean isSelected = newValue.booleanValue();
			updatePseudoClassesStates(isFolder, isSelected);
		};

		itemProperty().addListener((obs, oldPlaylist, newPlaylist) -> {

			if(oldPlaylist != null) {
				textProperty().unbind();
				setText("");
				oldPlaylist.folderProperty().removeListener(isFolderListener);
				selectedProperty().removeListener(isSelectedListener);
			}

			if(newPlaylist != null) {
				textProperty().bind(newPlaylist.nameProperty());
				newPlaylist.folderProperty().addListener(isFolderListener);
				selectedProperty().addListener(isSelectedListener);

				updatePseudoClassesStates(newPlaylist.isFolder(), selectedProperty().get());
			}
			else
				disablePseudoClassesStates();
		});
	}

	private void updatePseudoClassesStates(boolean isFolder, boolean isSelected) {
		pseudoClassStateChanged(folder, isFolder && !isSelected);
		pseudoClassStateChanged(folderSelected, isFolder && isSelected);
		pseudoClassStateChanged(playlist, !isFolder && !isSelected);
		pseudoClassStateChanged(playlistSelected, !isFolder && isSelected);
	}

	private void disablePseudoClassesStates() {
		pseudoClassStateChanged(folder, false);
		pseudoClassStateChanged(folderSelected, false);
		pseudoClassStateChanged(playlist, false);
		pseudoClassStateChanged(playlistSelected, false);
	}
}
