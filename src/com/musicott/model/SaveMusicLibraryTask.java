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

import com.cedarsoftware.util.io.*;
import com.musicott.*;
import javafx.application.*;
import javafx.collections.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static com.musicott.MainApp.*;

/**
 * @author Octavio Calleya
 *
 */
public class SaveMusicLibraryTask extends Thread {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private MusicLibrary musicLibrary = MusicLibrary.getInstance();

	private ObservableMap<Integer, Track> musicottTracks = musicLibrary.getTracks();
	private Map<Integer,float[]> waveforms = musicLibrary.getWaveforms();
	private List<Playlist> playlists = musicLibrary.getPlaylists();

	private String musicottUserPath;
	private File tracksFile, waveformsFile, playlistsFile;
	private Map<String,Object> tracksArgs, playlistArgs;
	private Semaphore saveSemaphore;
	private volatile boolean saveTracks, saveWaveforms, savePlaylists;

	public SaveMusicLibraryTask() {
		setName("Save Library Thread");
		musicottUserPath = "";
		saveSemaphore = new Semaphore(0);
		tracksArgs = new HashMap<>();
		playlistArgs = new HashMap<>();
		buildTracksJsonArguments();
		buildPlaylistsJsonArguments();
	}

	@Override
	public void run() {
		try {
			while(true) {
				saveSemaphore.acquire();
				checkMusicottFiles();

				if(saveTracks)
					serializeTracks();
				if(saveWaveforms)
					serializeWaveforms();
				if(savePlaylists)
					serializePlaylists();
			}
		} catch (IOException | RuntimeException | InterruptedException e) {
			Platform.runLater(() -> {
				LOG.error("Error saving music library", e);
				ErrorDemon.getInstance().showErrorDialog("Error saving music library", null, e);
			});
		}
	}

	public void save() {
		saveSemaphore.release();
	}

	public void setSaveTracks(boolean saveTracks) {
		this.saveTracks = saveTracks;
	}

	public void setSaveWaveforms(boolean saveWaveforms) {
		this.saveWaveforms = saveWaveforms;
	}

	public void setSavePlaylists(boolean savePlaylists) {
		this.savePlaylists = savePlaylists;
	}

	private void buildTracksJsonArguments() {
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

		Map<Class<?>,List<String>> trackFields = new HashMap<>();
		trackFields.put(Track.class, trackAtributes);

		tracksArgs.put(JsonWriter.FIELD_SPECIFIERS, trackFields);
		tracksArgs.put(JsonWriter.PRETTY_PRINT, true);
	}

	private void buildPlaylistsJsonArguments() {
		List<String> playlistAttribues = new ArrayList<>();
		playlistAttribues.add("name");
		playlistAttribues.add("tracksID");
		playlistAttribues.add("containedPlaylists");
		playlistAttribues.add("isFolder");

		Map<Class<?>, List<String>> playlistsFields = new HashMap<>();
		playlistsFields.put(Playlist.class, playlistAttribues);

		playlistArgs.put(JsonWriter.FIELD_SPECIFIERS, playlistsFields);
		playlistArgs.put(JsonWriter.PRETTY_PRINT, true);
	}

	private void checkMusicottFiles() throws FileNotFoundException {
		String newPath = MainPreferences.getInstance().getMusicottUserFolder();
		if(!newPath.equals(musicottUserPath)) {
			tracksFile = new File(newPath + File.separator + TRACKS_PERSISTENCE_FILE);
			waveformsFile = new File(newPath + File.separator + WAVEFORMS_PERSISTENCE_FILE);
			playlistsFile = new File(newPath + File.separator + PLAYLISTS_PERSISTENCE_FILE);
			musicottUserPath = newPath;
		}
	}

	private void serializeTracks() throws IOException {
		LOG.debug("Saving list of tracks in {}", tracksFile);
		FileOutputStream tracksFOS = new FileOutputStream(tracksFile);
		JsonWriter tracksJSW = new JsonWriter(tracksFOS, tracksArgs);
		saveTracks = false;
		synchronized(musicottTracks) {
			tracksJSW.write(musicottTracks);
		}
		tracksFOS.close();
		tracksJSW.close();
	}

	private void serializeWaveforms() throws IOException {
		LOG.debug("Saving waveform images in {}", waveformsFile);
		FileOutputStream waveformsFOS = new FileOutputStream(waveformsFile);
		JsonWriter waveformsJSW = new JsonWriter(waveformsFOS);
		saveWaveforms = false;
		synchronized(waveforms) {
			waveformsJSW.write(waveforms);
		}
		waveformsFOS.close();
		waveformsJSW.close();
	}

	private void serializePlaylists() throws IOException {
		LOG.debug("Saving playlists in {}", playlistsFile);
		FileOutputStream playlistsFOS = new FileOutputStream(playlistsFile);
		JsonWriter playlistsJSW = new JsonWriter(playlistsFOS, playlistArgs);
		savePlaylists = false;
		synchronized(playlists){
			playlistsJSW.write(playlists);
		}
		playlistsFOS.close();
		playlistsJSW.close();
	}
}
