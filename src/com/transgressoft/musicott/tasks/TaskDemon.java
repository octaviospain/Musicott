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

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.transgressoft.musicott.tasks.parse.audiofiles.*;
import com.transgressoft.musicott.tasks.parse.itunes.*;
import javafx.collections.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Singleton class that isolates the creation and the information
 * flow between concurrent task threads in the application.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 */
public class TaskDemon {

	private static final String ALREADY_IMPORTING_ERROR_MESSAGE = "There is already an import task running. " +
			 													  "Wait for it to perform another import task.";
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private ExecutorService parseExecutorService;
	private Future parseFuture;
	private BaseParseTask parseTask;
	private BlockingQueue<Track> tracksToProcessQueue;
	private WaveformTask waveformTask;
    private SaveMusicLibraryTask saveMusicLibraryTask;

	private ObservableMap<Integer, Track> tracks;
	private Map<Integer, float[]> waveforms;
	private List<Playlist> playlists;

	private ErrorDemon errorDemon = ErrorDemon.getInstance();

	private static class InstanceHolder {
		static final TaskDemon INSTANCE = new TaskDemon();
		private InstanceHolder() {}
	}

	private TaskDemon() {
		tracksToProcessQueue = new LinkedBlockingQueue<>();
		parseExecutorService = Executors.newSingleThreadExecutor();
	}

	public static TaskDemon getInstance() {
		return InstanceHolder.INSTANCE;
	}

	public void shutDownTasks() {
		parseExecutorService.shutdown();
	}

	public void setMusicCollections(ObservableMap<Integer, Track> musicottTracks, Map<Integer, float[]> waveforms,
			List<Playlist> playlists) {
		tracks = musicottTracks;
		this.waveforms = waveforms;
		this.playlists = playlists;
	}

	/**
	 * Creates a new {@link Thread} that analyzes and imports the contents
	 * of an iTunes library to the application.
	 *
	 * @param itunesLibraryPath The path where the {@code iTunes Music Library.xml} file is located.
	 */
	public void importFromItunesLibrary(String itunesLibraryPath) {
		if (parseFuture != null && ! parseFuture.isDone())
			errorDemon.showErrorDialog(ALREADY_IMPORTING_ERROR_MESSAGE, "");
		else {
			parseTask = new ItunesParseTask(itunesLibraryPath);
			parseFuture = parseExecutorService.submit(parseTask);
			LOG.debug("Importing Itunes Library: {}", itunesLibraryPath);
		}
	}

	/**
	 * Creates a new {@link Thread} that analyzes and import several audio files
	 * to the application.
	 *
	 * @param filesToImport The {@link List} of the files to import.
	 * @param playAtTheEnd  Specifies whether the application should play music at the end of the importation.
	 */
	public void importFiles(List<File> filesToImport, boolean playAtTheEnd) {
		if (parseFuture != null && ! parseFuture.isDone())
			errorDemon.showErrorDialog(ALREADY_IMPORTING_ERROR_MESSAGE, "");
		else {
			parseTask = new AudioFilesParseTask(filesToImport, playAtTheEnd);
			parseFuture = parseExecutorService.submit(parseTask);
			LOG.debug("Importing {} files from folder", filesToImport.size());
		}
	}

    public void saveLibrary(boolean saveTracks, boolean saveWaveforms, boolean savePlaylists) {
        if (saveMusicLibraryTask == null) {
			saveMusicLibraryTask = new SaveMusicLibraryTask(tracks, waveforms, playlists);
			saveMusicLibraryTask.saveMusicLibrary(saveTracks, saveWaveforms, savePlaylists);
			saveMusicLibraryTask.setDaemon(true);
			saveMusicLibraryTask.start();
        }
		saveMusicLibraryTask.saveMusicLibrary(saveTracks, saveWaveforms, savePlaylists);
    }

	public void analyzeTrackWaveform(Track trackToAnalyze) {
		if (waveformTask == null) {
			waveformTask = new WaveformTask(this);
			waveformTask.start();
		}
		tracksToProcessQueue.add(trackToAnalyze);
		LOG.debug("Added track {} to waveform analyze queue", trackToAnalyze);
	}

	Track getNextTrackToAnalyzeWaveform() throws InterruptedException {
		return tracksToProcessQueue.take();
	}
}
