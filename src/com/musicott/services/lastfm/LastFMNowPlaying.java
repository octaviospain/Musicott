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
 * @version 0.9-b
 */
@XmlRootElement (name = "nowplaying")
public class LastFmNowPlaying {

	@XmlElement
	private LastFmTrack track;
	@XmlElement
	private LastFmArtist artist;
	@XmlElement
	private LastFmAlbum album;
	@XmlElement
	private LastFmAlbumArtist albumArtist;
	@XmlElement
	private LastFmIgnoredMessage ignoredMessage;

	public LastFmTrack getTrack() {
		return track;
	}

	public void setTrack(LastFmTrack track) {
		this.track = track;
	}

	public LastFmArtist getArtist() {
		return artist;
	}

	public void setArtist(LastFmArtist artist) {
		this.artist = artist;
	}

	public LastFmAlbum getAlbum() {
		return album;
	}

	public void setAlbum(LastFmAlbum album) {
		this.album = album;
	}

	public LastFmAlbumArtist getAlbumArtist() {
		return albumArtist;
	}

	public void setAlbumArtist(LastFmAlbumArtist albumArtist) {
		this.albumArtist = albumArtist;
	}

	public LastFmIgnoredMessage getIgnoredMessage() {
		return ignoredMessage;
	}

	public void setIgnoredMessage(LastFmIgnoredMessage ignoredMessage) {
		this.ignoredMessage = ignoredMessage;
	}
}
