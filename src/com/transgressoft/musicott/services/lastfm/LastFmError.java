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

package com.transgressoft.musicott.services.lastfm;

import javax.xml.bind.annotation.*;

/**
 * @author Octavio Calleya
 * @version 0.9.1-b
 */
@XmlRootElement (name = "error")
public class LastFmError {

	private String code;
	private String message;

	@XmlValue
	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@XmlAttribute
	public String getCode() {
		return this.code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}	
