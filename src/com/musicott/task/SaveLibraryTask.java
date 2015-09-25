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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
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

import javafx.application.Platform;
import javafx.concurrent.Task;

import com.cedarsoftware.util.io.JsonWriter;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class SaveLibraryTask extends Task<Void> {

	private List<Track> trackList;
	private Map<Track, float[]> waveformsMap;
	private File tracksFile, waveformsFile;
	private Map<String,Object> args;

	public SaveLibraryTask() {
		tracksFile = new File("./resources/tracks.json");
		waveformsFile = new File("./resources/waveforms.json");
		trackList = MusicLibrary.getInstance().getTracks();
		waveformsMap = MusicLibrary.getInstance().getWaveforms();
		
		args = new HashMap<String,Object>();
		Map<Class<?>,List<String>> fields = new HashMap<Class<?>,List<String>>();
		args.put(JsonWriter.FIELD_SPECIFIERS, fields);
		
		List<String> fieldNames = new ArrayList<String>();
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
		fieldNames.add("hasCover");
		fieldNames.add("inDisk");
		fieldNames.add("isCompilation");
		fieldNames.add("dateModified");
		fieldNames.add("dateAdded");
		fieldNames.add("fileFormat");
		
		fields.put(Track.class,fieldNames);
	}
	
	@Override
	protected Void call() throws Exception {
		try {
			// Save the list of tracks
			FileOutputStream fos = new FileOutputStream(tracksFile);				
			JsonWriter jsw = new JsonWriter(fos, args);
			jsw.write(trackList);
			fos.close();
			jsw.close();
			// Save the map of waveforms
			fos = new FileOutputStream(waveformsFile);
			jsw = new JsonWriter(fos, args);
			jsw.write(waveformsMap);
			fos.close();
			jsw.close();
		}
		catch (IOException e) {
			Platform.runLater(() -> {
				ErrorHandler.getInstance().addError(e, ErrorType.COMMON);
				ErrorHandler.getInstance().showErrorDialog(ErrorType.COMMON);
			});
		}
		return null;
	}
}