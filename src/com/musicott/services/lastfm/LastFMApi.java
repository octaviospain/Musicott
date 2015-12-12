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

package com.musicott.services.lastfm;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author Octavio Calleya
 *
 */
public class LastFMApi {

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
	private ErrorHandler eh;
	private String token;
	private String sessionKey;
	private String username;
	private String password;
	private Client client;
	private WebResource resource;
	
	public LastFMApi() {
		eh = ErrorHandler.getInstance();
		client = Client.create();
		resource = client.resource(API_ROOT_URL);
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(CONFIG_FILE));
			API_KEY = prop.getProperty("lastfm_api_key");
			API_SECRET = prop.getProperty("lastfm_api_secret");
		} catch (IOException e) {
			treatException("Error reading LastFM API properties", e);
		}
	}
	
	public boolean authenticate() {
		boolean res = true;
		sessionKey = MainPreferences.getInstance().getLastFMSessionKey();
		if(!retrieveToken())
			res = false;
		else if(sessionKey == null) {
			res = fetchSession();
		}
		return res;
	}
		
	private boolean retrieveToken() {
		boolean res = true;
		LastFMResponse lfm;
		if(API_KEY == null || API_SECRET == null) {
			res = false;
			LOG.warn("LastFM API token retrieving rejected: some of the LastFM API properties are null");
		} else {
			MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
	        queryParams.add("method", "auth.getToken");
	        queryParams.add("api_key", API_KEY);
	        queryParams.add("api_sig", buildSignature(queryParams));
	    	lfm = makeRequest(queryParams, HttpMethod.GET);
	    	if(lfm == null || (lfm != null && !lfm.getStatus().equals("ok")))
	    		res = false;
	    	else
	        	token = lfm.getToken();	       
		}
		LOG.debug("LastFM API token: {}", token);
        return res;
	}
	
	private LastFMResponse makeRequest(MultivaluedMap<String, String> params, String method) {
		LastFMResponse lfmRequest = null;
    	ClientResponse response = null;
        try {
        	switch(method) {
	        	case HttpMethod.GET:
	        		response = resource.queryParams(params).get(ClientResponse.class);
	        		break;
	        	case HttpMethod.POST:
	        		response = resource.queryParams(params).post(ClientResponse.class);
	        		break;
        	}			
	     	LOG.debug("LastFM API GET petition status: {}", response.getStatus());
	     	lfmRequest = response.getEntity(LastFMResponse.class);
        } catch (ClientHandlerException e) {
        	if(e.getMessage().contains("UnknownHostException"))
        		treatException("Bad internet connection", e);
        	else
        		treatException(e.getMessage(), e);
        } finally {
	     	response.close();
        }
		return lfmRequest;
	}
	
	private boolean fetchSession() {
		boolean res = true;
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("method", "auth.getMobileSession");
        queryParams.add("api_key", API_KEY);
        queryParams.add("password", password);
        queryParams.add("username", username);
        queryParams.add("api_sig", buildUserSignature(queryParams));
        LastFMResponse lfm = makeRequest(queryParams, HttpMethod.POST);
        if(lfm == null)
        	res = false;
        else {
        	if(lfm.getStatus().equals("ok")) {
	        	sessionKey = lfm.getSession().getSessionKey();
				if(sessionKey != null)
					MainPreferences.getInstance().setLastFMSessionkey(sessionKey);
				else
					res = false;
        	}
        	else {
        		LOG.warn("LastFM API error: {} {}", lfm.getError().getCode(), lfm.getError().getMessage());
        	}
        }
		return res;
	}
	
	private void treatException(String msg, Exception ex) {
		LastFMException lfmerr = new LastFMException(msg, ex);
		eh.addError(lfmerr, ErrorType.COMMON);  // Comment for testing
		eh.showErrorDialog(ErrorType.COMMON);
		LOG.error(lfmerr.getMessage());
	}
	
	private String buildSignature(MultivaluedMap<String, String> params) {
		String sig = "";
		Set<String> sortedParams = new TreeSet<String>(params.keySet());
		for(String key: sortedParams)
			sig += key+params.getFirst(key);
		sig += (token == null ? "" : "token"+token)+(API_SECRET == null ? "" : API_SECRET);
		return MD5(sig);
	}
	
	private String buildUserSignature(MultivaluedMap<String, String> params) {
		String sig = "";
		Set<String> sortedParams = new TreeSet<String>(params.keySet());
		for(String key: sortedParams)
			sig += key+params.getFirst(key);
		sig += API_SECRET == null ? "" : API_SECRET;
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