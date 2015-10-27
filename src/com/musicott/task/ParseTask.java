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
import java.util.ArrayList;
import java.util.List;

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

import javafx.concurrent.Task;

/**
 * @author Octavio Calleya
 *
 */
public abstract class ParseTask extends Task<List<Track>> {

	protected final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private SceneManager sc = SceneManager.getInstance();

	protected List<Track> tracks;
	protected List<File> files;
	
	public ParseTask() {
		tracks = new ArrayList<>();
	}
	
	protected void parseFiles() {
		int currentFiles = 0;
		int totalFiles = files.size();
		Track currentTrack;
		for(File file: files)
			if(isCancelled())
				break;
			else {
				currentTrack = parseFileToTrack(file);
				if(currentTrack != null)
					tracks.add(currentTrack);
				updateProgress(++currentFiles, totalFiles);
			}
	}
	
	private Track parseFileToTrack(File file) {
		MetadataParser parser = new MetadataParser(file);
		Track currentTrack = null;
		try {
			currentTrack = parser.createTrack();
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException
				| InvalidAudioFrameException e) {
			ParseException pe = new ParseException("Error parsing "+file+": "+e.getMessage(), e, file);
			LOG.error(pe.getMessage(), pe);
			ErrorHandler.getInstance().addError(pe, ErrorType.PARSE);
		}
		if(currentTrack != null) {
			String format = currentTrack.getFileFormat();
			if(format.equals("wav") || format.equals("mp3")) 
				processWaveform(currentTrack);
		}
		return currentTrack;
	}

	private void processWaveform(Track track) {
		WaveformTask waveformTask = new WaveformTask(track);
		Thread t = new Thread(waveformTask,"Waveform Task of track "+track.getTrackID());
		t.start();
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage("Parse succeeded");
		sc.getProgressImportController().setIndeterminate();
		MusicLibrary.getInstance().getTracks().addAll(tracks);
		sc.closeImportScene();
		LOG.info("Parse task completed");
	}
	
	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Parse cancelled");
		LOG.info("Parse task cancelled");
	}
}