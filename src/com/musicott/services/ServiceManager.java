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

package com.musicott.services;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.musicott.MainPreferences;
import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class ServiceManager {

	private static ServiceManager instance;
	private LastFMService lastfm;
	private List<Map<Track, Integer>> tracksToScrobbleLater;
	
	private ServiceManager() {
		List<Map<Track, Integer>> tracksToScrobbleLater = new ArrayList<>();
		tracksToScrobbleLater.add(new HashMap<>());
	}
	
	public static ServiceManager getInstance() {
		if(instance == null)
			instance = new ServiceManager();
		return instance;
	}
	
	public boolean scrobbleLastFMTrack(Track track) {
		boolean res = false;
		if(usingLastFM()) {
			if(lastfm == null)
				lastfm = new LastFMService();
			if(lastfm.isAuthenticated() && lastfm.scrobble(track)) {
				res = true;
			}
		}
		return res;
	}
	
	public boolean udpateLastFMNowPlaying(Track track) {
		boolean res = false;
		if(usingLastFM()) {
			if(lastfm == null)
				lastfm = new LastFMService();
			if(lastfm.isAuthenticated() && lastfm.updateNowPlaying(track))
				res = true;
		}
		return res;
	}
	
	public void addTrackToScrobbleLater(Track track) {
		if(!tracksToScrobbleLater.isEmpty()) {
			ListIterator<Map<Track, Integer>> scrobblesIt = tracksToScrobbleLater.listIterator();
			boolean done = false;
			while(scrobblesIt.hasNext() && !done) {
				Map<Track, Integer> mapBatch = scrobblesIt.next();
				if(mapBatch.size() < 50) {
					mapBatch.put(track, (int)System.currentTimeMillis()/1000);
					done = true;
				}
				else if (scrobblesIt.nextIndex() == mapBatch.size()) {
					Map<Track, Integer> newMapBatch = new HashMap<>();
					newMapBatch.put(track, (int)System.currentTimeMillis()/1000);
					scrobblesIt.add(newMapBatch);
					done = true;
				}
			}
		}
	}
	
	private boolean usingLastFM() {
		String lastFMUsername = MainPreferences.getInstance().getLastFMUsername();
		String lastFMPassword = MainPreferences.getInstance().getLastFMPassword();
		return lastFMUsername != null && lastFMPassword != null;
	}
}