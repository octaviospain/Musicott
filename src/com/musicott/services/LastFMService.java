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

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.MainPreferences;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.error.LastFMException;
import com.musicott.model.Track;
import com.musicott.services.lastfm.LastFMError;
import com.musicott.services.lastfm.LastFMResponse;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author Octavio Calleya
 *
 */
public class LastFMService {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	/**
	 * The API Key for the application submitted in LastFM.
	 * Retrieved from config file for security reasons
	 */
	private String API_KEY;
	/**
	 * The API Secret for the application submitted in LastFM.
	 * Retrieved from config file for security reasons
	 */
	private String API_SECRET;
	private final String CONFIG_FILE = "resources/config/config.properties";
	private final String API_ROOT_URL = "https://ws.audioscrobbler.com/2.0/";
	private final String USERNAME;
	private final String PASSWORD;
	private ErrorHandler eh;
	private String sessionKey;
	private Client client;
	private WebResource resource;
	
	public LastFMService() {
		eh = ErrorHandler.getInstance();
		client = Client.create();
		resource = client.resource(API_ROOT_URL);
		USERNAME = MainPreferences.getInstance().getLastFMUsername();
		PASSWORD = MainPreferences.getInstance().getLastFMPassword();
		sessionKey = MainPreferences.getInstance().getLastFMSessionKey();
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(CONFIG_FILE));
			API_KEY = prop.getProperty("lastfm_api_key");
			API_SECRET = prop.getProperty("lastfm_api_secret");
		} catch (IOException e) {
			treatException("Error reading LastFM API properties", e);
		}
	}
	
	public boolean updateNowPlaying(Track track) {
		boolean res = true;
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("artist", track.getArtist());
        queryParams.add("track", track.getName());
        queryParams.add("sk", sessionKey);
        queryParams.add("method", "track.updateNowPlaying");
        queryParams.add("api_key", API_KEY);
        queryParams.add("sk", sessionKey);
        queryParams.add("password", PASSWORD);
        queryParams.add("username", USERNAME);
        queryParams.add("api_sig", buildSignature(queryParams));
        LastFMResponse lfm = makeRequest(queryParams, HttpMethod.POST);
    	if(lfm.getStatus() == "failed") {
    		treatException("LastFM API error "+lfm.getError().getMessage()+" status "+lfm.getError().getCode(), null);
    		res = false;
    	}
		return res;
	}
	
	public boolean scrobble(Track track) {
		boolean res = true;
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		queryParams.add("artist", track.getArtist());
		queryParams.add("track", track.getName());
		queryParams.add("timestamp", ""+System.currentTimeMillis()/1000);
        queryParams.add("method", "track.scrobble");
        queryParams.add("api_key", API_KEY);
        queryParams.add("sk", sessionKey);
        queryParams.add("password", PASSWORD);
        queryParams.add("username", USERNAME);
        queryParams.add("api_sig", buildSignature(queryParams));
    	LastFMResponse lfm = makeRequest(queryParams, HttpMethod.POST);
    	if(lfm.getStatus() == "failed") {
    		treatException("LastFM API error "+lfm.getError().getMessage()+" status "+lfm.getError().getCode(), null);
    		res = false;
    	}
		return res;
	}
	
	public void scrobbleBatches(List<Map<Track, Integer>> trackBatches) {
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		LastFMResponse lfm = null;
		for(Map<Track, Integer> mapBatch: trackBatches) {
			int i = 0;
			for(Track t: mapBatch.keySet()) {
				queryParams.add("artist["+i+"]", t.getArtist());
				queryParams.add("track"+i+"]", t.getName());
				queryParams.add("timestamp"+i+"]", ""+mapBatch.get(t));
				i++;
			}
	        queryParams.add("method", "track.scrobble");
	        queryParams.add("api_key", API_KEY);
	        queryParams.add("sk", sessionKey);
	        queryParams.add("password", PASSWORD);
	        queryParams.add("username", USERNAME);
	        queryParams.add("api_sig", buildSignature(queryParams));
	    	lfm = makeRequest(queryParams, HttpMethod.POST);
		}
    	if(lfm.getStatus() == "failed")
    		treatException("LastFM API error "+lfm.getError().getMessage()+" status "+lfm.getError().getCode(), null);
	}
	
	public boolean isAuthenticated() {
		boolean res = sessionKey == null ? res = fetchSession() : true;
		return res && API_KEY != null && API_SECRET != null;
	}
	
	private LastFMResponse makeRequest(MultivaluedMap<String, String> params, String method) {
		LastFMResponse lfmResponse = null;
    	ClientResponse response = null;
        try {
        	if(method.equals(HttpMethod.GET))
	        		response = resource.queryParams(params).get(ClientResponse.class);
	        else if(method.equals(HttpMethod.POST))
	        		response = resource.queryParams(params).post(ClientResponse.class);			
	     	LOG.debug("LastFM API GET petition status: {}", response.getStatus());
	     	lfmResponse = response.getEntity(LastFMResponse.class);
        } catch (RuntimeException e) {
        		treatException("LastFM unknown error:", e);
        } finally {
	     	response.close();
        }
        if(lfmResponse == null) {
        	LastFMError lastFMError = new LastFMError();
        	lfmResponse = new LastFMResponse();
        	lastFMError.setCode(""+response.getStatus());
        	lastFMError.setMessage("LastFM "+method+" petition error "+response.getStatus());
        	lfmResponse.setStatus("failed");
        	lfmResponse.setError(lastFMError);
        }
		return lfmResponse;
	}
	
	private boolean fetchSession() {
		boolean res = true;
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("method", "auth.getMobileSession");
        queryParams.add("api_key", API_KEY);
        queryParams.add("password", PASSWORD);
        queryParams.add("username", USERNAME);
        queryParams.add("api_sig", buildSignature(queryParams));
        LastFMResponse lfm = makeRequest(queryParams, HttpMethod.POST);
        if(lfm == null)
        	res = false;
        else {
        	if(lfm.getStatus().equals("ok")) {
	        	sessionKey = lfm.getSession().getSessionKey();
				if(sessionKey != null)
					MainPreferences.getInstance().setLastFMSessionkey(sessionKey);
				else {
					treatException("LastFM API Session not fetched: null", null);
					res = false;
				}
        	}
        	else {
        		LOG.warn("LastFM API error: {} {}", lfm.getError().getCode(), lfm.getError().getMessage());
        	}
        }
		return res;
	}
		
	private void treatException(String msg, Exception ex) {
		LastFMException lfmerr;
		if(ex == null)
			lfmerr = new LastFMException(msg);
		else
			lfmerr = new LastFMException(msg, ex);
		eh.addError(lfmerr, ErrorType.COMMON);
		eh.showErrorDialog(ErrorType.COMMON);
		LOG.error(lfmerr.getMessage());
	}
	
	private String buildSignature(MultivaluedMap<String, String> params) {
		String sig = "";
		Set<String> sortedParams = new TreeSet<String>(params.keySet());
		for(String key: sortedParams)
			sig += key+params.getFirst(key);
		sig += API_SECRET;
		return MD5(sig);
	}
	
	private String MD5(String message) {
		String md5 = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] array = md.digest(message.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i=0; i<array.length; i++)
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
			md5 = sb.toString();
	    } catch (NoSuchAlgorithmException e) {}
	    return md5;
	}
}