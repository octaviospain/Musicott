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

import static org.junit.Assert.*;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import org.junit.Before;
import org.junit.Test;

import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.model.TrackField;
import com.musicott.util.MetadataParser;

import javafx.beans.property.Property;
import javafx.util.Duration;

/**
 * @author Octavio Calleya
 *
 */
public class ParseUpdateTest extends AudioBaseTest {
	
	Path cover = Paths.get("/users/octavio/test", "cover.jpg");
	
	@Before
	public void setUp() {
		MusicLibrary.getInstance().setTrackSequence(new AtomicInteger());
		expectedTest = new Track();
		expectedTest.setTrackID(0);
		expectedTest.setInDisk(true);
		expectedTest.setFileFolder(fileFolder);
		expectedTest.setName("Temazo!");
		expectedTest.setAlbum("Musicott mola");
		expectedTest.setAlbumArtist("Batman Uroboros");
		expectedTest.setArtist("Trallaró");
		expectedTest.setGenre("House Dark Hardcid");
		expectedTest.setComments("Bad song!");
		expectedTest.setLabel("Gotham");
		expectedTest.setTrackNumber(8);
		expectedTest.setDiscNumber(5);
		expectedTest.setYear(2222);
		expectedTest.setBpm(125);
		expectedTest.setCompilation(true);
	}
	
	@Test
	public void parseMp3Test() throws Exception {
		expectedTest.setFileName("testeable."+mimeTypes[MP3]);
		baseParseTest(MP3);
	}
	
	@Test
	public void parseM4aTest() throws Exception {
		expectedTest.setFileName("testeable."+mimeTypes[M4A]);
		baseParseTest(M4A);
	}
	
	@Test
	public void parseFlacTest() throws Exception {
		expectedTest.setFileName("testeable."+mimeTypes[FLAC]);
		baseParseTest(FLAC);
	}
	
	@Test
	public void parseWavTest() throws Exception {
		expectedTest.setFileName("testeable."+mimeTypes[WAV]);
		baseParseTest(WAV);
	}
	
	@Test
	public void updateMp3Test() throws Exception {
		expectedTest.setFileName("testeable."+mimeTypes[MP3]);
		baseUpdateTest(MP3);
	}
	
	@Test
	public void updateM4aTest() throws Exception {
		expectedTest.setFileName("testeable."+mimeTypes[M4A]);
		baseUpdateTest(M4A);
	}
	
	@Test
	public void updateFlacTest() throws Exception {
		expectedTest.setFileName("testeable."+mimeTypes[FLAC]);
		baseUpdateTest(FLAC);
	}
	
	@Test
	public void updateWavTest() throws Exception {
		expectedTest.setFileName("testeable."+mimeTypes[WAV]);
		baseUpdateTest(WAV);
	}
	
	private void baseUpdateTest(int type) throws Exception {
		testPath = FileSystems.getDefault().getPath(fileFolder, expectedTest.getFileName());
		expectedTest.setTotalTime(Duration.seconds(AudioFileIO.read(testPath.toFile()).getAudioHeader().getTrackLength()));
		expectedTest.setSize((int)testPath.toFile().length());
		
		assertTrue(expectedTest.updateMetadata());
		assertTrue(type == WAV ? !expectedTest.updateCover(cover.toFile()) : expectedTest.updateCover(cover.toFile()));
		
		AudioFile audioFile = AudioFileIO.read(testPath.toFile());
		Tag tag = audioFile.getTag();
		assertEquals(expectedTest.getName(), tag.getFirst(FieldKey.TITLE));
		assertEquals(expectedTest.getArtist(), tag.getFirst(FieldKey.ARTIST));
		assertEquals(expectedTest.getAlbum(), tag.getFirst(FieldKey.ALBUM));
		assertEquals(expectedTest.getAlbumArtist(), tag.getFirst(FieldKey.ALBUM_ARTIST));
		assertEquals(expectedTest.getBpm(), Integer.parseInt(tag.getFirst(FieldKey.BPM)));
		assertEquals(expectedTest.getGenre(), tag.getFirst(FieldKey.GENRE));
		assertEquals(expectedTest.getComments(), tag.getFirst(FieldKey.COMMENT));
		assertEquals(expectedTest.getLabel(), tag.getFirst(FieldKey.GROUPING));
		assertEquals(expectedTest.getTrackNumber(), Integer.parseInt(tag.getFirst(FieldKey.TRACK)));
		assertEquals(expectedTest.getDiscNumber(), Integer.parseInt(tag.getFirst(FieldKey.DISC_NO)));
		assertEquals(expectedTest.getYear(), Integer.parseInt(tag.getFirst(FieldKey.YEAR)));
		if(type == M4A)	// if is testing m4a
			assertEquals(expectedTest.getIsCompilation(), ((Mp4Tag)tag).getFirst(Mp4FieldKey.COMPILATION).equals("1") ? true : false);
		else
			assertEquals(expectedTest.getIsCompilation(), tag.getFirst(FieldKey.IS_COMPILATION).equals("true") ? true : false);
		assertTrue(type == WAV ? tag.getArtworkList().isEmpty() : !tag.getArtworkList().isEmpty());
	}

	
	private void baseParseTest(int type) throws Exception {
		testPath = FileSystems.getDefault().getPath(fileFolder, expectedTest.getFileName());
		AudioHeader header = AudioFileIO.read(testPath.toFile()).getAudioHeader();
		expectedTest.setTotalTime(Duration.seconds(header.getTrackLength()));
		expectedTest.setSize((int)testPath.toFile().length());
		String bitRate = header.getBitRate();
		if(bitRate.substring(0, 1).equals("~")) {
			expectedTest.setIsVariableBitRate(true);
			bitRate = bitRate.substring(1);
		}
		expectedTest.setBitRate(Integer.parseInt(bitRate));
		writeMetadata(expectedTest, testPath);
		updateCover(testPath, cover);
		
		MetadataParser parser = new MetadataParser(testPath.toFile());
		Track tested = parser.createTrack();
		assertEquals(expectedTest.getName(), tested.getName());
		assertEquals(expectedTest.getAlbum(), tested.getAlbum());
		assertEquals(expectedTest.getAlbumArtist(), tested.getAlbumArtist());
		assertEquals(expectedTest.getArtist(), tested.getArtist());
		assertEquals(expectedTest.getGenre(), tested.getGenre());
		assertEquals(expectedTest.getComments(), tested.getComments());
		assertEquals(expectedTest.getLabel(), tested.getLabel());
		assertEquals(expectedTest.getTrackNumber(), tested.getTrackNumber());
		assertEquals(expectedTest.getDiscNumber(), tested.getDiscNumber());
		assertEquals(expectedTest.getYear(), tested.getYear());
		assertEquals(expectedTest.getBpm(), tested.getBpm());
		assertEquals(expectedTest.getIsCompilation(), tested.getIsCompilation());
		assertEquals(mimeTypes[type], tested.getFileFormat());
		assertEquals(fileFolder, tested.getFileFolder());
		assertEquals(expectedTest.getFileName(), tested.getFileName());
		assertEquals(expectedTest.getBitRate(), tested.getBitRate());
		assertEquals(1, tested.getTrackID());
		assertEquals(expectedTest.getInDisk(), tested.getInDisk());
		assertEquals(expectedTest, tested);
		Map<TrackField, Property<?>> expectedMap = expectedTest.getPropertiesMap();
		Map<TrackField, Property<?>> testedMap = expectedTest.getPropertiesMap();
		for(TrackField tf: TrackField.values())
			assertEquals(expectedMap.get(tf), testedMap.get(tf));
		assertTrue(type == WAV ? !tested.hasCover() : tested.hasCover());
		assertEquals(expectedTest.getTotalTime(), tested.getTotalTime());
		assertEquals(expectedTest, tested);
	}
}