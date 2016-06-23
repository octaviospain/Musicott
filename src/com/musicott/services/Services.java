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

package com.musicott.services;

import com.musicott.model.*;
import com.musicott.tasks.*;

import java.util.prefs.*;

/**
 * @author Octavio Calleya
 *
 */
public class Services {

	private static Services instance;
	private ServicesPreferences servicesPreferences;
	private LastFMTask lastfmTask;
	
	private Services() {
		servicesPreferences = new ServicesPreferences();
	}
	
	public static Services getInstance() {
		if(instance == null)
			instance = new Services();
		return instance;
	}
	
	public String getLastFMUsername() {
		return servicesPreferences.getLastFMUsername();
	}
	
	public String getLastFMPassword() {
		return servicesPreferences.getLastFMUsername();
	}
	
	public ServicesPreferences getServicesPreferences() {
		return servicesPreferences;
	}
	
	public void lastFMLogIn(String username, String password) {
		servicesPreferences.setLasFMUsername(username);
		servicesPreferences.setLasFMPassword(password);
		lastFMLogin();
	}
	
	public void lastFMLogOut() {
		servicesPreferences.deleteLastFMUserData();
	}
	
	public void udpateAndScrobbleLastFM(Track track) {
		if(usingLastFM()) {
			if(lastfmTask == null || lastfmTask.isInterrupted()) {
				lastfmTask = new LastFMTask("LastFM Thread");
				lastfmTask.start();
			}
			lastfmTask.updateAndScrobble(track);
		}
	}

	public boolean usingLastFM() {
		String lastFMUsername = servicesPreferences.getLastFMUsername();
		String lastFMPassword = servicesPreferences.getLastFMPassword();
		return lastFMUsername != null && lastFMPassword != null;
	}
	
	private void lastFMLogin() {
		if(lastfmTask != null && lastfmTask.isAlive())
			lastfmTask.interrupt();
		lastfmTask = new LastFMTask("LastFM Thread");
		lastfmTask.loginBefore();
		lastfmTask.start();
	}
	
	public class ServicesPreferences {

		private final String LASTFM_API_KEY = "lastfm_api_key";
		private final String LASTFM_API_SECRET = "lastfm_api_secret";
		private final String LASTFM_SESSION_KEY = "lastfm_session_key";
		private final String LASTFM_USERNAME = "lastfm_username";
		private final String LASTFM_PASSWORD = "lastfm_password";
		
		private Preferences preferences;
		
		private ServicesPreferences() {
			preferences = Preferences.userNodeForPackage(getClass());
		}
		
		public void setAPIKey(String api) {
			preferences.put(LASTFM_API_KEY, api);
		}
		
		public String getAPIKey() {
			return preferences.get(LASTFM_API_KEY, null);
		}
		
		public void setAPISecret(String secret) {
			preferences.put(LASTFM_API_SECRET, secret);
		}
		
		public String getAPISecret() {
			return preferences.get(LASTFM_API_SECRET, null);
		}
		
		protected void setLastFMSessionkey(String sessionKey) {
			preferences.put(LASTFM_SESSION_KEY, sessionKey);
		}

		protected String getLastFMSessionKey() {
			return preferences.get(LASTFM_SESSION_KEY, null);
		}
		
		protected void setLasFMUsername(String username) {
			preferences.put(LASTFM_USERNAME, username);
		}

		protected String getLastFMUsername() {
			return preferences.get(LASTFM_USERNAME, null);
		}
		
		protected void setLasFMPassword(String password) {
			preferences.put(LASTFM_PASSWORD, password);
		}

		protected String getLastFMPassword() {
			return preferences.get(LASTFM_PASSWORD, null);
		}
		
		protected void deleteLastFMUserData() {
			preferences.remove(LASTFM_USERNAME);
			preferences.remove(LASTFM_PASSWORD);
			preferences.remove(LASTFM_SESSION_KEY);
		} 
	}
}
