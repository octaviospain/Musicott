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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import com.cedarsoftware.util.io.JsonWriter;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class SaveLibraryTask extends Thread {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private ObservableList<Track> trackList;
	private Map<Integer, float[]> waveformsMap;
	private File tracksFile, waveformsFile, sequenceFile;
	private AtomicInteger trackSequence;
	private Map<String,Object> args;
	private boolean saveTracks, saveWaveforms;
	
	private MusicLibrary ml = MusicLibrary.getInstance();

	public SaveLibraryTask(boolean saveTracks, boolean saveWaveforms) {
		this.saveTracks = saveTracks;
		this.saveWaveforms = saveWaveforms;
		tracksFile = new File("./resources/tracks.json");
		waveformsFile = new File("./resources/waveforms.json");
		sequenceFile = new File("./resources/seq.json");
		trackList = ml.getTracks();
		waveformsMap = ml.getWaveforms();
		trackSequence = ml.getTrackSequence();
		
		args = new HashMap<>();
		Map<Class<?>,List<String>> fields = new HashMap<>();
		args.put(JsonWriter.FIELD_SPECIFIERS, fields);
		args.put(JsonWriter.PRETTY_PRINT, true);
		
		List<String> fieldNames = new ArrayList<>();
		fieldNames.add("trackID");
		fieldNames.add("fileFolder");
		fieldNames.add("fileName");
		fieldNames.add("name");
		fieldNames.add("artist");
		fieldNames.add("album");
		fieldNames.add("genre");
		fieldNames.add("comments");
		fieldNames.add("albumArtist");
		fieldNames.add("label");
		fieldNames.add("size");
		fieldNames.add("totalTime");
		fieldNames.add("bitRate");
		fieldNames.add("playCount");
		fieldNames.add("trackNumber");
		fieldNames.add("discNumber");
		fieldNames.add("year");
		fieldNames.add("bpm");
		fieldNames.add("inDisk");
		fieldNames.add("isCompilation");
		fieldNames.add("dateModified");
		fieldNames.add("dateAdded");
		fieldNames.add("fileFormat");
		fieldNames.add("hasCover");
		fieldNames.add("isVariableBitRate");
		fieldNames.add("encoder");
		
		fields.put(Track.class,fieldNames);
	}
	
	@Override
	public void run() {
		try {
			// Save the list of tracks, covers, and the sequence object
			FileOutputStream fos;
			JsonWriter jsw;
			if(saveTracks) {
				LOG.debug("Saving list of tracks in {}", tracksFile);
				fos = new FileOutputStream(tracksFile);				
				jsw = new JsonWriter(fos, args);
				jsw.write(trackList);
				fos.close();
				jsw.close();
				
				LOG.debug("Saving sequence object in {}", sequenceFile);
				fos = new FileOutputStream(sequenceFile);
				jsw = new JsonWriter(fos);
				jsw.write(trackSequence);
				fos.close();
				jsw.close();
			}
			// Save the map of waveforms
			if(saveWaveforms) {
				LOG.debug("Saving waveform images in {}", waveformsFile);
				fos = new FileOutputStream(waveformsFile);
				jsw = new JsonWriter(fos);
				jsw.write(waveformsMap);
				fos.close();
				jsw.close();
			}
		}
		catch (IOException |RuntimeException e) {
			Platform.runLater(() -> {
				LOG.error("Error saving music library", e);
				ErrorHandler.getInstance().addError(e, ErrorType.COMMON);
				ErrorHandler.getInstance().showErrorDialog(ErrorType.COMMON);
			});
		}
	}
}