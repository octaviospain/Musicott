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

import javafx.concurrent.Task;

import com.cedarsoftware.util.io.JsonWriter;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class SaveLibraryTask extends Task<Void> {

	private List<Track> trackList;
	private File tracksFile;
	private Map<String,Object> args;

	public SaveLibraryTask() {
		tracksFile = new File("./resources/tracks.json");
		trackList = MusicLibrary.getInstance().getTracks();
		
		args = new HashMap<String,Object>();
		Map<Class,List<String>> fields = new HashMap<Class,List<String>>();
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
		fieldNames.add("isInDisk");
		fieldNames.add("isCompilation");
		fieldNames.add("dateModified");
		fieldNames.add("dateAdded");
		
		fields.put(Track.class,fieldNames);
	}
	
	@Override
	protected Void call() throws Exception {
		try {
			FileOutputStream fos = new FileOutputStream(tracksFile);				
			JsonWriter jsw = new JsonWriter(fos, args);
			jsw.write(trackList);
			jsw.close();
			fos.close();
		}
		catch (IOException e) {
			//TODO show error dialog and continue without save library
		}
		return null;
	}
}