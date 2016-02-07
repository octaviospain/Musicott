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
 */

package com.musicott.model;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

/**
 * @author Octavio Calleya
 *
 */
public class Playlist {

	private String name;
	private List<Integer> tracksID;
	
	private StringProperty nameProperty;
	private ObjectProperty<Image> playlistCoverProperty;
	
	private Image DEFAULT_COVER_IMAGE = new Image(getClass().getResourceAsStream("/images/default-cover-image.png"));
	
	public Playlist(String name) {
		this.name = name;
		tracksID = new ArrayList<>();
		nameProperty = new SimpleStringProperty(this.name);
		nameProperty.addListener((obs, oldVal, newVal) -> setName(newVal));
		playlistCoverProperty = new SimpleObjectProperty<Image>(DEFAULT_COVER_IMAGE);
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
		nameProperty.setValue(this.name);
	}
	
	public List<Integer> getTracks() {
		return this.tracksID;
	}
	
	public void setTracks(List<Integer> tracks) {
		tracksID = tracks;
	}
	
	public void changePlaylistCover() {
		if(!tracksID.isEmpty()) {
			Random r = new Random();
			Track rt = MusicLibrary.getInstance().getTrack(tracksID.get(r.nextInt(tracksID.size())));
			if(rt.hasCover())
				playlistCoverProperty.set(new Image(new ByteArrayInputStream(rt.getCoverBytes())));
			else
				playlistCoverProperty.set(DEFAULT_COVER_IMAGE);
		}
		else
			playlistCoverProperty.set(DEFAULT_COVER_IMAGE);
	}
	
	public StringProperty nameProperty() {
		return nameProperty;
	}
	
	public ObjectProperty<Image> playlistCoverProperty() {
		if(playlistCoverProperty.get().equals(DEFAULT_COVER_IMAGE) && !tracksID.isEmpty())
			changePlaylistCover();
		return playlistCoverProperty;
	}
	
	@Override
	public int hashCode() {
		int hash = 71;
		hash = 73*hash + name.hashCode();		
		return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		boolean equals = false;
		if((o instanceof Playlist) && ((Playlist)o).getName().equals(this.name))
			equals = true;
		return equals;
	}
	
	@Override
	public String toString() {
		return name+"["+tracksID.size()+"]";
	}
}