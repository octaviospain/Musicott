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

package com.musicott.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.ErrorHandler;
import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.player.PlayerFacade;
import com.musicott.util.MetadataParser;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.util.Duration;

/**
 * @author Octavio Calleya
 *
 */
public class ParseTask extends Task<Void> {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private SceneManager sc = SceneManager.getInstance();
	private MusicLibrary ml = MusicLibrary.getInstance();
	private ErrorHandler eh = ErrorHandler.getInstance();
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
				sc.getRootController().setStatusMessage("Imported "+currentFiles+" of "+totalFiles);
				sc.getRootController().setStatusProgress((double) ++currentFiles/totalFiles);
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
		sc.getRootController().setStatusProgress(-1);
		new Thread(() -> {
			ml.addTracks(tracks);
			totalTime = System.currentTimeMillis() - startMillis;
			Platform.runLater(() -> {
				sc.closeIndeterminatedProgressScene();
				sc.getRootController().setStatusProgress(0.0);
				sc.getRootController().setStatusMessage("Imported "+tracks.size()+" files in "+Duration.millis(totalTime).toSeconds()+" seconds");
			});
		}).start();
		sc.openIndeterminatedProgressScene();
		if(!parseErrors.isEmpty())
			eh.showExpandableErrorsDialog("Errors importing files", "", parseErrors);
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