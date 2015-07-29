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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.musicott.SceneManager;
import com.musicott.model.Track;
import com.musicott.view.PlayQueueController;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;

/**
 * @author Octavio Calleya
 *
 */
public class Mp3Player implements TrackPlayer {

	private static int DEFAULT_RANDOMQUEUE_SIZE = 8;
	
	private Track currentTrack;
	private MediaPlayer mediaPlayer;
	private PlayQueueController playQueueController;
	private List<Track> playList;
	private List<Track> historyList;
	private List<Track> libraryTracks;
	private boolean random;
	private SceneManager sc;

	public Mp3Player(List<Track> libraryTracks) {
		sc = SceneManager.getInstance();
		playList = new ArrayList<Track>();
		historyList = new ArrayList<Track>();
		random = false;
		this.libraryTracks = libraryTracks;
	}

	@Override
	public void addTracks(List<Track> tracks) {
		if(playQueueController == null)
			this.playQueueController = sc.getPlayQueueController();
		if(random) {
			playQueueController.add(tracks);
			playList.clear();
			playList.addAll(tracks);
			random = false;
		}
		else {
			playQueueController.add(tracks);
			playList.addAll(tracks);
		}
	}

	@Override
	public void play(List<Track> tracks) {
		addTracks(tracks);
		play();
	}
	
	public void play(Track track) {
		List<Track> l = new ArrayList<Track>();
		l.add(track);
		play(l);
	}

	@Override
	public void removeTracks(List<? extends Track> tracks) {
		if(playQueueController != null)
			playQueueController.removeTracks(tracks);
		playList.removeAll(tracks);
		historyList.removeAll(tracks);
		// if currentTrack is removed, stop the player and reset the play button
		if(currentTrack != null)
			for(Track t: tracks)
				if(currentTrack.equals(t)) {
					stop();
					break;
				}
	}

	@Override
	public void play() {	
		if(playQueueController == null)
			this.playQueueController = sc.getPlayQueueController();
		if(mediaPlayer == null) { // first invocation
			if(playList.isEmpty())
				randomList();
			historyList.add(0, playList.get(0));
			setCurrent(playList.get(0));
			playList.remove(0);
			playQueueController.moveTrackToHistory();
			mediaPlayer.play();
		}
		else {
			if(mediaPlayer.getStatus() == Status.PAUSED)
				mediaPlayer.play();
			else if(mediaPlayer.getStatus() == Status.STOPPED) {
				if(playList.isEmpty())
					randomList();
				historyList.add(0, playList.get(0));
				setCurrent(playList.get(0));
				playList.remove(0);
				playQueueController.moveTrackToHistory();
				mediaPlayer.play();
			}
			else if(mediaPlayer.getStatus() == Status.PLAYING) {
				if(playList.isEmpty())
					stop();
				else {
					historyList.add(0, playList.get(0));
					setCurrent(playList.get(0));
					playList.remove(0);
					playQueueController.moveTrackToHistory();
					mediaPlayer.play();
				}
			}
		}
		sc.getRootController().setPlaying();
	}
	
	@Override
	public void pause() {
		if(mediaPlayer.getStatus() == Status.PLAYING)
			mediaPlayer.pause();
	}
	
	@Override
	public void next() {
		if(playList.isEmpty())
			stop();
		else {
			stop();
			play();
		}
	}
	
	@Override
	public void previous() {
		if(!historyList.isEmpty()) {
			setCurrent(historyList.get(0));
			historyList.remove(0);
			playQueueController.removeTopHistoryQueue();
			mediaPlayer.play();
		}
		else
			stop();
	}
	
	@Override
	public void stop() {
		mediaPlayer.stop();
		currentTrack = null;
		sc.getRootController().setStopped();
	}
	
	public Track getCurrentTrack() {
		return currentTrack;
	}
	
	public MediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}
	
	public void removeTrackFromPlayList(int index) {
		if(index == -1)
			playList.clear();
		else
			playList.remove(index);
	}
	
	public void removeTrackFromHistory(int index) {
		if(index == -1)
			historyList.clear();
		else
			historyList.remove(index);
	}
	
	public void playHistoryIndex(int index) {
		sc.getRootController().setPlaying();
		setCurrent(historyList.get(index));
		historyList.remove(index);
		mediaPlayer.play();
	}
	
	public void playQueueIndex(int index) {
		sc.getRootController().setPlaying();
		setCurrent(playList.get(index));
		historyList.add(0, playList.get(index));
		playList.remove(index);
		mediaPlayer.play();
	}
	
	public void increaseVolume(double d) {
		mediaPlayer.setVolume(mediaPlayer.getVolume()+d);
	}
	
	public void decreaseVolume(double d) {
		mediaPlayer.setVolume(mediaPlayer.getVolume()-d);
	}

	private void randomList() {
		random = true;
		if(!libraryTracks.isEmpty()) {
			Random randomGenerator = new Random();
			do {
				int rnd = randomGenerator.nextInt(libraryTracks.size());
				Track t = libraryTracks.get(rnd);
				if(t.getFileName().substring(t.getFileName().length()-3).equals("mp3"))
					playList.add(t);
			} while (playList.size() < DEFAULT_RANDOMQUEUE_SIZE);
			playQueueController.add(playList);
		}
	}

	private void setCurrent(Track track) {
		currentTrack = track;
		File mp3File = new File(currentTrack.getFileFolder()+"/"+currentTrack.getFileName());
		Media media = new Media(mp3File.toURI().toString());
		// disposes the previous player
		if(mediaPlayer != null)
			mediaPlayer.dispose();
		mediaPlayer = new MediaPlayer(media);
		mediaPlayer.setOnEndOfMedia(() -> next());
		sc.getRootController().preparePlayerInfo(mediaPlayer, currentTrack);
	}
}