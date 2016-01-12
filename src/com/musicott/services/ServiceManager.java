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
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.musicott.MainPreferences;
import com.musicott.model.Track;
import com.musicott.task.LastFMTask;

/**
 * @author Octavio Calleya
 *
 */
public class ServiceManager {

	private static ServiceManager instance;
	private LastFMTask lastfmTask;
	private boolean lastFMOff;
	
	private ServiceManager() {
		List<Map<Track, Integer>> tracksToScrobbleLater = new ArrayList<>();
		tracksToScrobbleLater.add(new HashMap<>());
		lastFMOff = false;
	}
	
	public static ServiceManager getInstance() {
		if(instance == null)
			instance = new ServiceManager();
		return instance;
	}
	
	public void udpateAndScrobbleLastFM(Track track) {
		if(lastfmTask == null) {
			lastfmTask = new LastFMTask("LastFM Thread", this);
			lastfmTask.start();
		}
		lastfmTask.updateAndScrobble(track);
	}
	
	public void endLastfmUsage() {
		lastFMOff = true;
	}
	
	public boolean usingLastFM() {
		String lastFMUsername = MainPreferences.getInstance().getLastFMUsername();
		String lastFMPassword = MainPreferences.getInstance().getLastFMPassword();
		return !lastFMOff && lastFMUsername != null && lastFMPassword != null;
	}
}