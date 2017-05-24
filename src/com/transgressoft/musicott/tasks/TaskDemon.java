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
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.transgressoft.musicott.tasks.parse.itunes.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.view.*;
import com.worldsworstsoftware.itunes.*;
import javafx.beans.value.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Singleton class that isolates the creation and the information
 * flow between concurrent task threads in the application.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 */
@Singleton
public class TaskDemon {

	private static final String ALREADY_IMPORTING_ERROR_MESSAGE = "There is already an import task running. " +
			 													  "Wait for it to perform another import task.";
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private SaveMusicLibraryTask saveMusicLibraryTask;

	private ExecutorService parseExecutorService;
	private Future parseFuture;
	private ParseTaskFactory parseTaskFactory;
	private ParseTask parseTask;
	private WaveformTask waveformTask;
	private BlockingQueue<Track> tracksToProcessQueue;
	private ErrorDialogController errorDialog;

	private boolean savingsActivated = true;

	@Inject
	public TaskDemon() {
		tracksToProcessQueue = new LinkedBlockingQueue<>();
		parseExecutorService = Executors.newSingleThreadExecutor();
	}

	public void deactivateSaveLibrary() {
		savingsActivated = false;
	}

	public void activateSaveLibrary() {
		savingsActivated = true;
	}

	public synchronized void shutDownTasks() {
		parseExecutorService.shutdown();
		saveMusicLibraryTask.finish();
	}

	/**
	 * Creates a new {@link ParseTask} that analyzes and imports the contents
	 * of an iTunes library to the application.
	 *
	 * @param itunesLibraryPath The path where the {@code iTunes Music Library.xml} file is located.
	 */
	public synchronized void importFromItunesLibrary(String itunesLibraryPath) {
		if (parseFuture != null && ! parseFuture.isDone())
			errorDialog.show(ALREADY_IMPORTING_ERROR_MESSAGE, "");
		else {
			parseTask = parseTaskFactory.create(itunesLibraryPath);
			parseFuture = parseExecutorService.submit(parseTask);
			LOG.debug("Importing Itunes Library: {}", itunesLibraryPath);
		}
	}

	public synchronized void setItunesPlaylistsToImport(List<ItunesPlaylist> itunesPlaylists) {
		if (parseFuture != null && ! parseFuture.isDone() && parseTask instanceof ItunesParseTask)
			((ItunesParseTask) parseTask).setItunesPlaylistsToParse(itunesPlaylists);
	}

	public synchronized void cancelItunesImport() {
		if (parseFuture != null && ! parseFuture.isDone())
			parseFuture.cancel(true);
	}

	/**
	 * Creates a new {@link ParseTask} that analyzes and import several audio files
	 * to the application.
	 *
	 * @param filesToImport The {@link List} of the files to import.
	 */
	public synchronized void importFiles(List<File> filesToImport, boolean playAtTheEnd) {
		if (parseFuture != null && ! parseFuture.isDone())
			errorDialog.show(ALREADY_IMPORTING_ERROR_MESSAGE, "");
		else {
			parseTask = parseTaskFactory.create(filesToImport, playAtTheEnd);
			parseFuture = parseExecutorService.submit(parseTask);
			LOG.debug("Importing {} files from folder", filesToImport.size());
		}
	}

    public synchronized void saveLibrary(boolean saveTracks, boolean saveWaveforms, boolean savePlaylists) {
		if (savingsActivated) {
			if (! saveMusicLibraryTask.isAlive())
				saveMusicLibraryTask.start();
			saveMusicLibraryTask.saveMusicLibrary(saveTracks, saveWaveforms, savePlaylists);
		}
    }

	public void analyzeTrackWaveform(Track trackToAnalyze) {
		if (! waveformTask.isAlive())
			waveformTask.start();
		tracksToProcessQueue.add(trackToAnalyze);
		LOG.debug("Added track {} to waveform analyze queue", trackToAnalyze);
	}

	@Inject (optional = true)
	public void setParseTaskFactory(ParseTaskFactory parseTaskFactory) {
		this.parseTaskFactory = parseTaskFactory;
	}

	@Inject
	public void setSaveMusicLibraryTask(SaveMusicLibraryTask saveMusicLibraryTask) {
		this.saveMusicLibraryTask = saveMusicLibraryTask;
		saveMusicLibraryTask.setDaemon(true);
	}

	@Inject
	public void setWaveformTaskFactory(WaveformTaskFactory waveformTaskFactory) {
		waveformTask = waveformTaskFactory.create(tracksToProcessQueue);
	}

	@Inject
	public void setTracksLibraryChangeListener(TracksLibrary tracksLibrary) {
		tracksLibrary.setListener(change -> saveLibrary(true, false, false));
	}

	@Inject
	public void setWaveformsLibraryChangeListener(WaveformsLibrary waveformsLibrary) {
		waveformsLibrary.setListener(change -> saveLibrary(false, true, false));
	}

	@Inject
	public void setPlaylistsLibraryChangeListener(PlaylistsLibrary playlistsLibrary) {
		playlistsLibrary.setPlaylistTracksListener(change -> saveLibrary(false, false, true));
		playlistsLibrary.setPlaylistNameListener((obs, o, n) -> saveLibrary(false, false, true));
	}

	@Inject (optional = true)
	public void setErrorDialog(@ErrorCtrl	 ErrorDialogController errorDialog) {
		this.errorDialog = errorDialog;
		saveMusicLibraryTask.setErrorDialog(errorDialog);
		waveformTask.setErrorDialog(errorDialog);
	}

	public ChangeListener<Number> getIncrementPlayCountChangeListener() {
		return (obs, o, n) -> saveLibrary(true, false, false);
	}

	public ChangeListener<String> getUserFolderListener() {
		return (obs, o, n) -> saveLibrary(true, true, true);
	}
}
