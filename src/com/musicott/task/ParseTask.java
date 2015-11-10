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
import java.util.List;
import java.util.Queue;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.error.ParseException;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.util.MetadataParser;

import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * @author Octavio Calleya
 *
 */
public class ParseTask extends Task<List<Track>> {

	protected final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private SceneManager sc;
	private MusicLibrary ml;

	protected Thread addFilesInFolderThread;
	protected List<Track> tracks;
	protected Queue<File> files;
	protected int totalFiles;
	private int currentFiles;
	private Track currentTrack;
	
	public ParseTask(List<File> filesToParse) {
		tracks = new ArrayList<>();
		files = new ArrayDeque<>();
		files.addAll(filesToParse);
		currentFiles = 0;
		totalFiles = files.size();
		sc = SceneManager.getInstance();
		ml = MusicLibrary.getInstance();
	}
	
	@Override
	protected List<Track> call() {
		parseFiles();
		return tracks;
	}
	
	protected void addFilesToParse(List<File> newFilesToParse) {
		synchronized(files) {
			files.addAll(newFilesToParse);
			totalFiles += files.size();
			LOG.debug("Added more tracks to parse {}", newFilesToParse);
		}
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
			if(currentTrack != null) 
				tracks.add(currentTrack);
			++currentFiles;
			Platform.runLater(() -> {
				sc.getRootController().setStatusMessage("Imported "+currentFiles+" of "+totalFiles);
				sc.getRootController().setStatusProgress((double) currentFiles/totalFiles);
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
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException
				| InvalidAudioFrameException e) {
			ParseException pe = new ParseException("Error parsing "+file+": "+e.getMessage(), e, file);
			LOG.error(pe.getMessage(), pe);
			ErrorHandler.getInstance().addError(pe, ErrorType.PARSE);
		}
		return newTrack;
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage("Parse succeeded");
		ml.getTracks().addAll(tracks);
		sc.getRootController().setStatusProgress(0.0);
		sc.getRootController().setStatusMessage("Import completed");
		LOG.info("Parse task completed");
	}
	
	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Parse cancelled");
		LOG.info("Parse task cancelled");
	}
}