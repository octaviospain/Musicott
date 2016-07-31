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

package com.transgressoft.musicott.tests.system.unit;

import com.cedarsoftware.util.io.*;
import com.sun.javafx.collections.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import javafx.collections.*;
import org.junit.*;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Octavio Calleya
 * @deprecated
 */
@SuppressWarnings({"restriction", "unchecked"})
public class LibrarySerializeTest {
	
	File jsonFile;
	Map<String,Object> args;
	FileOutputStream fos;
	JsonWriter jsw;
	FileInputStream fis;
	JsonReader jsr;
	
	@Before
	public void setUp() {
		jsonFile = new File(System.getProperty("user.home")+"/test.json");
		args = new HashMap<>();
		Map<Class<?>,List<String>> fields = new HashMap<>();
		List<String> fieldNames = new ArrayList<>();		
		args.put(JsonWriter.PRETTY_PRINT, true);
		args.put(JsonWriter.FIELD_SPECIFIERS, fields);
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
		fieldNames.add("encoding");

		fields.put(Track.class,fieldNames);
	}
	
	@After
	public void tearDown() throws Exception {
		if(jsonFile.exists())
			assertTrue(jsonFile.delete());
	}
	
	@Test
	public void testTrackMapToJson() throws Exception {
		int MAX_TRACKS = 10;
		FileFilter filter = file -> {
			int pos = file.getName().lastIndexOf(".");
			String format = file.getName().substring(pos+1);
			if(format.equals("mp3") ||
			   format.equals("flac"))
			   return true;
			else
				return false;
		};
		File folder = new File("/users/octavio/music/itunes/itunes media/music/");
		List<File> audioFiles = Utils.getAllFilesInFolder(folder, filter, MAX_TRACKS);
		assertEquals(MAX_TRACKS, audioFiles.size());
		Map<Integer, Track> tracks = new HashMap<>();
		for(File f: audioFiles) {
			Track t = MetadataParser.createTrack(f);
			tracks.put(t.getTrackId(), t);
		}
		FileOutputStream fos = new FileOutputStream(jsonFile);
		JsonWriter jsw = new JsonWriter(fos, args);
		jsw.write(tracks);
		jsw.close();
		fos.close();
		
		FileInputStream fis = new FileInputStream(jsonFile);
		JsonReader jsr = new JsonReader(fis);
		Map<Integer, Track> testedTracks = (Map<Integer, Track>) jsr.readObject();
		jsr.close();
		fis.close();
		assertEquals(tracks, testedTracks);
	}
	
	@Test
	public void testObservableTrackMapToJson() throws Exception {
		int MAX_TRACKS = 10;
		FileFilter filter = file -> {
			int pos = file.getName().lastIndexOf(".");
			String format = file.getName().substring(pos+1);
			if(format.equals("mp3") ||
			   format.equals("flac"))
			   return true;
			else
				return false;
		};
		File folder = new File("/users/octavio/music/itunes/itunes media/music/");
		List<File> audioFiles = Utils.getAllFilesInFolder(folder, filter, MAX_TRACKS);
		assertEquals(MAX_TRACKS, audioFiles.size());
		Map<Integer, Track> tracks = new HashMap<>();
		for(File f: audioFiles) {
			Track t = MetadataParser.createTrack(f);
			tracks.put(t.getTrackId(), t);
		}
		
		ObservableMap<Integer, Track> obsTracks = FXCollections.observableHashMap();
		obsTracks.putAll(tracks);
		
		FileOutputStream fos = new FileOutputStream(jsonFile);
		JsonWriter jsw = new JsonWriter(fos, args);
		jsw.write(obsTracks);
		jsw.close();
		fos.close();
		
		FileInputStream fis = new FileInputStream(jsonFile);
		JsonReader jsr = new JsonReader(fis);
		JsonReader.assignInstantiator(ObservableMapWrapper.class, new ObservableMapWrapperCreator());
		Map<Integer, Track> testedTracks = (Map<Integer, Track>) jsr.readObject();
		jsr.close();
		fis.close();
		assertEquals(obsTracks, testedTracks);
	}
	
	@Test
	public void testTrackToJsonToTrack() throws Exception {
		Track t = MetadataParser.createTrack(new File("/users/octavio/test/testeable.mp3"));
		fos = new FileOutputStream(jsonFile);
		jsw = new JsonWriter(fos, args);
		jsw.write(t);
		jsw.close();
		fos.close();
		
		fis = new FileInputStream(jsonFile);
		jsr = new JsonReader(fis);
		Track t2 = (Track) jsr.readObject();
		jsr.close();
		fis.close();
		assertEquals(t,t2);
	}	
}
