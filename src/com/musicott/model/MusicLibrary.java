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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cedarsoftware.util.io.JsonWriter;
import com.musicott.MainPreferences;
import com.musicott.SceneManager;
import com.musicott.error.ErrorHandler;
import com.musicott.player.PlayerFacade;

import javafx.application.Platform;
import javafx.collections.MapChangeListener;
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
	private Map<Integer,float[]> waveforms;
	private SaveLibraryTask saveLibraryTask;
	private volatile boolean save = true;
	
	private MusicLibrary() {
	}
	
	public static MusicLibrary getInstance() {
		if(instance == null)
			instance = new MusicLibrary();
		return instance;
	}
	
	public void setTracks(ObservableMap<Integer, Track> tracks) {
		synchronized(tracks) {
			this.tracks = tracks;
			this.tracks.addListener((MapChangeListener.Change<? extends Integer, ? extends Track> c) -> {
				boolean waveformsMapChanged = false;
				if(c.wasRemoved()) {
					waveformsMapChanged = removeWaveform(c.getKey());
					Platform.runLater(() -> PlayerFacade.getInstance().removeTrack(c.getKey()));
				}
				saveLibrary(true, waveformsMapChanged);
			});
		}
	}
	
	public Track getTrack(int trackID) {
		synchronized(tracks) {
			return tracks.get(trackID);
		}
	}
	
	public ObservableMap<Integer, Track> getTracks(){
		synchronized(tracks) {
			return this.tracks;
		}
	}
	
	public void addTracks(Map<Integer, Track> newTracks) {
		synchronized(tracks) {
			tracks.putAll(newTracks);
		}
	}
	
	public void removeTracks(List<Integer> selection) {
		synchronized(tracks) {
				tracks.keySet().removeAll(selection);
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
	
	private Map<Integer,float[]> getWaveforms() {
		synchronized(waveforms) {
			return this.waveforms;
		}
	}
	
	public boolean removeWaveform(int trackID) {
		synchronized(waveforms) {
			return waveforms.remove(trackID, waveforms.get(trackID));
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
	
	public void saveLibrary(boolean saveTracks, boolean saveWaveforms) {
		if(saveLibraryTask == null || (saveLibraryTask != null && !saveLibraryTask.isAlive())) {
			saveLibraryTask = new SaveLibraryTask(saveTracks, saveWaveforms);
			saveLibraryTask.setDaemon(true);
			saveLibraryTask.start();
		}
		else {
			save = true;
			saveLibraryTask.saveTracks = saveTracks;
			saveLibraryTask.saveWaveforms = saveWaveforms;
		}
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

		private File tracksFile, waveformsFile;
		private Map<String,Object> args;
		private boolean saveTracks, saveWaveforms;

		public SaveLibraryTask(boolean saveTracks, boolean saveWaveforms) {
			setName("Save Library Thread");
			this.saveTracks = saveTracks;
			this.saveWaveforms = saveWaveforms;
			String musicottUserPath = MainPreferences.getInstance().getMusicottUserFolder();
			tracksFile = new File(musicottUserPath+"/Musicott-tracks.json");
			waveformsFile = new File(musicottUserPath+"/Musicott-waveforms.json");
			
			args = new HashMap<>();
			Map<Class<?>,List<String>> fields = new HashMap<>();
			args.put(JsonWriter.FIELD_SPECIFIERS, fields);
			args.put(JsonWriter.PRETTY_PRINT, true);
			
			List<String> fieldNames = new ArrayList<>();
			fieldNames.add("trackID");
			fieldNames.add("fileFolder");
			fieldNames.add("fileName");
			fieldNames.add("name");
			fieldNames.add("artist");
			fieldNames.add("album");
			fieldNames.add("genre");
			fieldNames.add("comments");
			fieldNames.add("albumArtist");
			fieldNames.add("label");
			fieldNames.add("size");
			fieldNames.add("totalTime");
			fieldNames.add("bitRate");
			fieldNames.add("playCount");
			fieldNames.add("trackNumber");
			fieldNames.add("discNumber");
			fieldNames.add("year");
			fieldNames.add("bpm");
			fieldNames.add("inDisk");
			fieldNames.add("isCompilation");
			fieldNames.add("dateModified");
			fieldNames.add("dateAdded");
			fieldNames.add("fileFormat");
			fieldNames.add("hasCover");
			fieldNames.add("isVariableBitRate");
			fieldNames.add("encoder");
			fieldNames.add("encoding");
			
			fields.put(Track.class,fieldNames);
		}
		
		@Override
		public void run() {
			try {
				// Save the list of tracks, covers, and the sequence object
				FileOutputStream fos;
				JsonWriter jsw;
				while(save) {
					save = false;
					if(saveTracks) {
						LOG.debug("Saving list of tracks in {}", tracksFile);
						fos = new FileOutputStream(tracksFile);				
						jsw = new JsonWriter(fos, args);
						synchronized(tracks) {
							jsw.write(tracks);
						}
						fos.close();
						jsw.close();
					}
					// Save the map of waveforms
					if(saveWaveforms) {
						LOG.debug("Saving waveform images in {}", waveformsFile);
						fos = new FileOutputStream(waveformsFile);
						jsw = new JsonWriter(fos);
						synchronized(waveforms) {
							jsw.write(waveforms);
						}
						fos.close();
						jsw.close();
					}
				}
			}
			catch (IOException |RuntimeException e) {
				Platform.runLater(() -> {
					LOG.error("Error saving music library", e);
					ErrorHandler.getInstance().showErrorDialog("Error saving music library", null, e);
				});
			}
		}
	}
}