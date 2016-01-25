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

import java.util.ArrayList;
import java.util.List;

import com.musicott.MainPreferences;

/**
 * @author Octavio Calleya
 *
 */
public class Playlist {

	private int playlistID;
	private String name;
	private List<Integer> tracksID;
	
	public Playlist(String name) {
		this.name = name;
		playlistID = MainPreferences.getInstance().getPlaylistSequence();
		tracksID = new ArrayList<>();
	}
	
	public int getPlaylistID() {
		return this.playlistID;
	}
	
	public void setPlaylistId(int playlistID) {
		this.playlistID = playlistID;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<Integer> getTracks() {
		return this.tracksID;
	}
	
	public void setTracks(List<Integer> tracks) {
		tracksID = tracks;
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