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

import com.musicott.services.lastfm.LastFMApi;

/**
 * @author Octavio Calleya
 *
 */
public class ServiceManager {

	private static ServiceManager instance;
	private LastFMApi lastfm;
	
	private ServiceManager() {
	}
	
	public static ServiceManager getInstance() {
		if(instance == null)
			instance = new ServiceManager();
		return instance;
	}
	
	public void updateLastFMNowPlaying() {
		if(lastfm == null)
			initLastFM();
		
	}
	
	private void initLastFM() {
		lastfm = new LastFMApi();
		if(!lastfm.authenticate())
			;
	}
}