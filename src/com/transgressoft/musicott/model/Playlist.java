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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.transgressoft.musicott.model;

import com.transgressoft.musicott.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.image.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.view.MusicottController.*;

/**
 * Represents a Playlist of tracks. A <tt>Playlist</tt> can contain another playlists,
 * if so, an instance is a folder. One cover image of the contained tracks
 * is used when showing a {@link Playlist} on the application.
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 */
public class Playlist {

	private static final String ADDITION_NOT_SUPPORTED = "Addition not supported on folder playlist";
	private static final String DELETION_NOT_SUPPORTED = "Deletion not supported on folder playlist";

	private final Image COVER_IMAGE = new Image(getClass().getResourceAsStream(DEFAULT_COVER_IMAGE));
	private final boolean isFolder;
	private String name;
	private ObservableList<Integer> playlistTrackIds;
	private List<Playlist> containedPlaylists;
	private StringProperty nameProperty;
	private ObjectProperty<Image> playlistCoverProperty;
	private BooleanProperty isFolderProperty;

	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	private StageDemon stageDemon = StageDemon.getInstance();

	public Playlist(String name, boolean isFolder) {
		this.name = name;
		this.isFolder = isFolder;
		playlistTrackIds = FXCollections.observableArrayList();
		containedPlaylists = new ArrayList<>();
		nameProperty = new SimpleStringProperty(this, "name", name);
		nameProperty.addListener((obs, oldName, newName) -> setName(newName));
		playlistCoverProperty = new SimpleObjectProperty<>(this, "cover", COVER_IMAGE);
		isFolderProperty = new SimpleBooleanProperty(this, "folder", isFolder);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		nameProperty.setValue(this.name);
	}

	public StringProperty nameProperty() {
		return nameProperty;
	}

	public ObjectProperty<Image> playlistCoverProperty() {
		ObjectProperty<Image> returnedCoverProperty = playlistCoverProperty;
		if (isFolder) {
			Optional<Playlist> childPlaylistNotEmpty = containedPlaylists.stream()
																		 .filter(playlist -> ! playlist.getTracks()
																									   .isEmpty())
																		 .findAny();
			if (childPlaylistNotEmpty.isPresent())
				returnedCoverProperty = childPlaylistNotEmpty.get().playlistCoverProperty();
			else
				returnedCoverProperty.set(COVER_IMAGE);
		}
		else if (playlistCoverProperty.get().equals(COVER_IMAGE) && ! getTracks().isEmpty())
			changePlaylistCover();
		return returnedCoverProperty;
	}

	public BooleanProperty isFolderProperty() {
		return isFolderProperty;
	}

	public boolean isFolder() {
		return isFolder;
	}

	public boolean isEmpty() {
		return getTracks().isEmpty();
	}

	public boolean addTracks(List<Integer> tracksIds) {
		if (isFolder)
			throw new UnsupportedOperationException(ADDITION_NOT_SUPPORTED);

		boolean result = playlistTrackIds.addAll(tracksIds);
		if (result) {
			changePlaylistCover();
			musicLibrary.saveLibrary(false, false, true);
		}

		Optional<Playlist> selectedPlaylist = stageDemon.getNavigationController().selectedPlaylistProperty().get();
		selectedPlaylist.ifPresent(playlist -> {
			if (playlist.equals(this)) {
				musicLibrary.addToShowingTracks(tracksIds);
			}
		});
		return result;
	}

	public boolean removeTracks(List<Integer> tracksIds) {
		if (isFolder)
			throw new UnsupportedOperationException(DELETION_NOT_SUPPORTED);

		boolean result = playlistTrackIds.removeAll(tracksIds);
		if (result) {
			changePlaylistCover();
			musicLibrary.saveLibrary(false, false, true);
		}

		Optional<Playlist> selectedPlaylist = stageDemon.getNavigationController().selectedPlaylistProperty().get();
		selectedPlaylist.ifPresent(playlist -> {
			if (playlist.equals(this))
				musicLibrary.removeFromShowingTracks(tracksIds);
		});
		return result;
	}

	public void showTracksOnTable() {
		musicLibrary.clearShowingTracks();

		List<Integer> tracks = getTracks();
		if (! tracks.isEmpty())
			musicLibrary.addToShowingTracks(tracks);
	}

	public List<Playlist> getContainedPlaylists() {
		return containedPlaylists;
	}

	private List<Integer> getTracks() {
		List<Integer> allTracksWithin;
		if (isFolder) {
			allTracksWithin = new ArrayList<>();
			containedPlaylists.forEach(playlist -> allTracksWithin.addAll(playlist.getTracks()));
		}
		else
			allTracksWithin = playlistTrackIds;
		return allTracksWithin;
	}

	/**
	 * Search in the contained tracks for one that has cover and sets it as the cove of the playlist.
	 * If there is no track or any of them has cover, the default cover is used.
	 */
	private void changePlaylistCover() {
		List<Integer> tracks = getTracks();
		if (! tracks.isEmpty()) {
			Optional<Integer> trackWithCover = tracks.stream()
													 .filter(trackId -> musicLibrary.getTrack(trackId).isPresent())
													 .filter(trackId -> musicLibrary.getTrack(trackId).get()
																					.getCoverImage().isPresent())
													 .findAny();

			if (trackWithCover.isPresent()) {
				int trackId = trackWithCover.get();
				musicLibrary.getTrack(trackId).ifPresent(track -> {
					byte[] coverBytes = track.getCoverImage().get();
					Image image = new Image(new ByteArrayInputStream(coverBytes));
					playlistCoverProperty.set(image);
				});
			}
			else
				playlistCoverProperty.set(COVER_IMAGE);
		}
		else
			playlistCoverProperty.set(COVER_IMAGE);
	}

	@Override
	public int hashCode() {
		int hash = 71;
		hash = 73 * hash + name.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object o) {
		boolean equals = false;
		if ((o instanceof Playlist) && ((Playlist) o).getName().equals(this.name)) {
			equals = true;
		}
		return equals;
	}

	@Override
	public String toString() {
		return name + "[" + playlistTrackIds.size() + "]";
	}
}
