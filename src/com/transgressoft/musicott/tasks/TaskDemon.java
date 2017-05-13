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

import com.google.inject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.transgressoft.musicott.util.factories.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Singleton class that isolates the creation and the information
 * flow between concurrent task threads in the application.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
@Singleton
public class TaskDemon {

	private static final String ALREADY_IMPORTING_ERROR_MESSAGE = "There is already an import task running. " +
			 													  "Wait for it to perform another import task.";
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private final Provider<WaveformTask> waveformTaskProvider;
	private final SaveMusicLibraryTask saveMusicLibraryTask;
	private final Provider<ErrorDemon> errorDemon;

	private ExecutorService parseExecutorService;
	private Future parseFuture;
	private WaveformTask waveformTask;
	private ParseTask parseTask;
	private BlockingQueue<Track> tracksToProcessQueue;

	private boolean savingsActivated = true;

	@Inject
	private ParseTaskFactory parseTaskFactory;

	@Inject
	public TaskDemon(Provider<SaveMusicLibraryTask> saveMusicLibraryTask, Provider<WaveformTask> waveformTaskProvider,
			Provider<ErrorDemon> errorDemon) {
		this.errorDemon = errorDemon;
		this.waveformTaskProvider = waveformTaskProvider;
		this.saveMusicLibraryTask = saveMusicLibraryTask.get();
		this.saveMusicLibraryTask.setDaemon(true);
		tracksToProcessQueue = new LinkedBlockingQueue<>();
		parseExecutorService = Executors.newSingleThreadExecutor();
	}

	public void deactivateLibrarySaving() {
		savingsActivated = false;
	}

	public void activateLibrarySaving() {
		savingsActivated = true;
	}

	public void shutDownTasks() {
		parseExecutorService.shutdown();
	}

	/**
	 * Creates a new {@link Thread} that analyzes and imports the contents
	 * of an iTunes library to the application.
	 *
	 * @param itunesLibraryPath The path where the {@code iTunes Music Library.xml} file is located.
	 */
	public void importFromItunesLibrary(String itunesLibraryPath) {
		if (parseFuture != null && ! parseFuture.isDone())
			errorDemon.get().showErrorDialog(ALREADY_IMPORTING_ERROR_MESSAGE, "");
		else {
			parseTask = parseTaskFactory.create(itunesLibraryPath);
			parseFuture = parseExecutorService.submit(parseTask);
			LOG.debug("Importing Itunes Library: {}", itunesLibraryPath);
		}
	}

	/**
	 * Creates a new {@link Thread} that analyzes and import several audio files
	 * to the application.
	 *
	 * @param filesToImport The {@link List} of the files to import.
	 */
	public void importFiles(List<File> filesToImport, boolean playAtTheEnd) {
		if (parseFuture != null && ! parseFuture.isDone())
			errorDemon.get().showErrorDialog(ALREADY_IMPORTING_ERROR_MESSAGE, "");
		else {
			parseTask = parseTaskFactory.create(filesToImport, playAtTheEnd);
			parseFuture = parseExecutorService.submit(parseTask);
			LOG.debug("Importing {} files from folder", filesToImport.size());
		}
	}

    public void saveLibrary(boolean saveTracks, boolean saveWaveforms, boolean savePlaylists) {
		if (savingsActivated) {
			if (! saveMusicLibraryTask.isAlive())
				saveMusicLibraryTask.start();
			saveMusicLibraryTask.saveMusicLibrary(saveTracks, saveWaveforms, savePlaylists);
		}
    }

	public void analyzeTrackWaveform(Track trackToAnalyze) {
		if (waveformTask == null) {
			waveformTask = waveformTaskProvider.get();
			waveformTask.start();
		}
		tracksToProcessQueue.add(trackToAnalyze);
		LOG.debug("Added track {} to waveform analyze queue", trackToAnalyze);
	}

	Track getNextTrackToAnalyzeWaveform() throws InterruptedException {
		return tracksToProcessQueue.take();
	}
}
