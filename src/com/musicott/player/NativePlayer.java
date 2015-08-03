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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.musicott.player;

import java.io.File;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class Mp3Player implements TrackPlayer {
	
	private MediaPlayer mediaPlayer;
	
	public Mp3Player(Track track) {
		File mp3File = new File(track.getFileFolder()+"/"+track.getFileName());
		Media media = new Media(mp3File.toURI().toString());
		mediaPlayer = new MediaPlayer(media);
	}
	
	public MediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}
	
	@Override
	public String getStatus() {
		String status = "UNKNOWN";
		switch(mediaPlayer.getStatus()) {
			case PLAYING: status = "PLAYING";
				break;
			case STOPPED: status = "STOPPED";
				break;
			case DISPOSED: status = "DISPOSED";
				break;
			case HALTED: status = "HALTED";
				break;
			case PAUSED: status = "PAUSED";
				break;
			case READY: status = "READY";
				break;
			case STALLED: status = "STALLED";
				break;
			case UNKNOWN: status = "UNKNOWN";
				break;
		}
		return status;
	}
	
	@Override
	public void setTrack(Track track) {
	}

	@Override
	public void setVolume(double value) {
		mediaPlayer.setVolume(mediaPlayer.getVolume() + value);
	}

	@Override
	public void play() {
		mediaPlayer.play();
	}

	@Override
	public void pause() {
		mediaPlayer.pause();
	}

	@Override
	public void stop() {
		mediaPlayer.stop();
	}

	@Override
	public void dispose() {
		mediaPlayer.dispose();
		mediaPlayer = null;
	}
}