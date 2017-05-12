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

import java.util.prefs.*;

/**
 * Class that isolates and stores data associated with the LastFM external service.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.9
 */
public class LastFmPreferences {

    private static final String LASTFM_API_KEY = "lastfm_api_key";
    private static final String LASTFM_API_SECRET = "lastfm_api_secret";
    private static final String LASTFM_SESSION_KEY = "lastfm_session_key";
    private static final String LASTFM_USERNAME = "lastfm_username";
    private static final String LASTFM_PASSWORD = "lastfm_password";

    private Preferences preferences;

    @Inject
    public LastFmPreferences() {
        preferences = Preferences.userNodeForPackage(getClass());
    }

    public void setApiKey(String apiKey) {
        preferences.put(LASTFM_API_KEY, apiKey);
    }

    public String getApiKey() {
        return preferences.get(LASTFM_API_KEY, null);
    }

    public void setApiSecret(String apiSecret) {
        preferences.put(LASTFM_API_SECRET, apiSecret);
    }

    public String getApiSecret() {
        return preferences.get(LASTFM_API_SECRET, null);
    }

    public void setLastFmSessionKey(String sessionKey) {
        preferences.put(LASTFM_SESSION_KEY, sessionKey);
    }

    public String getLastFmSessionKey() {
        return preferences.get(LASTFM_SESSION_KEY, null);
    }

    public void setLastFmUsername(String username) {
        preferences.put(LASTFM_USERNAME, username);
    }

    public String getLastFmUsername() {
        return preferences.get(LASTFM_USERNAME, null);
    }

    public void setLasFmPassword(String password) {
        preferences.put(LASTFM_PASSWORD, password);
    }

    public String getLastFmPassword() {
        return preferences.get(LASTFM_PASSWORD, null);
    }

    public void deleteLastFmUserData() {
        preferences.remove(LASTFM_USERNAME);
        preferences.remove(LASTFM_PASSWORD);
        preferences.remove(LASTFM_SESSION_KEY);
    }
}
