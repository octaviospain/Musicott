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
 * @version 0.9.1-b
 */
public class TaskDemon {

	private static final String ALREADY_IMPORTING_ERROR_MESSAGE = "There is already an import task running." +
			 													  "Wait for it to perform another import task.";
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private static TaskDemon instance;
	private ParseTask parseTask;
	private ItunesImportTask itunesImportTask;
	private BlockingQueue<Track> tracksToProcessQueue;
	private WaveformTask waveformTask;
    private SaveMusicLibraryTask saveMusicLibraryTask;

	private ObservableMap<Integer, Track> tracks;
	private Map<Integer, float[]> waveforms;
	private List<Playlist> playlists;

	private ErrorDemon errorDemon = ErrorDemon.getInstance();

	private TaskDemon() {
		tracksToProcessQueue = new LinkedBlockingQueue<>();
	}

	public static TaskDemon getInstance() {
		if (instance == null)
			instance = new TaskDemon();
		return instance;
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
	 * @param itunesLibraryPath The path where the <tt>iTunes Music Library.xml</tt> file is located.
	 */
	public void importFromItunesLibrary(String itunesLibraryPath) {
		if (isOtherImportTaskRunning())
			errorDemon.showErrorDialog(ALREADY_IMPORTING_ERROR_MESSAGE);
		else {
			itunesImportTask = new ItunesImportTask(itunesLibraryPath);
			Thread itunesThread = new Thread(itunesImportTask, "Parse Itunes Task");
			itunesThread.setDaemon(true);
			itunesThread.start();
			LOG.debug("Importing Itunes Library: {}", itunesLibraryPath);
		}
	}

	private boolean isOtherImportTaskRunning() {
		return (itunesImportTask != null && itunesImportTask.isRunning())
				|| (parseTask != null && parseTask.isRunning());
	}

	/**
	 * Creates a new {@link Thread} that analyzes and import several audio files
	 * to the application.
	 *
	 * @param filesToImport The {@link List} of the files to import.
	 * @param playAtTheEnd  Specifies whether the application should play music at the end of the importation.
	 */
	public void importFiles(List<File> filesToImport, boolean playAtTheEnd) {
		if (isOtherImportTaskRunning())
			errorDemon.showErrorDialog(ALREADY_IMPORTING_ERROR_MESSAGE);
		else {
			parseTask = new ParseTask(filesToImport, playAtTheEnd);
			Thread parseThread = new Thread(parseTask, "Parse Files Task");
			parseThread.setDaemon(true);
			parseThread.start();
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
