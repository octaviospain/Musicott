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

package com.transgressoft.musicott.tasks;

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.view.*;
import javafx.application.*;
import javafx.concurrent.*;
import javafx.util.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;

/**
 * Extends from {@link Task} to perform the operation of import several
 * audio files and add them to the {@link MusicLibrary}.
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 */
public class ParseTask extends Task<Void> {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private Map<Integer, Track> parsedTracks;
	private Queue<File> filesToParse;
	private int totalFiles;
	private int currentFiles;
	private boolean playAtTheEnd;
	private long startMillis;
	private List<String> parseErrors;

	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	private ErrorDemon errorDemon = ErrorDemon.getInstance();
	private PlayerFacade player = PlayerFacade.getInstance();
	private NavigationController navigationController = stageDemon.getNavigationController();

	public ParseTask(List<File> files, boolean playAtTheEnd) {
		super();
		parsedTracks = new HashMap<>();
		filesToParse = new ArrayDeque<>();
		parseErrors = new ArrayList<>();
		currentFiles = 0;
		this.playAtTheEnd = playAtTheEnd;
		filesToParse.addAll(files);
		totalFiles = filesToParse.size();
	}

	@Override
	protected Void call() {
		startMillis = System.currentTimeMillis();
		LOG.debug("Starting file importing");
		for (File fileToParse : filesToParse) {
			if (isCancelled())
				break;
			parseFile(fileToParse);
		}
		return null;
	}

	private void parseFile(File fileToParse) {
		Track currentTrack = parseFileToTrack(fileToParse);
		if (currentTrack != null)
			parsedTracks.put(currentTrack.getTrackId(), currentTrack);

		double progress = (double) ++ currentFiles / totalFiles;
		String statusMessage = "Imported " + Integer.toString(currentFiles) + " of " + Integer.toString(totalFiles);
		Platform.runLater(() -> updateTaskProgressOnView(progress, statusMessage));
	}

	private Track parseFileToTrack(File file) {
		Track newTrack = null;
		try {
			newTrack = MetadataParser.createTrack(file);
			LOG.debug("Parsed file {}: {}", file, newTrack);
		}
		catch (TrackParseException exception) {
			LOG.error("Error parsing file {}: ", file, exception.getCause());
			parseErrors.add(file + ": " + exception.getMessage());
		}
		return newTrack;
	}

	/**
	 * Updates on the view the progress and a message of the task.
	 * Should be called on the JavaFX Application Thread.
	 */
	private void updateTaskProgressOnView(double progress, String message) {
		navigationController.setStatusProgress(progress);
		navigationController.setStatusMessage(message);
	}

	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage("Parse succeeded");
		LOG.info("Parse task completed");

		Thread addTracksToMusicLibraryThread = new Thread(this::addTracksToMusicLibrary);
		addTracksToMusicLibraryThread.start();
		stageDemon.showIndeterminateProgress();

		if (! parseErrors.isEmpty())
			errorDemon.showExpandableErrorsDialog("Errors importing files", "", parseErrors);
		if (playAtTheEnd)
			player.addTracksToPlayQueue(parsedTracks.keySet(), true);
	}

	public void addTracksToMusicLibrary() {
		Platform.runLater(() -> updateTaskProgressOnView(- 1, ""));
		musicLibrary.addTracks(parsedTracks);
		Platform.runLater(stageDemon::closeIndeterminateProgress);

		double endMillis = System.currentTimeMillis() - startMillis;
		double totalTaskTime = Duration.millis(endMillis).toSeconds();
		String statusMessage = Integer.toString(parsedTracks.size()) + " in (" + Double
				.toString(totalTaskTime) + ") secs";
		Platform.runLater(() -> updateTaskProgressOnView(0.0, statusMessage));
	}

	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Parse cancelled");
		LOG.info("Parse task cancelled");
	}
}
