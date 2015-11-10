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

package com.musicott.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.view.PlayQueueController;

import javafx.application.Platform;

/**
 * @author Octavio Calleya
 *
 */
public class PlayerFacade {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private static int DEFAULT_RANDOMQUEUE_SIZE = 8;

	private static	PlayerFacade instance;
	private SceneManager sc;
	private MusicLibrary ml;
	private PlayQueueController playQueueController;
	private Track currentTrack;
	private TrackPlayer trackPlayer;
	private List<Track> playList;
	private List<Track> historyList;
	private boolean random, played;

	private PlayerFacade() {
		sc = SceneManager.getInstance();
		ml = MusicLibrary.getInstance();
		playList = new ArrayList<>();
		historyList = new ArrayList<>();
		random = false;
		played = false;
	}
	
	public static PlayerFacade getInstance() {
		if(instance == null)
			instance = new PlayerFacade();
		return instance;
	}
	
	public void incrementCurentTrackPlayCount() {
		if(!played) {
			currentTrack.incrementPlayCount();
			sc.saveLibrary(true, false);
			played = true;
		}
	}
	
	public Track getCurrentTrack() {
		return currentTrack;
	}
	
	public TrackPlayer getTrackPlayer() {
		return trackPlayer;
	}

	public void addTracks(List<Track> tracks) {
		List<Track> playableTracks = tracks.stream().filter(t -> t.isPlayable()).collect(Collectors.toList());
		if(playQueueController == null)
			playQueueController = sc.getPlayQueueController();
		if(random) {
			playQueueController.doDeleteAll();
			playQueueController.add(playableTracks);
			playList.clear();
			playList.addAll(playableTracks);
			random = false;
		}
		else if (playableTracks.size() != 0){
			playQueueController.add(playableTracks);
			playList.addAll(playableTracks);
			LOG.info("Added tracks to player: {}", playableTracks);
		}
	}

	public void removeTracks(List<? extends Track> tracks) { 
		if(playQueueController == null)
			playQueueController = sc.getPlayQueueController();
		playQueueController.removeTracks(tracks);
		playList.removeAll(tracks);
		historyList.removeAll(tracks);
		// if currentTrack is removed, stop the player
		if(currentTrack != null)
			for(Track t: tracks)
				if(currentTrack.equals(t)) {
					stop();
					break;
				}
	}
	
	public void play(List<Track> tracks) {
		addTracks(tracks);
		play();
	}
	
	public void play(Track track) {
		List<Track> l = new ArrayList<>();
		l.add(track);
		play(l);
	}

	public void play() {	
		if(trackPlayer == null || (trackPlayer != null && trackPlayer.getStatus().equals("STOPPED"))) {
			if(!playList.isEmpty())
				setCurrent();
		}
		else {
			if(trackPlayer.getStatus().equals("PAUSED")) {
				LOG.info("Player resumed");
				trackPlayer.play();
			}
			else if(trackPlayer.getStatus().equals("PLAYING")) {
				if(playList.isEmpty())
					stop();
				else 
					setCurrent();
			}
		}
	}
	
	public void playOrRandom() {
		if(trackPlayer == null || (trackPlayer != null && trackPlayer.getStatus().equals("STOPPED"))) {
			if(randomList())
				setCurrent();
			else
				sc.getRootController().setStopped();
		}
		else {
			if(trackPlayer.getStatus().equals("PAUSED")) {
				LOG.info("Player resumed");
				trackPlayer.play();
			}
			else if(trackPlayer.getStatus().equals("PLAYING")) {
				if(playList.isEmpty())
					stop();
				else 
					setCurrent();
			}
		}
	}
	
	public void pause() {
		if(trackPlayer != null && trackPlayer.getStatus().equals("PLAYING")) {
			trackPlayer.pause();
			LOG.info("Player paused");
		}
	}

	public void next() {
		if(playList.isEmpty())
			stop();
		else
			play();
	}
	
	public void previous() {
		if(!historyList.isEmpty()) {
			setPlayer(historyList.get(0));
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
		setPlayer(historyList.get(index));
		historyList.remove(index);
		trackPlayer.play();
	}
	
	public void playQueueIndex(int index) {
		setPlayer(playList.get(index));
		historyList.add(0, playList.get(index));
		playList.remove(index);
		trackPlayer.play();
	}
	
	public void increaseVolume(double d) {
		if(trackPlayer != null)
			trackPlayer.setVolume(d);
	}
	
	public void decreaseVolume(double d) {
		if(trackPlayer != null)
			trackPlayer.setVolume(-1*d);
	}

	private boolean randomList() {
		if(playQueueController == null)
			playQueueController = sc.getPlayQueueController();
		List<Track> libraryTracks = ml.getTracks();
		List<Track> playableTracks = libraryTracks.stream().filter(t -> t.isPlayable()).collect(Collectors.toList());
		if(!playableTracks.isEmpty()) {
			if(playableTracks.size() <= DEFAULT_RANDOMQUEUE_SIZE) {
				playList.addAll(playableTracks);
				playQueueController.add(playList);
			}
			else {
				Random randomGenerator = new Random();
				List<Integer> listVisited = new ArrayList<>();
				do {
					int rnd = randomGenerator.nextInt(libraryTracks.size());
					playList.add(playableTracks.get(rnd));
					listVisited.add(rnd);
				} while (playList.size() < DEFAULT_RANDOMQUEUE_SIZE || listVisited.size() == playableTracks.size());
			}
			playQueueController.add(playList);
		}
		if(!playList.isEmpty()) {
			LOG.info("Created random list of tracks");
			random = true;
		}
		else
			random = false;
		if(random)
			Platform.runLater(() -> sc.getRootController().setStatusMessage("Playing a random playlist"));
		return random;
	}
	
	private void setCurrent() {
		historyList.add(0, playList.get(0));
		setPlayer(playList.get(0));
		playList.remove(0);
		playQueueController.moveTrackToHistory();
		trackPlayer.play();
		LOG.info("Playing {}", historyList.get(0));
	}

	private void setPlayer(Track track) {
		currentTrack = track;
		played = false;
		if(trackPlayer != null) {
			trackPlayer.dispose();
			LOG.debug("Disposed recent player");
		}
		String fileExtension = track.getFileFormat();
		if(fileExtension.equals("mp3") || fileExtension.equals("wav") || fileExtension.equals("m4a")) {
			trackPlayer = new NativePlayer();
		}
		else if(fileExtension.equals("flac")) {
			trackPlayer = new FlacPlayer();
			//TODO
		}
		trackPlayer.setTrack(track);
		LOG.debug("Created new player");
		sc.getRootController().preparePlayerInfo(trackPlayer, currentTrack);
	}
}