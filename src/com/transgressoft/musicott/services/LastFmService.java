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
import com.sun.jersey.api.client.*;
import com.sun.jersey.core.util.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.services.lastfm.*;
import org.slf4j.*;

import javax.ws.rs.core.*;
import java.security.*;
import java.util.*;
import java.util.Map.*;

import static javax.ws.rs.HttpMethod.*;

/**
 * Performs the usage of the LastFM API.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @see <a href="http://www.last.fm/es/api/scrobbling">LastFM API documentation</a>
 */
public class LastFmService {

    private static final String API_ROOT_URL = "https://ws.audioscrobbler.com/2.0/";
    private static final String OK = "ok";
    private static final String FAILED = "failed";
    private static final String UPDATE_PLAYING = "track.updateNowPlaying";
    private static final String TRACK_SCROBBLE = "track.scrobble";
    private static final String MOBILE_SESSION = "auth.getMobileSession";

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final LastFmPreferences lastFmPreferences;
    private final ErrorDemon errorDemon;

    /**
     * The API Key for the application submitted in LastFM.
     * Retrieved from config file for security reasons
     */
    private final String API_KEY;

    /**
     * The API Secret for the application submitted in LastFM.
     * Retrieved from config file for security reasons
     */
    private final String API_SECRET;

    private final String USERNAME;
    private final String PASSWORD;

    private String sessionKey;
    private WebResource resource;

    @Inject
    public LastFmService(LastFmPreferences lastFmPreferences, ErrorDemon errorDemon) {
        this.errorDemon = errorDemon;
        this.lastFmPreferences = lastFmPreferences;
        Client client = Client.create();
        resource = client.resource(API_ROOT_URL);
        USERNAME = this.lastFmPreferences.getLastFmUsername();
        PASSWORD = this.lastFmPreferences.getLastFmPassword();
        sessionKey = this.lastFmPreferences.getLastFmSessionKey();
        API_KEY = this.lastFmPreferences.getApiKey();
        API_SECRET = this.lastFmPreferences.getApiSecret();
    }

    public boolean isApiConfigurationPresent() {
        return API_KEY != null && API_SECRET != null;
    }

    public LastFmResponse updateNowPlaying(Track track) {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("artist", track.getArtist());
        queryParams.add("track", track.getName());
        addApiParams(queryParams, UPDATE_PLAYING);
        return makeRequest(queryParams, POST);
    }

    private void addApiParams(MultivaluedMap<String, String> queryParams, String apiMethod) {
        queryParams.add("method", apiMethod);
        queryParams.add("api_key", API_KEY);
        queryParams.add("sk", sessionKey);
        queryParams.add("password", PASSWORD);
        queryParams.add("username", USERNAME);
        queryParams.add("api_sig", buildSignature(queryParams));
    }

    private LastFmResponse makeRequest(MultivaluedMap<String, String> params, String httpMethod) {
        LastFmResponse lfmResponse = null;
        ClientResponse clientResponse = null;
        try {
            if (httpMethod.equals(GET))
                clientResponse = resource.queryParams(params).get(ClientResponse.class);
            else if (httpMethod.equals(POST))
                clientResponse = resource.queryParams(params).post(ClientResponse.class);

            if (clientResponse != null) {
                int clientStatus = clientResponse.getStatus();
                LOG.debug("LastFM API {} petition status: {}", httpMethod, clientStatus);
                lfmResponse = clientResponse.getEntity(LastFmResponse.class);

                if (lfmResponse == null) {
                    String statusString = Integer.toString(clientResponse.getStatus());
                    String errorMessage = "LastFM " + httpMethod + " petition error " + statusString;
                    lfmResponse = buildLastFmErrorResponse(statusString, errorMessage);
                }
            }
        }
        catch (RuntimeException exception) {
            lfmResponse = buildLastFmErrorResponse("U1", exception.getMessage());
            LOG.warn("LastFM API " + httpMethod + " petition failed: {}", exception);
        }
        finally {
            if (clientResponse != null)
                clientResponse.close();
        }
        return lfmResponse;
    }

    private String buildSignature(MultivaluedMap<String, String> params) {
        StringBuilder stringBuilder = new StringBuilder();
        Set<String> sortedParams = new TreeSet<>(params.keySet());
        for (String key : sortedParams)
            stringBuilder.append(key).append(params.getFirst(key));
        stringBuilder.append(API_SECRET);
        return getMd5Hash(stringBuilder.toString());
    }

    private LastFmResponse buildLastFmErrorResponse(String status, String message) {
        LastFmError lfmError = new LastFmError();
        lfmError.setCode(status);
        lfmError.setMessage(message);

        LastFmResponse lfmResponse = new LastFmResponse();
        lfmResponse.setStatus(FAILED);
        lfmResponse.setError(lfmError);
        return lfmResponse;
    }

    private String getMd5Hash(String message) {
        String md5 = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] array = messageDigest.digest(message.getBytes());
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < array.length; i++)
                stringBuilder.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            md5 = stringBuilder.toString();
        }
        catch (NoSuchAlgorithmException exception) {
            LOG.warn("Error creating MD5 hash", exception);
            errorDemon.showLastFmErrorDialog("Error creating MD5 hash", exception.getMessage());
        }
        return md5;
    }

    public LastFmResponse scrobbleTrack(Track track) {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("artist", track.getArtist());
        queryParams.add("track", track.getName());
        queryParams.add("timestamp", Long.toString(System.currentTimeMillis() / 1000));
        addApiParams(queryParams, TRACK_SCROBBLE);
        return makeRequest(queryParams, POST);
    }

    public LastFmResponse scrobbleTrackBatch(Map<Integer, Track> trackBatch) {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        int i = 0;
        for (Entry<Integer, Track> entry : trackBatch.entrySet()) {
            int timeStamp = entry.getKey();
            Track track = entry.getValue();
            queryParams.add("artist[" + i + "]", track.getArtist());
            queryParams.add("track[" + i + "]", track.getName());
            queryParams.add("timestamp[" + i + "]", Integer.toString(timeStamp));
            i++;
        }
        addApiParams(queryParams, TRACK_SCROBBLE);
        return makeRequest(queryParams, POST);
    }

    public LastFmResponse getSession() {
        LastFmResponse lfm;
        if (sessionKey == null) {
            MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
            queryParams.add("method", MOBILE_SESSION);
            queryParams.add("api_key", API_KEY);
            queryParams.add("password", PASSWORD);
            queryParams.add("username", USERNAME);
            queryParams.add("api_sig", buildSignature(queryParams));
            lfm = makeRequest(queryParams, POST);
            if (lfm != null && OK.equals(lfm.getStatus())) {
                sessionKey = lfm.getSession().getSessionKey();
                lastFmPreferences.setLastFmSessionKey(sessionKey);
            }
        }
        else {
            lfm = new LastFmResponse();
            lfm.setStatus(OK);
        }
        return lfm;
    }
}
