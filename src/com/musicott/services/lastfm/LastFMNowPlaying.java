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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Octavio Calleya
 *
 */
@XmlRootElement(name = "nowplaying")
public class LastFMNowPlaying {

	@XmlElement
	private LastFMTrack track;
	@XmlElement
	private LastFMArtist artist;
	@XmlElement
	private LastFMAlbum album;
	@XmlElement
	private LastFMAlbumArtist albumArtist;
	@XmlElement
	private LastFMIgnoredMessage ignoredMessage;
	
	public LastFMTrack getTrack() {
		return track;
	}
	
	public void setTrack(LastFMTrack track) {
		this.track = track;
	}
	
	public LastFMArtist getArtist() {
		return artist;
	}
	
	public void setArtist(LastFMArtist artist) {
		this.artist = artist;
	}
	
	public LastFMAlbum getAlbum() {
		return album;
	}
	
	public void setAlbum(LastFMAlbum album) {
		this.album = album;
	}
	
	public LastFMAlbumArtist getAlbumArtist() {
		return albumArtist;
	}
	
	public void setAlbumArtist(LastFMAlbumArtist albumArtist) {
		this.albumArtist = albumArtist;
	}

	public LastFMIgnoredMessage getIgnoredMessage() {
		return ignoredMessage;
	}
	
	public void setIgnoredMessage(LastFMIgnoredMessage ignoredMessage) {
		this.ignoredMessage = ignoredMessage;
	}	
}