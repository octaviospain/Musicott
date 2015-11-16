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

package tests.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.musicott.model.Track;
import com.musicott.util.MetadataParser;
import com.musicott.util.ObservableListWrapperCreator;
import com.musicott.util.Utils;
import com.musicott.view.ImportController;
import com.sun.javafx.collections.ObservableListWrapper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * @author Octavio Calleya
 *
 */
@SuppressWarnings({"restriction", "unchecked"})
public class LibrarySerializeTest {
	
	File jsonFile;
	Map<String,Object> args;
	FileOutputStream fos;
	JsonWriter jsw;
	FileInputStream fis;
	JsonReader jsr;
	ImportController icc = new ImportController();
	
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
	public void testTrackListToJson() throws Exception {
		int MAX_TRACKS = 5;
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
		List<Track> tracks = new ArrayList<>();
		for(File f: audioFiles) {
			MetadataParser parser = new MetadataParser(f);
			Track t = parser.createTrack();
			tracks.add(t);
		}
		FileOutputStream fos = new FileOutputStream(jsonFile);
		JsonWriter jsw = new JsonWriter(fos, args);
		jsw.write(tracks);
		jsw.close();
		fos.close();
		
		FileInputStream fis = new FileInputStream(jsonFile);
		JsonReader jsr = new JsonReader(fis);
		List<Track> testedTracks = (List<Track>) jsr.readObject();
		jsr.close();
		fis.close();
		assertEquals(tracks, testedTracks);
	}
	
	@Test
	public void testObservableTrackListToJson() throws Exception {
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
		List<Track> tracks = new ArrayList<>();
		for(File f: audioFiles) {
			MetadataParser parser = new MetadataParser(f);
			Track t = parser.createTrack();
			tracks.add(t);
		}
		
		ObservableList<Track> obsTracks = FXCollections.observableArrayList();
		obsTracks.addAll(tracks);
		
		FileOutputStream fos = new FileOutputStream(jsonFile);
		JsonWriter jsw = new JsonWriter(fos, args);
		jsw.write(obsTracks);
		jsw.close();
		fos.close();
		
		FileInputStream fis = new FileInputStream(jsonFile);
		JsonReader jsr = new JsonReader(fis);
		JsonReader.assignInstantiator(ObservableListWrapper.class, new ObservableListWrapperCreator());
		List<Track> testedTracks = (List<Track>) jsr.readObject();
		jsr.close();
		fis.close();
		assertEquals(obsTracks, testedTracks);
	}
	
	@Test
	public void testTrackToJsonToTrack() throws Exception {
		MetadataParser parser = new MetadataParser(new File("/users/octavio/test/testeable.mp3"));
		Track t = parser.createTrack();
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