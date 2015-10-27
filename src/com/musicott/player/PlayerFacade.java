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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.error.CommonException;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.view.PlayQueueController;

/**
 * @author Octavio Calleya
 *
 */
public class PlayerFacade {

	private final Logger LOG = LoggerFactory.getLogger(PlayerFacade.class.getName());
	private static int DEFAULT_RANDOMQUEUE_SIZE = 8;
	
	private Track currentTrack;
	private TrackPlayer trackPlayer;
	private PlayQueueController playQueueController;
	private List<Track> playList;
	private List<Track> historyList;
	private List<Track> libraryTracks;
	private boolean random;
	private SceneManager sc;
	private static PlayerFacade instance;

	private PlayerFacade() {
		sc = SceneManager.getInstance();
		libraryTracks = MusicLibrary.getInstance().getTracks();
		playList = new ArrayList<>();
		historyList = new ArrayList<>();
		random = false;
	}
	
	public static PlayerFacade getInstance() {
		if(instance == null)
			instance = new PlayerFacade();
		return instance;
	}

	public void addTracks(List<Track> tracks) {
		if(playQueueController == null)
			playQueueController = sc.getPlayQueueController();
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
		LOG.info("Added tracks to player: {}", tracks);
	}

	public void play(List<Track> tracks) {
		checkPlayableTracks(tracks);
		if(!tracks.isEmpty()) {
			addTracks(tracks);
			play();
		}
	}
	
	public void play(Track track) {
		List<Track> l = new ArrayList<>();
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
			prepareAndPlay();
		}
		else {
			if(trackPlayer.getStatus().equals("PAUSED")) {
				LOG.info("Player resumed");
				sc.getRootController().setPlaying();
				trackPlayer.play();
			}
			else if(trackPlayer.getStatus().equals("STOPPED")) {
				prepareAndPlay();
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
		if(trackPlayer.getStatus().equals("PLAYING")) {
			trackPlayer.pause();
			LOG.info("Player paused");
		}
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
		setPlayer(historyList.get(index));
		historyList.remove(index);
		trackPlayer.play();
	}
	
	public void playQueueIndex(int index) {
		sc.getRootController().setPlaying();
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
		if(!libraryTracks.isEmpty()) {
			if(libraryTracks.size() <= DEFAULT_RANDOMQUEUE_SIZE) {
				for(Track t: libraryTracks)
					if(t.getInDisk())
						playList.add(t);
				playQueueController.add(playList);
			}
			else {
				Set<Track> notInDiskTracks = new HashSet<Track>();
				Random randomGenerator = new Random();
				do {
					int rnd = randomGenerator.nextInt(libraryTracks.size());
					Track t = libraryTracks.get(rnd);
					if(!t.getFileFormat().equals("flac") && t.getInDisk())		// TODO for flac
						playList.add(t);
					else
						notInDiskTracks.add(t);
				} while (playList.size() < DEFAULT_RANDOMQUEUE_SIZE || notInDiskTracks.containsAll(libraryTracks));
			}
			playQueueController.add(playList);
		}
		if(!playList.isEmpty()) {
			LOG.info("Created random list of tracks");
			random = true;
		}
		else {
			random = false;
			sc.getRootController().setStopped();
		}
		return random;
	}
	
	private void checkPlayableTracks(List<Track> tracks) {
		LOG.debug("Checking files consistency");
		Iterator<Track> it = tracks.iterator();
		while(it.hasNext()) {
			Track track = it.next();
			File file = new File(track.getFileFolder()+"/"+track.getFileName());
			if(!file.exists()) {
				LOG.warn("File {} not found", file);
				ErrorHandler.getInstance().addError(new CommonException(track.getFileFolder()+"/"+track.getFileName()+" not found"), ErrorType.COMMON);
				track.setInDisk(false);
				it.remove();
			}
			if(track.getFileFormat().equals("flac"))
				it.remove();
		}
		if(ErrorHandler.getInstance().hasErrors(ErrorType.COMMON))
			ErrorHandler.getInstance().showErrorDialog(ErrorType.COMMON);
	}
	
	private void prepareAndPlay() {
		if(playList.isEmpty()) {
			if(randomList())
				setCurrent();
		}
		else
			setCurrent();
	}
	
	private void setCurrent() {
		historyList.add(0, playList.get(0));
		setPlayer(playList.get(0));
		playList.remove(0);
		playQueueController.moveTrackToHistory();
		LOG.info("Playing {}", historyList.get(0));
		trackPlayer.play();
		sc.getRootController().setPlaying();
	}

	private void setPlayer(Track track) {
		currentTrack = track;
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