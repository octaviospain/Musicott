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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.musicott.model;

import java.util.List;
import java.util.Map;

/**
 * @author Octavio Calleya
 *
 */
public class MusicLibrary {

	private static MusicLibrary instance;
	private List<Track> tracks;
	private Map<Track,float[]> waveforms;
	
	private MusicLibrary() {
	}
	
	public static MusicLibrary getInstance() {
		if(instance == null)
			instance = new MusicLibrary();
		return instance;
	}
	
	public void setTracks(List<Track> tracks) {
		this.tracks = tracks;
	}
	
	public List<Track> getTracks() {
		return this.tracks;
	}
	
	public void setWaveforms(Map<Track,float[]> waveforms) {
		this.waveforms = waveforms;
	}
	
	public Map<Track,float[]> getWaveforms() {
		return this.waveforms;
	}
	
	public int hashCode() {
		int hash = 71;
		hash = 73*hash + tracks.hashCode();
		hash = 73*hash + waveforms.hashCode();
		return hash;
	}
	
	public boolean equals(Object o) {
		boolean res;
		if(o instanceof MusicLibrary &&
		   ((MusicLibrary)o).getTracks().equals(tracks) &&
		   ((MusicLibrary)o).getWaveforms().equals(waveforms))
			res = true;
		else
			res = false;
		return res;
	}
}