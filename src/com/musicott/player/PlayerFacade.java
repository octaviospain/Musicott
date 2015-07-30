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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.musicott.SceneManager;
import com.musicott.model.Track;
import com.musicott.view.PlayQueueController;

/**
 * @author Octavio Calleya
 *
 */
public class PlayerFacade {

	private static int DEFAULT_RANDOMQUEUE_SIZE = 8;
	
	private Track currentTrack;
	private TrackPlayer trackPlayer;
	private PlayQueueController playQueueController;
	private List<Track> playList;
	private List<Track> historyList;
	private List<Track> libraryTracks;
	private boolean random;
	private SceneManager sc;

	public PlayerFacade(List<Track> libraryTracks) {
		sc = SceneManager.getInstance();
		playList = new ArrayList<Track>();
		historyList = new ArrayList<Track>();
		random = false;
		this.libraryTracks = libraryTracks;
	}

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

	public void play(List<Track> tracks) {
		addTracks(tracks);
		play();
	}
	
	public void play(Track track) {
		List<Track> l = new ArrayList<Track>();
		l.add(track);
		play(l);
	}

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

	public void play() {	
		if(playQueueController == null)
			this.playQueueController = sc.getPlayQueueController();
		if(trackPlayer == null) { // first invocation
			if(playList.isEmpty())
				randomList();
			historyList.add(0, playList.get(0));
			setCurrent(playList.get(0));
			playList.remove(0);
			playQueueController.moveTrackToHistory();
			trackPlayer.play();
		}
		else {
			if(trackPlayer.getStatus().equals("PAUSED"))
				trackPlayer.play();
			else if(trackPlayer.getStatus().equals("STOPPED")) {
				if(playList.isEmpty())
					randomList();
				historyList.add(0, playList.get(0));
				setCurrent(playList.get(0));
				playList.remove(0);
				playQueueController.moveTrackToHistory();
				trackPlayer.play();
			}
			else if(trackPlayer.getStatus().equals("PLAYING")) {
				if(playList.isEmpty())
					stop();
				else {
					historyList.add(0, playList.get(0));
					setCurrent(playList.get(0));
					playList.remove(0);
					playQueueController.moveTrackToHistory();
					trackPlayer.play();
				}
			}
		}
		sc.getRootController().setPlaying();
	}
	
	public void pause() {
		if(trackPlayer.getStatus().equals("PLAYING"));
			trackPlayer.pause();
	}
	
	public void next() {
		if(playList.isEmpty())
			stop();
		else {
			stop();
			play();
		}
	}
	
	public void previous() {
		if(!historyList.isEmpty()) {
			setCurrent(historyList.get(0));
			historyList.remove(0);
			playQueueController.removeTopHistoryQueue();
			trackPlayer.play();
		}
		else
			stop();
	}
	
	public void stop() {
		trackPlayer.stop();
		currentTrack = null;
		sc.getRootController().setStopped();
	}
	
	public Track getCurrentTrack() {
		return currentTrack;
	}
	
	public TrackPlayer getTrackPlayer() {
		return trackPlayer;
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
		trackPlayer.play();
	}
	
	public void playQueueIndex(int index) {
		sc.getRootController().setPlaying();
		setCurrent(playList.get(index));
		historyList.add(0, playList.get(index));
		playList.remove(index);
		trackPlayer.play();
	}
	
	public void increaseVolume(double d) {
		trackPlayer.setVolume(d);
	}
	
	public void decreaseVolume(double d) {
		trackPlayer.setVolume(-1*d);
	}

	private void randomList() {
		random = true;
		if(!libraryTracks.isEmpty()) {
			Random randomGenerator = new Random();
			do {
				int rnd = randomGenerator.nextInt(libraryTracks.size());
				Track t = libraryTracks.get(rnd);
				playList.add(t);
			} while (playList.size() < DEFAULT_RANDOMQUEUE_SIZE);
			playQueueController.add(playList);
		}
	}

	private void setCurrent(Track track) {
		currentTrack = track;
		if(trackPlayer != null)
			trackPlayer.dispose();
		if(track.getFileName().substring(track.getFileName().length()-3).equals("mp3")) {
			trackPlayer = new Mp3Player(track);
			((Mp3Player) trackPlayer).getMediaPlayer().setOnEndOfMedia(() -> next());
		}
		else {
			trackPlayer = new FlacPlayer(track);
		}
		sc.getRootController().preparePlayerInfo(trackPlayer, currentTrack);
	}
}