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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.musicott.model.Track;
import com.musicott.task.parser.Mp3Parser;

/**
 * @author Octavio Calleya
 *
 */
public class LibrarySerializeTest {
	
	@Test
	public void testTrackToJsonToTrack() throws Exception {
		Track t = Mp3Parser.parseMp3File(new File("/Users/octavio/Music/iTunes/iTunes Media/Music/MATRiXXMAN/Metaphysix_ II, Rhythm/02 Stop It (Percapella).mp3"));
		File jsonFile = new File("./resources/test.json");
		FileOutputStream fos = new FileOutputStream(jsonFile);
		
		Map<String,Object> args = new HashMap<String,Object>();
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
		
		JsonWriter jsw = new JsonWriter(fos, args);
		jsw.write(t);
		jsw.close();
		fos.close();
		
		FileInputStream fis = new FileInputStream(jsonFile);
		JsonReader jsr = new JsonReader(fis);
		Track t2 = (Track) jsr.readObject();
		jsr.close();
		fis.close();
		assertEquals(t,t2);
	}
}