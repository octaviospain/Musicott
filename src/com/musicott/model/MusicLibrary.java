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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.collections.ObservableList;

/**
 * @author Octavio Calleya
 *
 */
public class MusicLibrary {

	private static volatile MusicLibrary instance;
	private ObservableList<Track> tracks;
	private Map<Integer,float[]> waveforms;
	private AtomicInteger trackSequenceID;
	
	private MusicLibrary() {
	}
	
	public static MusicLibrary getInstance() {
		if(instance == null)
			instance = new MusicLibrary();
		return instance;
	}
	
	public void setTracks(ObservableList<Track> tracks) {
		this.tracks = tracks;
	}
	
	public ObservableList<Track> getTracks() {
		return this.tracks;
	}
	
	public void setWaveforms(Map<Integer,float[]> waveforms) {
		this.waveforms = waveforms;
	}
	
	public Map<Integer,float[]> getWaveforms() {
		return this.waveforms;
	}
	
	public void setTrackSequence(AtomicInteger trackSequence) {
		trackSequenceID = trackSequence;
	}
	
	public AtomicInteger getTrackSequence() {
		return trackSequenceID;
	}
	
	public int hashCode() {
		int hash = 71;
		hash = 73*hash + tracks.hashCode();
		hash = 73*hash + waveforms.hashCode();
		hash = 73*hash + trackSequenceID.hashCode();
		return hash;
	}
	
	public boolean equals(Object o) {
		boolean res;
		if(o instanceof MusicLibrary &&
		   ((MusicLibrary)o).getTracks().equals(tracks) &&
		   ((MusicLibrary)o).getWaveforms().equals(waveforms) &&
		   ((MusicLibrary)o).getTrackSequence().equals(trackSequenceID))
			res = true;
		else
			res = false;
		return res;
	}
}