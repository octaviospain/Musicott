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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.junit.After;
import org.junit.Test;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.musicott.model.MusicLibrary;
import com.musicott.model.ObservableTrack;
import com.musicott.task.parser.Mp3Parser;

/**
 * @author Octavio Calleya
 *
 */
public class LibrarySerializeTest {
	
	private final int MAX = 1;
	File jsonTrackFolder = new File("./data/tracks/");
	
	@After
	public void tearDown() {
		File[] files = jsonTrackFolder.listFiles();
		for(File f: files)
			if(f.getName().substring(f.getName().length()-4).equals("json"))
				f.delete();
	}
	
	@Test
	public void testTrackToJsonToTrack() throws Exception {
		ObservableTrack t = Mp3Parser.parseMp3File(new File("/Users/octavio/Music/iTunes/iTunes Media/Music/MATRiXXMAN/Metaphysix_ II, Rhythm/02 Stop It (Percapella).mp3"));
		File jsonFile = new File("./data/test.json");
		FileOutputStream fos = new FileOutputStream(jsonFile);
		JsonWriter jsw = new JsonWriter(fos);
		jsw.write(t);
		jsw.close();
		
		FileInputStream fis = new FileInputStream(jsonFile);
		JsonReader jsr = new JsonReader(fis);
		ObservableTrack t2 = (ObservableTrack) jsr.readObject();
		jsr.close();
		assertEquals(t,t2);
	}
	
	private void scanFolder(File folder, List<ObservableTrack> list) throws UnsupportedTagException, InvalidDataException, IOException {
		File[] files = folder.listFiles();
		for(File file:files)
			if(file.isDirectory())
				scanFolder(file, list);
			else
				if(list.size() >= MAX)
					break;
				else
					if(file.getName().substring(file.getName().length()-3).equals("mp3")) {
						list.add(Mp3Parser.parseMp3File(file));
					}
	}
}