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

package com.musicott.model;

import com.musicott.*;
import com.musicott.player.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.collections.*;
import org.slf4j.*;

import java.util.*;
import java.util.stream.*;

/**
 * @author Octavio Calleya
 *
 */
public class MusicLibrary {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private static final int DEFAULT_RANDOM_QUEUE_SIZE = 8;

	private static MusicLibrary instance;

	private ObservableMap<Integer, Track> musicottTracks;
	private Map<Integer, float[]> waveforms;
	private List<Playlist> playlists;

	private ObservableList<Map.Entry<Integer, Track>> showingTracks;
	private ListProperty<Map.Entry<Integer, Track>> showingTracksProperty;

	private ObservableList<Map.Entry<Integer, Track>> allTracks;
	private ListProperty<Map.Entry<Integer, Track>> allTracksProperty;

	private SaveMusicLibraryTask saveMusicLibraryTask;

	private MusicLibrary() {
		musicottTracks = FXCollections.observableHashMap();
	}
	
	public static MusicLibrary getInstance() {
		if(instance == null)
			instance = new MusicLibrary();
		return instance;
	}
	
	public void setTracks(ObservableMap<Integer, Track> tracks) {
		synchronized(musicottTracks) {
			musicottTracks = tracks;
			bindAllTracks();
			bindShowingTracks();

			// Listen changes in musicott's map of tracks to add/remove in the showing tracks and playlists
			musicottTracks.addListener((MapChangeListener.Change<? extends Integer, ? extends Track> c) -> {
				if(c.wasAdded()) {
					Track added = c.getValueAdded();
					Map.Entry<Integer, Track> addedEntry = new AbstractMap.SimpleEntry<>(added.getTrackID(), added);
					allTracks.add(addedEntry);
					if(StageDemon.getInstance().getNavigationController().selectedPlaylistProperty() == null)
						Platform.runLater(() -> showingTracks.add(addedEntry));
				}
				else if (c.wasRemoved()) {
		          	Track removed = c.getValueRemoved();
					Map.Entry<Integer, Track> removedEntry = new AbstractMap.SimpleEntry<>(removed.getTrackID(), removed);
		          	waveforms.remove(removed.getTrackID());
					allTracks.remove(removedEntry);
					PlayerFacade.getInstance().removeTrack(removed.getTrackID());
		          	showingTracks.remove(new AbstractMap.SimpleEntry<>(removed.getTrackID(), removed));
		          	playlists.forEach(p -> {
		          		if(p.getTracks().remove((Integer) removed.getTrackID()))
		          			p.changePlaylistCover();
		          	});
					LOG.info("Deleted track: {}", removed);
				}
				saveLibrary(true, true, true);
			});
		}
	}

	private void bindAllTracks() {
		allTracks = FXCollections.observableArrayList(musicottTracks.entrySet());
		allTracksProperty = new SimpleListProperty<>(this, "All tracks");
		allTracksProperty.bind(new SimpleObjectProperty<>(allTracks));
	}

	/**
	 * Binds the tracks that must be shown on the table to a list property
	 */
	private void bindShowingTracks() {
		showingTracks = FXCollections.observableArrayList(musicottTracks.entrySet());;
		showingTracksProperty = new SimpleListProperty<>(this, "Showing tracks");
		showingTracksProperty.bind(new SimpleObjectProperty<>(showingTracks));
	}

	protected ObservableMap<Integer, Track> getTracks(){
		synchronized(musicottTracks) {
			return this.musicottTracks;
		}
	}

	public Track getTrack(int trackID) {
		synchronized(musicottTracks) {
			return musicottTracks.get(trackID);
		}
	}

	public void addTracks(Map<Integer, Track> newTracks) {
		synchronized(musicottTracks) {
			musicottTracks.putAll(newTracks);
		}
	}

	public void deleteTracks(List<Integer> selection) {
		synchronized(musicottTracks) {
			Platform.runLater(() -> musicottTracks.keySet().removeAll(selection));
		}
		Platform.runLater(() -> StageDemon.getInstance().getNavigationController().setStatusMessage("Removed " + selection.size() + " tracks"));
	}

	public void clearShowingTracks() {
		synchronized(showingTracks) {
			showingTracks.clear();
		}
	}

	public void setWaveforms(Map<Integer,float[]> waveforms) {
		synchronized(waveforms) {
			this.waveforms = waveforms;
		}
	}

	protected Map<Integer,float[]> getWaveforms() {
		synchronized(waveforms) {
			return this.waveforms;
		}
	}
	
	public float[] getWaveform(int trackID) {
		return waveforms.get(trackID);
	}

	public void addWaveform(int trackID, float[] waveform) {
		synchronized(waveforms) {
			waveforms.put(trackID, waveform);
		}
	}

	public boolean containsWaveform(int trackID) {
		synchronized(waveforms) {
			return waveforms.containsKey(trackID);
		}
	}

	public void setPlaylists(List<Playlist> playlists) {
		synchronized(playlists) {
			this.playlists = playlists;
		}
	}

	public List<Playlist> getPlaylists() {
		synchronized(playlists) {
			return this.playlists;
		}
	}

	public void addPlaylist(Playlist playlist) {
		synchronized(playlists) {
			playlists.add(playlist);
		}
		saveLibrary(false, false, true);
	}

	public void deletePlaylist(Playlist playlistToDelete) {
		synchronized (playlists) {
			boolean removed = playlists.remove(playlistToDelete);
			if(!removed) // deletes the playlist that is on a folder
				playlists.stream().filter(playlist -> playlist.isFolder())
					.forEach(playlist -> playlist.getContainedPlaylists().remove(playlistToDelete));
		}
		saveLibrary(false, false, true);
	}

	public boolean containsPlaylist(String playlistName) {
		synchronized (playlists) {
			List<Playlist> allPlaylists = new ArrayList<>(playlists);
			playlists.stream().filter(playlist -> playlist.isFolder())
					.forEach(folder -> allPlaylists.addAll(folder.getContainedPlaylists()));
			return allPlaylists.stream().anyMatch(playlist -> playlistName.equals(playlist.getName()));
		}
	}

	public void addToPlaylist(String folderPlaylistName, List<Integer> tracksIDs) {
		synchronized(playlists) {
			List<Playlist> notFolderPlaylistsAndChilds = playlists.stream()
					.filter(playlist -> !playlist.isFolder())
					.collect(Collectors.toList());

			playlists.stream().filter(playlist -> playlist.isFolder())
					.forEach(folder -> notFolderPlaylistsAndChilds.addAll(folder.getContainedPlaylists()));

			Playlist folderPlaylist = notFolderPlaylistsAndChilds.stream()
					.filter(p -> p.getName().equalsIgnoreCase(folderPlaylistName)).findFirst().get();

			folderPlaylist.getTracks().addAll(tracksIDs);
			folderPlaylist.changePlaylistCover();
		}
		saveLibrary(false, false, true);
	}

	public void deleteFromPlaylist(Playlist playlist, List<Integer> tracksIDs) {
		synchronized(playlists) {
			playlist.getTracks().removeAll(tracksIDs);
			showingTracks.removeIf(f -> tracksIDs.contains(f.getKey()));
			playlist.changePlaylistCover();
		}
		saveLibrary(false, false, true);
	}

	public void showPlaylist(Playlist playlist) {
		showingTracks.clear();
		synchronized(musicottTracks) {
			for(Integer id: playlist.getTracks()) {
				Map.Entry<Integer, Track> entry = new AbstractMap.SimpleEntry<>(id, musicottTracks.get(id));
				showingTracks.add(entry);
			}
		}
	}

	public ListProperty<Map.Entry<Integer, Track>> allTracksProperty() {
		return allTracksProperty;
	}

	public ListProperty<Map.Entry<Integer, Track>> showingTracksProperty() {
		return showingTracksProperty;
	}

	public void showMode(NavigationMode mode) {
		switch(mode) {
			case ALL_SONGS_MODE:
				showingTracks.clear();
				showingTracks.addAll(musicottTracks.entrySet());
				break;
		}
	}

	public void playRandomPlaylist(){
		Thread randomPlaylistThread = new Thread(() -> {
			List<Integer> randomPlaylist = new ArrayList<>();
			synchronized(musicottTracks) {
				List<Integer> trackIDs = new ArrayList<>(musicottTracks.keySet());
				Random randomGenerator = new Random();
				do {
					int rnd = randomGenerator.nextInt(trackIDs.size());
					int randomTrackID = trackIDs.get(rnd);
					Track randomTrack = musicottTracks.get(randomTrackID);
					if(randomTrack.isPlayable())
						randomPlaylist.add(randomTrackID);
				} while(randomPlaylist.size() < DEFAULT_RANDOM_QUEUE_SIZE);
			}
			PlayerFacade.getInstance().setRandomList(randomPlaylist);
		}, "Random Playlist Thread");
		randomPlaylistThread.start();
	}

	public void saveLibrary(boolean saveTracks, boolean saveWaveforms, boolean savePlaylists) {
		if(saveMusicLibraryTask == null) {
			saveMusicLibraryTask = new SaveMusicLibraryTask();
			saveMusicLibraryTask.setDaemon(true);
			saveMusicLibraryTask.start();
		}
		saveMusicLibraryTask.setSaveTracks(saveTracks);
		saveMusicLibraryTask.setSaveWaveforms(saveWaveforms);
		saveMusicLibraryTask.setSavePlaylists(savePlaylists);
		saveMusicLibraryTask.save();
	}

	@Override
	public int hashCode() {
		int hash = 71;
		synchronized(musicottTracks) {
			synchronized(waveforms) {
				synchronized (playlists) {
					hash = 73 * hash + musicottTracks.hashCode();
					hash = 73 * hash + waveforms.hashCode();
					hash = 73 * hash + playlists.hashCode();
				}
			}
		}
		return hash;
	}

	@Override
	public boolean equals(Object o) {
		boolean res;
		synchronized(musicottTracks) {
			synchronized(waveforms) {
				if(o instanceof MusicLibrary &&
						((MusicLibrary) o).getTracks().equals(musicottTracks) &&
				   		((MusicLibrary) o).getWaveforms().equals(waveforms) &&
				   		((MusicLibrary) o).getPlaylists().equals(playlists))
					res = true;
				else
					res = false;
			}
		}
		return res;
	}
}
