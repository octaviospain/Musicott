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

package com.musicott.model;

import static com.musicott.MainApp.PLAYLISTS_PERSISTENCE_FILE;
import static com.musicott.MainApp.TRACKS_PERSISTENCE_FILE;
import static com.musicott.MainApp.WAVEFORMS_PERSISTENCE_FILE;
import static com.musicott.view.custom.MusicottScene.ALL_SONGS_MODE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cedarsoftware.util.io.JsonWriter;
import com.musicott.ErrorHandler;
import com.musicott.MainPreferences;
import com.musicott.SceneManager;
import com.musicott.player.PlayerFacade;
import com.musicott.view.custom.PlaylistTreeView;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 * @author Octavio Calleya
 *
 */
public class MusicLibrary {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private static int DEFAULT_RANDOMQUEUE_SIZE = 8;

	private static MusicLibrary instance;
	private ObservableMap<Integer, Track> tracks;
	private ObservableList<Map.Entry<Integer, Track>> showingTracks;
	private Map<Integer,float[]> waveforms;
	private List<Playlist> playlists;
	private ListProperty<Map.Entry<Integer, Track>> showingTracksProperty;
	private SaveLibraryTask saveLibraryTask;
	private Semaphore saveSemaphore;
	
	private MusicLibrary() {
		saveSemaphore = new Semaphore(0);
	}
	
	public static MusicLibrary getInstance() {
		if(instance == null)
			instance = new MusicLibrary();
		return instance;
	}
	
	public void setTracks(ObservableMap<Integer, Track> tracks) {
		synchronized(tracks) {
			this.tracks = tracks;
			this.showingTracks = FXCollections.observableArrayList(tracks.entrySet());
			this.showingTracksProperty = new SimpleListProperty<>();
			this.showingTracksProperty.bind(new SimpleObjectProperty<>(showingTracks));
			this.tracks.addListener((MapChangeListener.Change<? extends Integer, ? extends Track> c) -> {
				if(c.wasAdded()) {
					Track added = c.getValueAdded();
					PlaylistTreeView ptv = (PlaylistTreeView) SceneManager.getInstance().getMainStage().getScene().lookup("#playlistTreeView");
					if(ptv.getSelectionModel().selectedItemProperty().get() == null)
						Platform.runLater(() -> showingTracks.add(new AbstractMap.SimpleEntry<Integer, Track>(added.getTrackID(), added)));
				}
				else if (c.wasRemoved()) {
		          	Track removed = c.getValueRemoved();
		          	waveforms.remove(removed.getTrackID());
					PlayerFacade.getInstance().removeTrack(removed.getTrackID());
		          	showingTracks.remove(new AbstractMap.SimpleEntry<Integer, Track>(removed.getTrackID(), removed));
		          	playlists.forEach(p -> {
		          		if(p.getTracks().remove((Integer)(removed.getTrackID())))
		          			p.changePlaylistCover();
		          	});
					LOG.info("Deleted track: {}", removed);
				}
				saveLibrary(true, true, true);
			});
		}
	}
	
	public Track getTrack(int trackID) {
		synchronized(tracks) {
			return tracks.get(trackID);
		}
	}
	
	private ObservableMap<Integer, Track> getTracks(){
		synchronized(tracks) {
			return this.tracks;
		}
	}
	
	public ObservableList<Map.Entry<Integer, Track>> getShowingTracks() {
		return this.showingTracks;
	}
	
	public ListProperty<Map.Entry<Integer, Track>> getShowingTracksProperty(){
		return showingTracksProperty;
	}
	
	public void showMode(String mode) {
		switch(mode) {
			case ALL_SONGS_MODE:
				showingTracks.clear();
				showingTracks.addAll(tracks.entrySet());
				break;
		}
	}
	
	public void showPlaylist(Playlist playlist) {
		showingTracks.clear();
		synchronized(tracks) {
			if(playlist != null)
				for(Integer id: playlist.getTracks()) {
					Map.Entry<Integer, Track> entry = new AbstractMap.SimpleEntry<Integer, Track>(id, tracks.get(id));
					showingTracks.add(entry);
				}
		}
	}
	
	public void clearShowingTracks() {
		synchronized(showingTracks) {
			showingTracks.clear();
		}
	}
	
	public void addTracks(Map<Integer, Track> newTracks) {
		synchronized(tracks) {
			tracks.putAll(newTracks);
		}
	}
	
	public void removeTracks(List<Integer> selection) {
		synchronized(tracks) {
			if(!Platform.isFxApplicationThread())
				Platform.runLater(() -> tracks.keySet().removeAll(selection));
		}
		Platform.runLater(() -> SceneManager.getInstance().getRootController().setStatusMessage("Removed "+selection.size()+" tracks"));
	}
	
	public void setWaveforms(Map<Integer,float[]> waveforms) {
		synchronized(waveforms) {
			this.waveforms = waveforms;
		}
	}
	
	public float[] getWaveform(int trackID) {
		return waveforms.get(trackID);
	}
	
	public Map<Integer,float[]> getWaveforms() {
		synchronized(waveforms) {
			return this.waveforms;
		}
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
	
	public boolean containsPlaylist(String playlistName) {
		return playlists.stream().anyMatch(playlist -> playlistName.equals(playlist.getName()));
	}
	
	public void addPlaylist(Playlist playlist) {
		synchronized(playlists) {
			playlists.add(playlist);
		}
		saveLibrary(false, false, true);
	}
	
	public void removePlaylist(Playlist playlist) {
		synchronized(playlists) {
			playlists.remove(playlist);
		}
		saveLibrary(false, false, true);
	}
	
	public void addToPlaylist(String playlistName, List<Integer> tracksIDs) {
		synchronized(playlists) {
			Playlist pl = playlists.stream().filter(p -> p.getName().equalsIgnoreCase(playlistName)).findFirst().get();
			pl.getTracks().addAll(tracksIDs);
			pl.changePlaylistCover();
		}
		saveLibrary(false, false, true);
	}
	
	public void removeFromPlaylist(Playlist playlist, List<Integer> tracksIDs) {
		synchronized(playlists) {
			playlist.getTracks().removeAll(tracksIDs);
			showingTracks.removeIf(f -> tracksIDs.contains(f.getKey()));
			playlist.changePlaylistCover();
		}
		saveLibrary(false, false, true);
	}
	
	public ListProperty<Map.Entry<Integer, Track>> trackslistProperty() {
		return this.showingTracksProperty;
	}
	
	public void playRandomPlaylist(){
		Thread randomPlaylistThread = new Thread(() -> {
			List<Integer> randomPlaylist = new ArrayList<>();
			synchronized(tracks) {
				List<Integer> trackIDs = new ArrayList<>(tracks.keySet());
				Random randomGenerator = new Random();
				do {
					int rnd = randomGenerator.nextInt(trackIDs.size());
					int randomTrackID = trackIDs.get(rnd);
					Track randomTrack = tracks.get(randomTrackID);
					if(randomTrack.isPlayable())
						randomPlaylist.add(randomTrackID);
				} while(randomPlaylist.size() < DEFAULT_RANDOMQUEUE_SIZE);
			}
			PlayerFacade.getInstance().setRandomList(randomPlaylist);
		}, "Random Playlist Thread");
		randomPlaylistThread.start();
	}
	
	public void saveLibrary(boolean saveTracks, boolean saveWaveforms, boolean savePlaylists) {
		if(saveLibraryTask == null) {
			saveLibraryTask = new SaveLibraryTask();
			saveLibraryTask.setDaemon(true);
			saveLibraryTask.start();
		}
		saveLibraryTask.saveTracks = saveTracks;
		saveLibraryTask.saveWaveforms = saveWaveforms;
		saveLibraryTask.savePlaylists = savePlaylists;
		saveSemaphore.release();
	}
	
	public int hashCode() {
		int hash = 71;
		synchronized(tracks) {
			synchronized(waveforms) {
				hash = 73*hash + tracks.hashCode();
				hash = 73*hash + waveforms.hashCode();
			}
		}
		return hash;
	}
	
	public boolean equals(Object o) {
		boolean res;
		synchronized(tracks) {
			synchronized(waveforms) {
				if(o instanceof MusicLibrary &&
				   ((MusicLibrary)o).getTracks().equals(tracks) &&
				   ((MusicLibrary)o).getWaveforms().equals(waveforms))
					res = true;
				else
					res = false;
			}
		}
		return res;
	}
	
	public class SaveLibraryTask extends Thread {

		private String musicottUserPath;
		private File tracksFile, waveformsFile, playlistsFile;
		private Map<String,Object> tracksArgs, playlistArgs;
		private FileOutputStream tracksFOS, waveformsFOS, playlistsFOS;
		private JsonWriter tracksJSW, waveformsJSW, playlistsJSW;
		private volatile boolean saveTracks, saveWaveforms, savePlaylists;

		public SaveLibraryTask() {
			setName("Save Library Thread");
			musicottUserPath = "";
			
			tracksArgs = new HashMap<>();
			Map<Class<?>,List<String>> trFields = new HashMap<>();
			tracksArgs.put(JsonWriter.FIELD_SPECIFIERS, trFields);
			tracksArgs.put(JsonWriter.PRETTY_PRINT, true);
			
			List<String> trackAtributes = new ArrayList<>();
			trackAtributes.add("trackID");
			trackAtributes.add("fileFolder");
			trackAtributes.add("fileName");
			trackAtributes.add("name");
			trackAtributes.add("artist");
			trackAtributes.add("album");
			trackAtributes.add("genre");
			trackAtributes.add("comments");
			trackAtributes.add("albumArtist");
			trackAtributes.add("label");
			trackAtributes.add("size");
			trackAtributes.add("totalTime");
			trackAtributes.add("bitRate");
			trackAtributes.add("playCount");
			trackAtributes.add("trackNumber");
			trackAtributes.add("discNumber");
			trackAtributes.add("year");
			trackAtributes.add("bpm");
			trackAtributes.add("inDisk");
			trackAtributes.add("isCompilation");
			trackAtributes.add("dateModified");
			trackAtributes.add("dateAdded");
			trackAtributes.add("fileFormat");
			trackAtributes.add("hasCover");
			trackAtributes.add("isVariableBitRate");
			trackAtributes.add("encoder");
			trackAtributes.add("encoding");
			
			trFields.put(Track.class, trackAtributes);
			
			playlistArgs = new HashMap<>();
			Map<Class<?>, List<String>> plFields = new HashMap<>();
			playlistArgs.put(JsonWriter.FIELD_SPECIFIERS, plFields);
			playlistArgs.put(JsonWriter.PRETTY_PRINT, true);
			
			List<String> playlistAttribues = new ArrayList<>();
			playlistAttribues.add("name");
			playlistAttribues.add("tracksID");
			
			plFields.put(Playlist.class, playlistAttribues);
		}
		
		@Override
		public void run() {
			try {
				while(true) {
					saveSemaphore.acquire();
					checkMusicottFiles();
					if(saveTracks) {
						LOG.debug("Saving list of tracks in {}", tracksFile);
						tracksFOS = new FileOutputStream(tracksFile);
						tracksJSW = new JsonWriter(tracksFOS, tracksArgs);
						saveTracks = false;
						synchronized(tracks) {
							tracksJSW.write(tracks);
						}
						tracksFOS.close();
						tracksJSW.close();
					}
					// Save the map of waveforms
					if(saveWaveforms) {
						LOG.debug("Saving waveform images in {}", waveformsFile);
						waveformsFOS = new FileOutputStream(waveformsFile);
						waveformsJSW = new JsonWriter(waveformsFOS);
						saveWaveforms = false;
						synchronized(waveforms) {
							waveformsJSW.write(waveforms);
						}
						waveformsFOS.close();
						waveformsJSW.close();
					}
					// Save the playlists
					if(savePlaylists) {
						LOG.debug("Saving playlists in {}", playlistsFile);
						playlistsFOS = new FileOutputStream(playlistsFile);
						playlistsJSW = new JsonWriter(playlistsFOS, playlistArgs);
						savePlaylists = false;
						synchronized(playlists){
							playlistsJSW.write(playlists);
						}
						playlistsFOS.close();
						playlistsJSW.close();
					}
				}
			} catch (IOException | RuntimeException | InterruptedException e) {
				Platform.runLater(() -> {
					LOG.error("Error saving music library", e);
					ErrorHandler.getInstance().showErrorDialog("Error saving music library", null, e);
				});
			}
		}
		
		private void checkMusicottFiles() throws FileNotFoundException {
			String newPath = MainPreferences.getInstance().getMusicottUserFolder();
			if(!newPath.equals(musicottUserPath)) {	// Musicott folder has changed
				tracksFile = new File(newPath+File.separator+TRACKS_PERSISTENCE_FILE);
				waveformsFile = new File(newPath+File.separator+WAVEFORMS_PERSISTENCE_FILE);
				playlistsFile = new File(newPath+File.separator+PLAYLISTS_PERSISTENCE_FILE);
				musicottUserPath = newPath;
			}
		}
	}
}