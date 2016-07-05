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

package com.musicott.model;

import com.musicott.view.*;
import javafx.beans.property.*;
import javafx.scene.image.*;

import java.io.*;
import java.util.*;

/**
 * @author Octavio Calleya
 *
 */
public class Playlist {

	private final Image COVER_IMAGE = new Image(getClass().getResourceAsStream(MusicottController.DEFAULT_COVER_IMAGE));

	private String name;
	private List<Integer> tracksID;
	private List<Integer> containedTracksID;
	private List<Playlist> containedPlaylists;
	private final boolean isFolder;
	
	private StringProperty nameProperty;
	private ObjectProperty<Image> playlistCoverProperty;
	private BooleanProperty folderProperty;

	private MusicLibrary musicLibrary =  MusicLibrary.getInstance();
	
	public Playlist(String name, boolean isFolder) {
		this.name = name;
		this.isFolder = isFolder;
		tracksID = new ArrayList<>();
		containedPlaylists = new ArrayList<>();
		nameProperty = new SimpleStringProperty(this.name);
		nameProperty.addListener((obs, oldName, newName) -> setName(newName));
		playlistCoverProperty = new SimpleObjectProperty<>(COVER_IMAGE);
		folderProperty = new SimpleBooleanProperty(this.isFolder);
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
		nameProperty.setValue(this.name);
	}

	public void setTracks(List<Integer> tracks) {
		tracksID = tracks;
	}

	public List<Integer> getTracks() {
		List<Integer> tracksInContainedPlaylists;
		if(!isFolder)
			tracksInContainedPlaylists = tracksID;
		else {
			if(containedTracksID == null)
				containedTracksID = new ArrayList<>();
			else
				containedTracksID.clear();
			for(Playlist playlist: containedPlaylists)
				containedTracksID.addAll(playlist.getTracks());
			tracksInContainedPlaylists = containedTracksID;
		}
		return tracksInContainedPlaylists;
	}
	
	public List<Playlist> getContainedPlaylists() {
		return containedPlaylists;
	}


	public StringProperty nameProperty() {
		return nameProperty;
	}

	public ObjectProperty<Image> playlistCoverProperty() {
		if(playlistCoverProperty.get().equals(COVER_IMAGE) && !tracksID.isEmpty())
			changePlaylistCover();
		return playlistCoverProperty;
	}

	public BooleanProperty folderProperty() {
		return folderProperty;
	}

	public void addPlaylistChild(Playlist playlistChild) {
		containedPlaylists.add(playlistChild);
	}

	public boolean isFolder() {
		return isFolder;
	}

	public void changePlaylistCover() {
		if(!getTracks().isEmpty()) {
			Random random = new Random();
			Optional<Track> randomTrack = musicLibrary.getTrack(getTracks().get(random.nextInt(tracksID.size())));
			randomTrack.ifPresent(track -> {
				if(track.hasCover())
					playlistCoverProperty.set(new Image(new ByteArrayInputStream(track.getCoverBytes())));
				else
					playlistCoverProperty.set(COVER_IMAGE);
			});
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
		if((o instanceof Playlist) && ((Playlist) o).getName().equals(this.name))
			equals = true;
		return equals;
	}
	
	@Override
	public String toString() {
		return name + "[" + tracksID.size() + "]";
	}
}
