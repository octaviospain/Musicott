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

package com.musicott.player;

import com.musicott.*;
import com.musicott.model.*;
import com.musicott.model.Track;
import com.musicott.services.*;
import com.musicott.view.*;
import com.musicott.view.custom.*;
import javafx.application.*;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.media.*;
import javafx.scene.media.MediaPlayer.*;
import javafx.util.*;
import org.slf4j.*;

import java.util.*;
import java.util.stream.*;

/**
 * Singleton class that isolates the usage of the music player.
 *
 * @author Octavio Calleya
 * @version 0.9
 */
public class PlayerFacade {
 
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	private static PlayerFacade instance;
	private PlayQueueController playQueueController;
	private PlayerController playerController;
	private Optional<Track> currentTrack;
	private TrackPlayer trackPlayer;
	private ObservableList<TrackQueueRow> playList;
	private ObservableList<TrackQueueRow> historyList;
	private boolean playingRandom;
	private boolean played;
	private boolean scrobbled;
	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	private Services services = Services.getInstance();

	private PlayerFacade() {		
		playList = FXCollections.observableArrayList();
		historyList = FXCollections.observableArrayList();
		playingRandom = false;
		played = false;
		scrobbled = false;
		currentTrack = Optional.empty();
	}
	
	public static PlayerFacade getInstance() {
		if(instance == null)
			instance = new PlayerFacade();
		return instance;
	}
	
	public ObservableList<TrackQueueRow> getPlayList() {
		return playList;
	}
	
	public ObservableList<TrackQueueRow> getHistorylist() {
		return historyList;
	}

	public Optional<Track> getCurrentTrack() {
		return currentTrack;
	}

	public String getPlayerStatus() {
		return trackPlayer.getStatus();
	}

	/**
	 * Adds a {@link List} of tracks to the play queue. Checks if all tracks given are playable.
	 *
	 * @param tracksID The <tt>List</tt> of tracks ids to add.
	 * @param placeFirstInPlayQueue <tt>true</tt> if the tracks added should be placed in the beggining
	 * o							of the play queue, <tt>false</tt> otherwise
	 */
	public void addTracksToPlayQueue(Collection<Integer> tracksID, boolean placeFirstInPlayQueue) {
		Thread playableTracksThread = new Thread(() -> {

			List<Integer> playableTracks = tracksID.stream().filter(trackID -> {
				Optional<Track> track = musicLibrary.getTrack(trackID);
				boolean[] isPlayable = new boolean[1];
				track.ifPresent(t ->  isPlayable[0] = t.isPlayable());

				return track.isPresent() && isPlayable[0];
			}).collect(Collectors.toList());

			Platform.runLater(() -> addPlayableTracksToPlayQueue(playableTracks, placeFirstInPlayQueue));
		});
		playableTracksThread.start();
	}

	/**
	 * Adds a {@link List} of tracks to the play queue.
	 *
	 * @param playableTracks The <tt>List</tt> of tracks ids that are playable.
	 * @param placeFirstInPlayQueue <tt>true</tt> if the tracks added should be placed in the beggining
	 * o							of the play queue, <tt>false</tt> otherwise
	 */
	private void addPlayableTracksToPlayQueue(List<Integer> playableTracks, boolean placeFirstInPlayQueue) {
		if(playQueueController == null)
			playQueueController = stageDemon.getPlayQueueController();
		if(playingRandom) {
			playList.clear();
			playableTracks.forEach(trackID -> playList.add(new TrackQueueRow(trackID)));
			playingRandom = false;
		}
		else if (!playableTracks.isEmpty()){
			List<TrackQueueRow> newTrackRows = playableTracks.stream()
					.map(TrackQueueRow::new).collect(Collectors.toList());
			synchronized(playList) {
				if(placeFirstInPlayQueue)
					playList.addAll(0, newTrackRows);
				else
					playList.addAll(newTrackRows);
			}
			LOG.info("Added {} tracks to player", playableTracks.size());
			if(placeFirstInPlayQueue)
				play(false);
		}
	}

	/**
	 * Deletes a {@link Track} from the play queue and from the history queue.
	 *
	 * @param trackID The track id.
	 */
	public void deleteTrackFromQueues(int trackID) {
		currentTrack.ifPresent(track -> {
			if(track.getTrackID() == trackID) {
				currentTrack = Optional.empty();
				stop();
			}
		});

		Iterator<TrackQueueRow> trackQueueRowIterator = playList.iterator();
		while(trackQueueRowIterator.hasNext())
			if(trackQueueRowIterator.next().getRepresentedTrackID() == trackID)
				trackQueueRowIterator.remove();

		trackQueueRowIterator = historyList.iterator();
		while(trackQueueRowIterator.hasNext())
			if(trackQueueRowIterator.next().getRepresentedTrackID() == trackID)
				trackQueueRowIterator.remove();
	}

	public void play(boolean playRandom) {
		if(trackPlayer == null || "STOPPED".equals(trackPlayer.getStatus())) {
			if(!playList.isEmpty())
				setCurrentTrack();
			else if(playRandom)
				musicLibrary.getRandomPlaylist();
		}
		else {
			if("PLAYING".equals(trackPlayer.getStatus())) {
				if(playList.isEmpty())
					stop();
				else
					setCurrentTrack();
			}
		}
	}

	public void pause() {
		if(trackPlayer != null && "PLAYING".equals(trackPlayer.getStatus())) {
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
		currentTrack = Optional.empty();
	}
	
	public void resume() {
		trackPlayer.play();
		LOG.info("Player resumed");
	}

	public void seek(double seekValue) {
		trackPlayer.seek(seekValue);
		LOG.debug("Player seeked value {}", seekValue);
	}

	/**
	 * Plays a {@link Track} that is on the history queue selected by the user.
	 *
	 * @param index The position of the <tt>track</tt> in the history queue.
	 */
	public void playHistoryIndex(int index) {
		setPlayer(historyList.get(index).getRepresentedTrackID());
		historyList.remove(index);
		trackPlayer.play();
	}

	/**
	 * Plays a {@link Track} that is on the play queue selected by the user.
	 *
	 * @param index The position of the <tt>track</tt> in the history queue.
	 */
	public void playQueueIndex(int index) {
		setPlayer(playList.get(index).getRepresentedTrackID());
		historyList.add(0, playList.get(index));
		playList.remove(index);
		trackPlayer.play();
	}
	
	public void increaseVolume(double amount) {
		if(trackPlayer != null)
			trackPlayer.setVolume(amount);
	}

	public void decreaseVolume(double amount) {
		if(trackPlayer != null)
			trackPlayer.setVolume(-1 * amount);
	}

	public void setRandomList(List<Integer> randomTrackIDs) {
		if(playQueueController == null)
			playQueueController = stageDemon.getPlayQueueController();

		if(!randomTrackIDs.isEmpty()) {
			LOG.info("Created random list of tracks");
			playingRandom = true;
			for(int index = 0; index < randomTrackIDs.size(); index++) {
				int i = index;
				Platform.runLater(() -> {
					playList.add(new TrackQueueRow(randomTrackIDs.get(i)));
					if(i == 0)
						setCurrentTrack();
				});
			}
			Platform.runLater(() -> stageDemon.getNavigationController().setStatusMessage("Playing a random playlist"));
		}
		else
			Platform.runLater(() -> stageDemon.getPlayerController().setStopped());
	}

	private void setCurrentTrack() {
		historyList.add(0, playList.get(0));
		setPlayer(playList.get(0).getRepresentedTrackID());
		playList.remove(0);
		trackPlayer.play();
		LOG.info("Playing {}", currentTrack);
	}

	private void setPlayer(int trackID) {
		scrobbled = false;
		played = false;
		if(playerController == null)
			playerController = stageDemon.getPlayerController();
		currentTrack = musicLibrary.getTrack(trackID);

		currentTrack.ifPresent(track -> {
			if(trackPlayer != null) {
				trackPlayer.dispose();
				LOG.debug("Disposed recent player");
			}
			String fileExtension = track.getFileFormat();
			if("mp3".equals(fileExtension) || "wav".equals(fileExtension) || "m4a".equals(fileExtension)) {
				trackPlayer = new NativePlayer();
			}
			else if("flac".equals(fileExtension)) {
				trackPlayer = new FlacPlayer();
				//TODO
			}
			trackPlayer.setTrack(track);
			bindMediaPlayer();
			playerController.updatePlayer(track);
			LOG.debug("Created new player");
		});
	}

	/**
	 * Binds the properties of the {@link MediaPlayer} to the components of the view.
	 */
	private void bindMediaPlayer() {
		MediaPlayer mediaPlayer = ((NativePlayer) trackPlayer).getMediaPlayer();
		mediaPlayer.volumeProperty().bindBidirectional(playerController.volumeSliderValueProperty());

		mediaPlayer.currentTimeProperty().addListener((observable, oldTime, newTime) -> {
			Duration halfTime = mediaPlayer.getStopTime().divide(2.0);
			if(newTime.greaterThanOrEqualTo(halfTime))
				incrementCurrentTrackPlayCount();
			if(isCurrentTrackValidToScrobble(mediaPlayer, newTime)) {
				scrobbled = true;
				services.udpateAndScrobbleLastFM(currentTrack.get());
			}
		});

		DoubleProperty trackSliderMaxProperty = playerController.trackSliderMaxProperty();
		trackSliderMaxProperty.bind(
				Bindings.createDoubleBinding(
						() -> mediaPlayer.totalDurationProperty().getValue().toMillis(),
						mediaPlayer.totalDurationProperty()));

		BooleanProperty trackSliderValueChangingProperty = playerController.trackSliderValueChangingProperty();
		DoubleProperty trackSliderValueProperty = playerController.trackSliderValueProperty();
		mediaPlayer.currentTimeProperty().addListener(
				(observable, oldTime, newTime) -> {
					if(!trackSliderValueChangingProperty.get())
						trackSliderValueProperty.setValue(newTime.toMillis());
				});

		DoubleProperty trackProgressBarProgressProperty = playerController.trackProgressBarProgressProperty();
		trackSliderValueProperty.addListener((observable, oldValue, newValue) -> {
			Double endTime = mediaPlayer.getStopTime().toMillis();
			if(trackSliderValueChangingProperty.get() &&
					(!endTime.equals(Double.POSITIVE_INFINITY) || !endTime.equals(Double.NaN))) {
				trackProgressBarProgressProperty.set(newValue.doubleValue() / endTime);
				mediaPlayer.seek(Duration.millis(newValue.doubleValue()));
			}
		});

		mediaPlayer.currentTimeProperty().addListener(
				(observable, oldTime, newTime) ->
						trackProgressBarProgressProperty.set(newTime.toMillis() / trackSliderMaxProperty.get()));

		mediaPlayer.currentTimeProperty().addListener(
				(observable, oldTime, newTime) ->
						playerController.updateTrackLabels(newTime, mediaPlayer.getMedia().getDuration()));

		mediaPlayer.statusProperty().addListener((observable, oldStatus, newStatus) -> {
			if(Status.PLAYING == newStatus)
				playerController.setPlaying();
			else if (Status.PAUSED == newStatus)
				playerController.playButtonSelectedProperty().setValue(false);
			else if (Status.STOPPED == newStatus) {
				playerController.setStopped();
				playerController.volumeSliderValueProperty().unbindBidirectional(mediaPlayer.volumeProperty());
			}
		});
	}

	private void incrementCurrentTrackPlayCount() {
		if(!played) {
			currentTrack.ifPresent(track -> track.incrementPlayCount());
			musicLibrary.saveLibrary(true, false, false);
			played = true;
		}
	}

	private boolean isCurrentTrackValidToScrobble(MediaPlayer mediaPlayer, Duration newTime) {
		boolean isDurationBeyond30Seconds = mediaPlayer.getTotalDuration().greaterThanOrEqualTo(Duration.seconds(30));
		boolean isDurationBeyondMidTime = newTime.greaterThanOrEqualTo(mediaPlayer.getStopTime().divide(2.0));
		boolean isDurationLongerThan4Minutes = newTime.greaterThanOrEqualTo(Duration.minutes(4));

		return !scrobbled && isDurationBeyond30Seconds && (isDurationBeyondMidTime || isDurationLongerThan4Minutes);
	}
}