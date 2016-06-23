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

import com.musicott.*;
import com.musicott.model.*;
import com.musicott.player.*;
import com.musicott.util.*;
import javafx.application.*;
import javafx.concurrent.*;
import javafx.util.*;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.tag.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;

/**
 * @author Octavio Calleya
 *
 */
public class ParseTask extends Task<Void> {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	private ErrorDemon errorDemon = ErrorDemon.getInstance();
	private PlayerFacade player = PlayerFacade.getInstance();

	private Map<Integer, Track> tracks;
	private Queue<File> files;
	private int totalFiles;
	private int currentFiles;
	private Track currentTrack;
	private boolean play = false;
	private long startMillis, totalTime;
	private List<String> parseErrors;
	
	public ParseTask(List<File> filesToParse, boolean playFinally) {
		tracks = new HashMap<>();
		files = new ArrayDeque<>();
		parseErrors = new ArrayList<>();
		currentFiles = 0;
		play = playFinally;
		files.addAll(filesToParse);
		totalFiles = files.size();
	}
	
	@Override
	protected Void call() {
		startMillis = System.currentTimeMillis();
		parseFiles();
		return null;
	}
	
	protected void parseFiles() {
		boolean end = false;
		File currentFile = null;
		while(!end) {
			if(isCancelled())
				break;
			synchronized(files) {
				if(!files.isEmpty())
					currentFile = files.poll();
				else
					break;
			}
			currentTrack = parseFileToTrack(currentFile);
			if(currentTrack != null) {
				tracks.put(currentTrack.getTrackID(), currentTrack);
			}
			Platform.runLater(() -> {
				stageDemon.getNavigationController().setStatusProgress((double) ++currentFiles/totalFiles);
				stageDemon.getNavigationController().setStatusMessage("Imported "+currentFiles+" of "+totalFiles);
			});
			synchronized(files) {
				if(files.isEmpty())
					end = true;
			}
		}
	}
	
	private Track parseFileToTrack(File file) {
		MetadataParser parser = new MetadataParser(file);
		Track newTrack = null;
		try {
			newTrack = parser.createTrack();
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
			LOG.error("Error parsing file "+file+": ", e);
			parseErrors.add(file+": "+e.getMessage());
		}
		return newTrack;
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage("Parse succeeded");
		stageDemon.getNavigationController().setStatusProgress(0);
		new Thread(() -> {
			musicLibrary.addTracks(tracks);
			totalTime = System.currentTimeMillis() - startMillis;
			Platform.runLater(() -> {
				stageDemon.closeIndeterminateProgress();
				stageDemon.getNavigationController().setStatusMessage(tracks.size()+" ("+ Duration.millis(totalTime).toSeconds()+") secs");
			});
		}).start();
		stageDemon.showIndeterminateProgress();
		if(!parseErrors.isEmpty())
			errorDemon.showExpandableErrorsDialog("Errors importing files", "", parseErrors);
		if(play)
			player.addTracks(tracks.keySet(), true);
		LOG.info("Parse task completed");
	}
	
	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Parse cancelled");
		LOG.info("Parse task cancelled");
	}
}
