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

package com.musicott.tasks;

import com.cedarsoftware.util.io.*;
import com.musicott.*;
import com.musicott.model.*;
import javafx.application.*;
import javafx.collections.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static com.musicott.MainApp.*;

/**
 * Extends from {@link Thread} to perform the operation of save data
 * of the {@link MusicLibrary} in the filesystem. It waits for a {@link Semaphore}
 * to perform the task in a endless loop, instead of finishing the execution
 * for each save request.
 *
 * @author Octavio Calleya
 * @version 0.9
 */
public class SaveMusicLibraryTask extends Thread {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private String musicottUserPath;
	private File tracksFile;
	private File waveformsFile;
	private File playlistsFile;
	private Map<String, Object> tracksArgs;
	private Map<String, Object> playlistArgs;
	private Semaphore saveSemaphore;
	private volatile boolean saveTracks;
	private volatile boolean saveWaveforms;
	private volatile boolean savePlaylists;

	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	private ErrorDemon errorDemon = ErrorDemon.getInstance();

	private ObservableMap<Integer, Track> musicottTracks = musicLibrary.getTracks();
	private Map<Integer, float[]> waveforms = musicLibrary.getWaveforms();
	private List<Playlist> playlists = musicLibrary.getPlaylists();

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
				checkMusicottUserPathChanged();

				if(saveTracks)
					serializeTracks();
				if(saveWaveforms)
					serializeWaveforms();
				if(savePlaylists)
					serializePlaylists();
			}
		} catch (IOException | RuntimeException | InterruptedException exception) {
			Platform.runLater(() -> {
				LOG.error("Error saving music library", exception.getCause());
				errorDemon.showErrorDialog("Error saving music library", "", exception);
			});
		}
	}

	public void saveMusicLibrary() {
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
		List<String> trackAttributes = new ArrayList<>();
		trackAttributes.add("trackID");
		trackAttributes.add("fileFolder");
		trackAttributes.add("fileName");
		trackAttributes.add("name");
		trackAttributes.add("artist");
		trackAttributes.add("album");
		trackAttributes.add("genre");
		trackAttributes.add("comments");
		trackAttributes.add("albumArtist");
		trackAttributes.add("label");
		trackAttributes.add("size");
		trackAttributes.add("totalTime");
		trackAttributes.add("bitRate");
		trackAttributes.add("playCount");
		trackAttributes.add("trackNumber");
		trackAttributes.add("discNumber");
		trackAttributes.add("year");
		trackAttributes.add("bpm");
		trackAttributes.add("inDisk");
		trackAttributes.add("isPartOfCompilation");
		trackAttributes.add("lastDateModified");
		trackAttributes.add("dateAdded");
		trackAttributes.add("fileFormat");
		trackAttributes.add("isVariableBitRate");
		trackAttributes.add("encoder");
		trackAttributes.add("encoding");

		Map<Class<?>,List<String>> trackFields = new HashMap<>();
		trackFields.put(Track.class, trackAttributes);

		tracksArgs.put(JsonWriter.FIELD_SPECIFIERS, trackFields);
		tracksArgs.put(JsonWriter.PRETTY_PRINT, true);
	}

	private void buildPlaylistsJsonArguments() {
		List<String> playlistAttributes = new ArrayList<>();
		playlistAttributes.add("name");
		playlistAttributes.add("tracksIds");
		playlistAttributes.add("containedPlaylists");
		playlistAttributes.add("isFolder");

		Map<Class<?>, List<String>> playlistsFields = new HashMap<>();
		playlistsFields.put(Playlist.class, playlistAttributes);

		playlistArgs.put(JsonWriter.FIELD_SPECIFIERS, playlistsFields);
		playlistArgs.put(JsonWriter.PRETTY_PRINT, true);
	}

	private void checkMusicottUserPathChanged() throws FileNotFoundException {
		String applicationPath = MainPreferences.getInstance().getMusicottUserFolder();
		if(!applicationPath.equals(musicottUserPath)) {
			String sep = File.separator;
			tracksFile = new File(applicationPath + sep + TRACKS_PERSISTENCE_FILE);
			waveformsFile = new File(applicationPath + sep + WAVEFORMS_PERSISTENCE_FILE);
			playlistsFile = new File(applicationPath + sep + PLAYLISTS_PERSISTENCE_FILE);
			musicottUserPath = applicationPath;
		}
	}

	private void serializeTracks() throws IOException {
		FileOutputStream tracksFileOutputStream = new FileOutputStream(tracksFile);
		JsonWriter tracksJsonWriter = new JsonWriter(tracksFileOutputStream, tracksArgs);
		saveTracks = false;
		synchronized(musicottTracks) {
			tracksJsonWriter.write(musicottTracks);
		}
		LOG.debug("Saved list of tracks in {}", tracksFile);
		tracksFileOutputStream.close();
		tracksJsonWriter.close();
	}

	private void serializeWaveforms() throws IOException {
		FileOutputStream waveformsFileOutputStream = new FileOutputStream(waveformsFile);
		JsonWriter waveformsJsonWriter = new JsonWriter(waveformsFileOutputStream);
		saveWaveforms = false;
		synchronized(waveforms) {
			waveformsJsonWriter.write(waveforms);
		}
		LOG.debug("Saved waveform images in {}", waveformsFile);
		waveformsFileOutputStream.close();
		waveformsJsonWriter.close();
	}

	private void serializePlaylists() throws IOException {
		FileOutputStream playlistsFileOutputStream = new FileOutputStream(playlistsFile);
		JsonWriter playlistsJsonWriter = new JsonWriter(playlistsFileOutputStream, playlistArgs);
		savePlaylists = false;
		synchronized(playlists) {
			playlistsJsonWriter.write(playlists);
		}
		LOG.debug("Saved playlists in {}", playlistsFile);
		playlistsFileOutputStream.close();
		playlistsJsonWriter.close();
	}
}