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

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.After;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.api.mockito.PowerMockito;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v1Tag;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.musicott.model.Track;
import com.musicott.task.parser.Mp3Parser;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static com.mpatric.mp3agic.ID3v1Genres.matchGenreDescription;;

/**
 * @author Octavio Calleya
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Mp3Parser.class)
public class Mp3ParserTest {
	
	@After
	public void tearDown() {
		verifyStatic();
	}
	
	@Test
	public void trackNoId3TagTest() throws Exception {
		File file = noId3File("/Users/Octavio/Test/testeable.mp3");
		
		// Expected track creation with no ID3 tag
		Track t = new Track();
		t.setFileFolder(new File(file.getParent()).getAbsolutePath());
		t.setFileName(file.getName());
		t.getIsInDisk().set(true);
		t.getSize().set((int) (file.length()));
		t.getName().set(file.getName());

		PowerMockito.spy(Mp3Parser.class);
		Track expectedTrack = Mp3Parser.parseMp3File(file);
		
		verifyPrivate(Mp3Parser.class, times(1)).invoke("checkCover", t);
		assertEquals(expectedTrack, t);
	}
	
	@Test
	public void trackId3v1TagTest() throws Exception {
		File file = id3v1TagFile("/Users/Octavio/Test/testeable.mp3");
		
		// Expected track creation with no ID3 tag
		Track track = new Track();
		track.setFileFolder(new File(file.getParent()).getAbsolutePath());
		track.setFileName(file.getName());
		track.getIsInDisk().set(true);
		track.getSize().set((int) (file.length()));
		
		// Expected info with ID3v1 tag
		track.getName().set("Skeksis (Original Mix)");
		track.getArtist().set("Alan Fitzpatrick");
		track.getAlbum().set("Skeksis");
		track.getComments().set("Very good song! Nice drop");
		track.getGenre().set("Techno");
		track.getTrackNumber().set(3);
		track.getYear().set(2011);

		PowerMockito.spy(Mp3Parser.class);
		Track expectedTrack = Mp3Parser.parseMp3File(file);
		
		verifyPrivate(Mp3Parser.class, times(1)).invoke("checkCover", track);
		assertEquals(expectedTrack, track);
	}
	
	@Test
	public void trackId3v2TagTest() throws Exception {
		File file = id3v2TagFile("/Users/Octavio/Test/testeable.mp3");
		
		// Expected track creation with no ID3 tag
		Track track = new Track();
		track.setFileFolder(new File(file.getParent()).getAbsolutePath());
		track.setFileName(file.getName());
		track.getIsInDisk().set(true);
		track.getSize().set((int) (file.length()));
		
		// Expected info with ID3v2 tag
		track.getName().set("Skeksis (Original Mix)");
		track.getArtist().set("Alan Fitzpatrick");
		track.getAlbum().set("Skeksis");
		track.getAlbumArtist().set("Alan Fitzpatrick");
		track.getBPM().set(128);
		track.getComments().set("Very good song! Nice drop");
		track.getLabel().set("Drumcode");
		track.getGenre().set("Techno");
		track.getTrackNumber().set(3);
		track.getYear().set(2011);
		track.getTotalTime().set(new Mp3File("/Users/Octavio/Test/testeable.mp3").getId3v2Tag().getLength());

		PowerMockito.spy(Mp3Parser.class);
		Track expectedTrack = Mp3Parser.parseMp3File(file);
		
		verifyPrivate(Mp3Parser.class, times(1)).invoke("checkCover", track);
		assertEquals(expectedTrack, track);
	}
	
	private File id3v2TagFile(String path) throws Exception {
		File file = new File(path);
		Mp3File mp3 = new Mp3File(file);
		mp3.removeId3v1Tag();
		mp3.removeCustomTag();
		ID3v2 tag = new ID3v24Tag();
		tag.setTitle("Skeksis (Original Mix)");
		tag.setArtist("Alan Fitzpatrick");
		tag.setAlbum("Skeksis");
		tag.setAlbumArtist("Alan Fitzpatrick");
		tag.setBPM(128);
		tag.setComment("Very good song! Nice drop");
		tag.setGenre(matchGenreDescription("Techno"));
		tag.setTrack("3");
		tag.setGrouping("Drumcode");
		tag.setYear("2011");
		mp3.setId3v2Tag(tag);
		mp3.save("/Users/Octavio/Test/testeable_.mp3");
		
		File file2 = new File("/Users/Octavio/Test/testeable_.mp3");
		assertTrue(file.delete());
		assertTrue(file2.renameTo(file));
		return file;
	}
	
	private File id3v1TagFile(String path) throws Exception{
		File file = new File(path);
		Mp3File mp3 = new Mp3File(file);
		mp3.removeId3v2Tag();
		mp3.removeCustomTag();
		ID3v1 tag = new ID3v1Tag();
		tag.setTitle("Skeksis (Original Mix)");
		tag.setArtist("Alan Fitzpatrick");
		tag.setAlbum("Skeksis");
		tag.setComment("Very good song! Nice drop");
		tag.setGenre(matchGenreDescription("Techno"));
		tag.setTrack("3");
		tag.setYear("2011");
		mp3.setId3v1Tag(tag);
		mp3.save("/Users/Octavio/Test/testeable_.mp3");
		
		File file2 = new File("/Users/Octavio/Test/testeable_.mp3");
		assertTrue(file.delete());
		assertTrue(file2.renameTo(file));
		return file;
	}

	private File noId3File(String path) throws Exception {
		File file = new File(path);
		Mp3File mp3 = new Mp3File(file);
		mp3.removeId3v2Tag();
		mp3.removeId3v1Tag();
		mp3.removeCustomTag();
		mp3.save("/Users/Octavio/Test/testeable_.mp3");
		
		File file2 = new File("/Users/Octavio/Test/testeable_.mp3");
		assertTrue(file.delete());
		assertTrue(file2.renameTo(file));
		return file;
	}
}