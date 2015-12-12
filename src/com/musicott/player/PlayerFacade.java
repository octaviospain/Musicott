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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.view.PlayQueueController;
import com.musicott.view.custom.TrackQueueRow;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * @author Octavio Calleya
 *
 */
public class PlayerFacade {
 
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	private static	PlayerFacade instance;
	private SceneManager sc;
	private MusicLibrary ml;
	private PlayQueueController playQueueController;
	private Track currentTrack;
	private TrackPlayer trackPlayer;
	private ObservableList<TrackQueueRow> playList;
	private ObservableList<TrackQueueRow> historyList;
	private boolean random, played;

	private PlayerFacade() {
		sc = SceneManager.getInstance();
		ml = MusicLibrary.getInstance();
		playList = FXCollections.observableArrayList();
		historyList = FXCollections.observableArrayList();
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
			ml.saveLibrary(true, false);
			played = true;
		}
	}
	
	public ObservableList<TrackQueueRow> getPlayList() {
		return playList;
	}
	
	public ObservableList<TrackQueueRow> getHistorylist() {
		return historyList;
	}
	
	public Track getCurrentTrack() {
		return currentTrack;
	}
	
	public TrackPlayer getTrackPlayer() {
		return trackPlayer;
	}

	public void addTracks(Collection<Integer> tracks, boolean placeFirst) {
		if(tracks != null) {
			Thread playableTracksThread = new Thread(() -> {
				List<Integer> playableTracks = new ArrayList<>();
				playableTracks.addAll(tracks.stream().filter(trackID -> {
					Track track = ml.getTrack(trackID);
					return track != null && track.isPlayable();
				}).collect(Collectors.toList()));
				Platform.runLater(() -> addPlayableTracks(playableTracks, placeFirst));
			});
			playableTracksThread.start();
		}
	}
	
	private void addPlayableTracks(List<Integer> playableTracks, boolean placeFirst) {
		if(playQueueController == null)
			playQueueController = sc.getPlayQueueController();
		if(random) {
			playList.clear();
			playableTracks.forEach(trackID -> playList.add(new TrackQueueRow(trackID)));
			random = false;
		}
		else if (!playableTracks.isEmpty()){
			List<TrackQueueRow> newTrackRows = new ArrayList<>();
			playableTracks.forEach(trackID -> newTrackRows.add(new TrackQueueRow(trackID)));
			synchronized(playList) {
				if(placeFirst)
					playList.addAll(0, newTrackRows);
				else
					playList.addAll(newTrackRows);
			}
			LOG.info("Added tracks to player: {}", playableTracks.size());
			if(placeFirst)
				play(false);
		}
	}

	public void removeTrack(int trackID) {
		if(currentTrack != null && currentTrack.getTrackID() == trackID) {
			currentTrack = null;
			stop();
		}
		Iterator<TrackQueueRow> tqrIt = playList.iterator();
		while(tqrIt.hasNext())
			if(tqrIt.next().getRepresentedTrackID() == trackID)
				tqrIt.remove();
		tqrIt = historyList.iterator();
		while(tqrIt.hasNext())
			if(tqrIt.next().getRepresentedTrackID() == trackID)
				tqrIt.remove();
	}
	
	public void play(boolean playRandom) {	
		if(trackPlayer == null || (trackPlayer != null && trackPlayer.getStatus().equals("STOPPED"))) {
			if(!playList.isEmpty())
				setCurrent();
			else if(playRandom)
				ml.playRandomPlaylist();
		}
		else {
			if(trackPlayer.getStatus().equals("PLAYING")) {
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
			play(false);
	}
		
	public void previous() {
		if(!historyList.isEmpty()) {
			setPlayer(historyList.get(0).getRepresentedTrackID());
			historyList.remove(0);
			trackPlayer.play();
		}
		else
			stop();
	}
	
	public void stop() {
		trackPlayer.stop();
		currentTrack = null;
	}
	
	public void resume() {
		trackPlayer.play();
		LOG.info("Player resumed");
	}
	
	public void playHistoryIndex(int index) {
		setPlayer(historyList.get(index).getRepresentedTrackID());
		historyList.remove(index);
		trackPlayer.play();
	}
	
	public void playQueueIndex(int index) {
		setPlayer(playList.get(index).getRepresentedTrackID());
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

	public void setRandomList(List<Integer> randomTrackIDs) {
		if(playQueueController == null)
			playQueueController = sc.getPlayQueueController();
		if(!randomTrackIDs.isEmpty()) {
			LOG.info("Created random list of tracks");
			random = true;
			for(int index=0 ; index<randomTrackIDs.size(); index++) {
				int i = index;
				Platform.runLater(() -> {
					playList.add(new TrackQueueRow(randomTrackIDs.get(i)));
					if(i == 0)
						setCurrent();
				});
			}
			Platform.runLater(() -> sc.getRootController().setStatusMessage("Playing a random playlist"));
		}
		else
			Platform.runLater(() -> sc.getRootController().setStopped());
	}
	
	private void setCurrent() {
		historyList.add(0, playList.get(0));
		setPlayer(playList.get(0).getRepresentedTrackID());
		playList.remove(0);
		trackPlayer.play();
		LOG.info("Playing {}", currentTrack);
	}

	private void setPlayer(int trackID) {
		Track track = ml.getTrack(trackID);
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