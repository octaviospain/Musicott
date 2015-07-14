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
import java.util.List;
import java.util.Random;

import com.musicott.SceneManager;
import com.musicott.model.Track;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;

/**
 * @author Octavio Calleya
 *
 */
public class Mp3Player {

	private static int DEFAULT_RANDOMQUEUE_SIZE = 8;
	
	private Track currentTrack;
	private MediaPlayer mediaPlayer;
	private PlayQueue<Track> playQueue;
	private PlayQueue<Track> historyQueue;
	private boolean random;

	public Mp3Player() {
		playQueue = new PlayQueue<Track>();
		historyQueue = new PlayQueue<Track>();
		random = false;
	}
	
	public void addToList(List<Track> selection) {
		if(random) {
			playQueue.clear();
			playQueue.addAll(selection);
			random = false;
		}
		else
			playQueue.addAll(selection);
	}
	
	public void play(List<Track> selection) {
			playQueue.clear();
			playQueue.addAll(selection);
			if(random)
				random = false;
			if(mediaPlayer != null && (mediaPlayer.getStatus() == Status.PLAYING || mediaPlayer.getStatus() == Status.PAUSED))
				mediaPlayer.stop();
			play();
	}
	
	public void removeFromList(List<? extends Track> selection) {
		playQueue.removeAll(selection);
		historyQueue.removeAll(selection);
		// if currentTrack is removed, stop the player and reset the play button
		if(currentTrack != null)
			for(Track t: selection)
				if(currentTrack.equals(t)) {
					mediaPlayer.stop();
					SceneManager.getInstance().getRootController().setStopped();
				}
	}
	
	public void play() {
		if(mediaPlayer == null)	// first invocation
			if(playQueue.size() > 0) {	// the play queue is saved/loaded if Musicott is closed/opened
				historyQueue.add(playQueue.peek());
				setCurrent(playQueue.poll());
				mediaPlayer.play();
			}
			else
				randomQueue();
		
		if(mediaPlayer != null)	// if is still null means empty library
			if(mediaPlayer.getStatus() == Status.PAUSED)
				mediaPlayer.play();
			else if(mediaPlayer.getStatus() == Status.STOPPED) {
				if(playQueue.size() > 0) {
					historyQueue.add(playQueue.peek());
					setCurrent(playQueue.poll());
					mediaPlayer.play();
				}
				else
					randomQueue();
			}
	}
	
	public void pause() {
		if(mediaPlayer.getStatus() == Status.PLAYING)
			mediaPlayer.pause();
	}
	
	public void next() {
		if(random && playQueue.size() == 0)			
			randomQueue();
		else
			if(!playQueue.isEmpty()){			
				historyQueue.add(playQueue.peek());
				setCurrent(playQueue.poll());
				mediaPlayer.play();
			}
			else {
				mediaPlayer.dispose();
				play();
			}
	}
	
	public void previous() {
		if(!historyQueue.isEmpty()) {
			setCurrent(historyQueue.poll());
			mediaPlayer.play();
		}
		else {
			mediaPlayer.stop();
		}
	}
	
	private void randomQueue() {
		random = true;
		List<Track> libraryTrackList = SceneManager.getInstance().getRootController().getTracks();
		if(libraryTrackList.size() > 0) {
			Random randomGenerator = new Random();
			do {
				int rnd = randomGenerator.nextInt(libraryTrackList.size());
				Track t = libraryTrackList.get(rnd);
				if(t.getFileName().substring(t.getFileName().length()-3).equals("mp3"))
					playQueue.add(t);
			} while (playQueue.size() < DEFAULT_RANDOMQUEUE_SIZE);
			setCurrent(playQueue.peek());
			mediaPlayer.play();
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
		SceneManager.getInstance().getRootController().preparePlayerInfo(mediaPlayer, currentTrack);
	}
}