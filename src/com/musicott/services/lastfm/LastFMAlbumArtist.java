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
 * @version 0.9
 */
@XmlRootElement
public class LastFmAlbumArtist {

	@XmlAttribute
	private String corrected;
	@XmlValue
	private String value;
	
	public String getCorrected() {
		return corrected;
	}
	
	public void setCorrected(String corrected) {
		this.corrected = corrected;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}	
}
