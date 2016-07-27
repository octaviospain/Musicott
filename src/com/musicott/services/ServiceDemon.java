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
import com.musicott.services.lastfm.*;
import javafx.application.*;
import javafx.beans.property.*;

/**
 * Singleton class that manages the connection and resource handling
 * from external services.
 *
 * @author Octavio Calleya
 * @version 0.9
 */
public class ServiceDemon {

	private static ServiceDemon instance;

	private LastFmPreferences lastFmPreferences;
	private LastFmTask lastFmTask;
	private boolean usingLastFm;
	private BooleanProperty usingLastFmProperty;
	
	private ServiceDemon() {
		lastFmPreferences = new LastFmPreferences();
		usingLastFmProperty = new SimpleBooleanProperty(this, "using LastFM", false);
	}
	
	public static ServiceDemon getInstance() {
		if(instance == null)
			instance = new ServiceDemon();
		return instance;
	}

	public LastFmPreferences getLastFmPreferences() {
		return lastFmPreferences;
	}

	public void lastFmLogIn(String username, String password) {
		lastFmPreferences.setLastFmUsername(username);
		lastFmPreferences.setLasFmPassword(password);
		lastFmTask = new LastFmTask();
		lastFmTask.start();
	}

	public void lastFmLogOut() {
		lastFmPreferences.deleteLastFmUserData();
		if(lastFmTask != null && lastFmTask.isAlive())
			lastFmTask.interrupt();
	}
	
	public void updateAndScrobbleTrack(Track track) {
		lastFmTask.updateAndScrobble(track);
	}

	protected void setUsingLastFm(boolean usingLastFm) {
		this.usingLastFm = usingLastFm;
		Platform.runLater(() -> usingLastFmProperty.set(usingLastFm));
	}

	public boolean usingLastFm() {
		return usingLastFm;
	}

	public ReadOnlyBooleanProperty usingLastFmProperty() {
		return usingLastFmProperty;
	}
}