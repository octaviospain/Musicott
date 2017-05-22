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
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package com.transgressoft.musicott.tasks;

import com.cedarsoftware.util.io.*;
import com.google.inject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.view.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static com.transgressoft.musicott.model.CommonObject.*;

/**
 * Extends from {@link Thread} to perform the operation of save data of the {@link MusicLibrary} in the filesystem.
 * <p>It waits for a {@link Semaphore} to perform the task in a endless loop, instead of finishing the execution for
 * each save request.</p>
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class SaveMusicLibraryTask extends Thread {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final TracksLibrary tracksLibrary;
    private final PlaylistsLibrary playlistsLibrary;
    private final WaveformsLibrary waveformsLibrary;

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
    private boolean finish = false;
    @Inject
    private Injector injector;
    private ErrorDialogController errorDialog;

    @Inject
    public SaveMusicLibraryTask(TracksLibrary tracksLibrary, PlaylistsLibrary playlistsLibrary,
            WaveformsLibrary waveformsLibrary) {
        setName("Save Library Thread");
        this.tracksLibrary = tracksLibrary;
        this.playlistsLibrary = playlistsLibrary;
        this.waveformsLibrary = waveformsLibrary;
        musicottUserPath = "";
        saveSemaphore = new Semaphore(0);
        tracksArgs = new HashMap<>();
        playlistArgs = new HashMap<>();
        buildTracksJsonArguments();
        buildPlaylistsJsonArguments();
    }

    private void buildTracksJsonArguments() {
        List<String> trackAttributes = new ArrayList<>();
        trackAttributes.add("trackId");
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
        trackAttributes.add("isInDisk");
        trackAttributes.add("isPartOfCompilation");
        trackAttributes.add("lastDateModified");
        trackAttributes.add("dateAdded");
        trackAttributes.add("fileFormat");
        trackAttributes.add("isVariableBitRate");
        trackAttributes.add("encoder");
        trackAttributes.add("encoding");
        trackAttributes.add("artistsInvolved");

        Map<Class<?>, List<String>> trackFields = new HashMap<>();
        trackFields.put(Track.class, trackAttributes);

        tracksArgs.put(JsonWriter.FIELD_SPECIFIERS, trackFields);
        tracksArgs.put(JsonWriter.PRETTY_PRINT, true);
    }

    private void buildPlaylistsJsonArguments() {
        List<String> playlistAttributes = new ArrayList<>();
        playlistAttributes.add("name");
        playlistAttributes.add("playlistTrackIds");
        playlistAttributes.add("containedPlaylists");
        playlistAttributes.add("isFolder");

        Map<Class<?>, List<String>> playlistsFields = new HashMap<>();
        playlistsFields.put(Playlist.class, playlistAttributes);

        playlistArgs.put(JsonWriter.FIELD_SPECIFIERS, playlistsFields);
        playlistArgs.put(JsonWriter.PRETTY_PRINT, true);
    }

    public void saveMusicLibrary(boolean saveTracks, boolean saveWaveforms, boolean savePlaylists) {
        this.saveTracks = saveTracks;
        this.saveWaveforms = saveWaveforms;
        this.savePlaylists = savePlaylists;
        if (saveSemaphore.availablePermits() < 1)
            saveSemaphore.release();
    }

    public void finish() {
        finish = true;
        saveSemaphore.release();
    }

    @Override
    public void run() {
        try {
            while (true) {
                saveSemaphore.acquire();
                if (finish)
                    break;
                checkMusicottUserPathChanged();

                if (saveTracks)
                    serializeTracks();
                if (saveWaveforms)
                    serializeWaveforms();
                if (savePlaylists)
                    serializePlaylists();
            }
        }
        catch (IOException | InterruptedException exception) {
            LOG.error("Error saving music library: {}", exception.getMessage(), exception);
            errorDialog.show("Error saving music library", null, exception);
        }
    }

    private void checkMusicottUserPathChanged() {
        MainPreferences mainPreferences = injector.getInstance(MainPreferences.class);
        String applicationPath = mainPreferences.getMusicottUserFolder();
        if (! applicationPath.equals(musicottUserPath)) {
            tracksFile = new File(applicationPath, TRACKS_FILE.toString());
            waveformsFile = new File(applicationPath, WAVEFORMS_FILE.toString());
            playlistsFile = new File(applicationPath, PLAYLISTS_FILE.toString());
            musicottUserPath = applicationPath;
        }
    }

    private void serializeTracks() throws IOException {
        synchronized (tracksLibrary) {
            writeObjectToJsonFile(tracksLibrary.getMusicottTracks(), tracksFile, tracksArgs);
        }
        saveTracks = false;
        LOG.debug("Saved list of tracks in {}", tracksFile);
    }

    private void serializeWaveforms() throws IOException {
        synchronized (waveformsLibrary) {
            writeObjectToJsonFile(waveformsLibrary.getWaveforms(), waveformsFile, null);
        }
        saveWaveforms = false;
        LOG.debug("Saved waveform images in {}", waveformsFile);
    }

    private void serializePlaylists() throws IOException {
        synchronized (playlistsLibrary) {
            writeObjectToJsonFile(playlistsLibrary.getPlaylistsTree(), playlistsFile, playlistArgs);
        }
        savePlaylists = false;
        LOG.debug("Saved playlists in {}", playlistsFile);
    }

    /**
     * Writes an {@code Object} into a JSON formatted file using Json-IO
     *
     * @param object   The {@code Object} to write
     * @param jsonFile The {@link File} where to write the {@code Object}
     * @param args     The arguments to pass to the {@link JsonWriter}
     *
     * @throws IOException If something went bad
     * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
     */
    private void writeObjectToJsonFile(Object object, File jsonFile, Map<String, Object> args) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(jsonFile);
        JsonWriter jsonWriter = new JsonWriter(outputStream, args);
        jsonWriter.write(object);
        outputStream.close();
        jsonWriter.close();
    }

    public void setErrorDialog(ErrorDialogController errorDialog) {
        this.errorDialog = errorDialog;
    }
}
