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

package com.musicott.services.lastfm;

import javax.xml.bind.annotation.*;

/**
 * @author Octavio Calleya
 *
 */
@XmlRootElement(name = "lfm")
public class LastFMResponse {

	private String token;
	private String status;
	private LastFMError error;
	private LastFMSession session;
	
	public LastFMResponse() {
	}

	@XmlAttribute
	public String getStatus() {
		return this.status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}

	@XmlElement
	public String getToken() {
		return this.token;
	}
	
	public void setToken(String token) {
		this.token = token;
	}

	@XmlElement
	public LastFMError getError() {
		return this.error;
	}
	
	public void setError(LastFMError error) {
		this.error = error;
	}

	@XmlElement
	public LastFMSession getSession() {
		return session;
	}
	
	public void setSession(LastFMSession session) {
		this.session = session;
	}
}
