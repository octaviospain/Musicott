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
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package com.transgressoft.musicott.services;

import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import javafx.application.*;
import javafx.beans.property.*;

/**
 * Singleton class that manages the connection and resource handling
 * from external services.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 */
@Singleton
public class ServiceDemon {


    private LastFmTask lastFmTask;
    private boolean usingLastFm;
    private BooleanProperty usingLastFmProperty;

    private LastFmPreferences lastFmPreferences;
    @Inject
    private Injector injector;

    @Inject
    public ServiceDemon() {
        usingLastFmProperty = new SimpleBooleanProperty(this, "using LastFM", false);
    }

    public void lastFmLogIn(String username, String password) {
        lastFmPreferences.setLastFmUsername(username);
        lastFmPreferences.setLasFmPassword(password);
        lastFmTask = injector.getInstance(LastFmTask.class);
        lastFmTask.start();
    }

    public void lastFmLogOut() {
        lastFmPreferences.deleteLastFmUserData();
        if (lastFmTask != null && lastFmTask.isAlive()) {
            lastFmTask.logout();
            setUsingLastFm(false);
        }
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

    @Inject
    public void setLastFmPreferences(LastFmPreferences lastFmPreferences) {
        this.lastFmPreferences = lastFmPreferences;
    }

    public ReadOnlyBooleanProperty usingLastFmProperty() {
        return usingLastFmProperty;
    }
}
