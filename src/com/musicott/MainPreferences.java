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

package com.musicott;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * @author Octavio Calleya
 *
 */
public class MainPreferences {

	private final String MUSICOTT_FOLDER = "musicott_folder";
	private final String TRACK_SEQUENCE = "track_sequence";
	private final String LASTFM_SESSION_KEY = "lastfm_session_key";
	private final String LASTFM_USERNAME = "lastfm_username";
	private final String LASTFM_PASSWORD = "lastfm_password";
	
	private static MainPreferences instance;
	private Preferences mainPreferences;
	
	private MainPreferences() {
		mainPreferences = Preferences.userNodeForPackage(getClass());
	}
	
	public static MainPreferences getInstance() {
		if(instance == null)
			instance = new MainPreferences();
		return instance;			
	}
	
	public int getTrackSequence() {
		int sequence = mainPreferences.getInt(TRACK_SEQUENCE, 0);
		mainPreferences.putInt(TRACK_SEQUENCE, ++sequence);
		return sequence;
	}
	
	public boolean setMusicottUserFolder(String path) {
		mainPreferences.put(MUSICOTT_FOLDER, path);
		mainPreferences.putInt(TRACK_SEQUENCE, 0);	// reset the sequence number
		return new File(path).mkdirs();
	}
	
	public String getMusicottUserFolder() {
		return mainPreferences.get(MUSICOTT_FOLDER, null);
	}
	
	public void setLastFMSessionkey(String sessionKey) {
		mainPreferences.put(LASTFM_SESSION_KEY, sessionKey);
	}
	
	public String getLastFMSessionKey() {
		return mainPreferences.get(LASTFM_SESSION_KEY, null);
	}
	
	public void setLasFMUsername(String username) {
		mainPreferences.put(LASTFM_USERNAME, username);
	}

	public String getLastFMUsername() {
		return mainPreferences.get(LASTFM_USERNAME, null);
	}
	
	public void deleteLastFMUsername() {
		mainPreferences.remove(LASTFM_USERNAME);
	}
	
	public void setLasFMPassword(String password) {
		mainPreferences.put(LASTFM_PASSWORD, password);
	}

	public String getLastFMPassword() {
		return mainPreferences.get(LASTFM_PASSWORD, null);
	}
	
	public void deleteLastFMPassword() {
		mainPreferences.remove(LASTFM_PASSWORD);
	}
}