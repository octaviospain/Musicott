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

import static com.mpatric.mp3agic.ID3v1Genres.matchGenreDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import org.junit.Test;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.musicott.model.Track;
import com.musicott.task.parser.AudioFileParser;

import javafx.util.Duration;

/**
 * @author Octavio Calleya
 *
 */
public class AudioFileParserTest {

	@Test
	public void parseMp3Test() throws NotSupportedException, IOException, UnsupportedTagException, InvalidDataException {
		File mp3File = mp3File("/Users/Octavio/Test/testeable.mp3");
		
		// Expected track creation with no ID3 tag
		Track track = new Track();
		track.setFileFolder(new File(mp3File.getParent()).getAbsolutePath());
		track.setFileName(mp3File.getName());
		track.setInDisk(true);
		track.setSize((int) (mp3File.length()));
		
		// Expected info with ID3v2 tag
		track.getNameProperty().set("Skeksis (Original Mix)");
		track.getArtistProperty().set("Alan Fitzpatrick");
		track.getAlbumProperty().set("Skeksis");
		track.getAlbumArtistProperty().set("Alan Fitzpatrick");
		track.getBpmProperty().set(128);
		track.getCommentsProperty().set("Very good song! Nice drop");
		track.getLabelProperty().set("Drumcode");
		track.getGenreProperty().set("Techno");
		track.getTrackNumberProperty().set(3);
		track.getYearProperty().set(2011);
		track.setTotalTime(Duration.seconds((int)new Mp3File("/Users/Octavio/Test/testeable.mp3").getLengthInSeconds()));

		Track expectedTrack = AudioFileParser.parseAudioFile(mp3File, true, true, true);
		
		assertEquals(expectedTrack, track);
	}
	
	@Test
	public void parseM4aTest() throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		File m4aFile = m4aFile("/users/octavio/test/testeable.m4a");
		Track track = AudioFileParser.parseAudioFile(m4aFile, true, true, true);
		assertEquals(track.getName(),"Skeksis (Original Mix)");
		assertEquals(track.getArtist(),"Adam Beyer");
		assertEquals(track.getAlbum(),"Skeksis");
		assertEquals(track.getAlbumArtist(),"Alan Fitzpatrick");
		assertEquals(track.getComments(),"Good hit!");
		assertEquals(track.getGenre(),"Techno");
		assertEquals(track.getLabel(),"Drumcode");
		assertTrue(track.getIsCompilation());
		assertEquals(track.getBpm(), 128);
		assertEquals(track.getTrackNumber(), 9);
		assertEquals(track.getDiscNumber(), 1);
		assertEquals(track.getYear(), 2003);
	}
	
	@Test
	public void parseFlacTest() throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		File flacFile = flacFile("/users/octavio/test/testeable.flac");
		Track track = AudioFileParser.parseAudioFile(flacFile, true, true, true);
		assertEquals(track.getName(),"Skeksis (Original Mix)");
		assertEquals(track.getArtist(),"Adam Beyer");
		assertEquals(track.getAlbum(),"Skeksis");
		assertEquals(track.getAlbumArtist(),"Alan Fitzpatrick");
		assertEquals(track.getComments(),"Good hit!");
		assertEquals(track.getGenre(),"Techno");
		assertEquals(track.getLabel(),"Drumcode");
		assertTrue(!track.getIsCompilation());
		assertEquals(track.getBpm(), 128);
		assertEquals(track.getTrackNumber(), 9);
		assertEquals(track.getDiscNumber(), 1);
		assertEquals(track.getYear(), 2003);
	}
	
	@Test
	public void parseWavTest() throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
		File wavFile = new File("/users/octavio/test/testeable.wav");
		Track track = AudioFileParser.parseAudioFile(wavFile, true, true, true);
		AudioFile audioFile = AudioFileIO.read(wavFile);
		int bitRate = Integer.parseInt(audioFile.getAudioHeader().getBitRate());
		Duration totalTime = Duration.seconds(audioFile.getAudioHeader().getTrackLength());
		assertEquals(track.getName(), "testeable.wav");
		assertEquals(track.getFileFolder(), "/users/octavio/test");
		assertEquals(track.getFileName(), "testeable.wav");
		assertTrue(track.getInDisk());
		assertEquals(track.getSize(), (int)wavFile.length());
		assertEquals(track.getBitRate(), bitRate);
		assertEquals(track.getTotalTime(), totalTime);
	}
	
	public File flacFile(String path) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		File file = new File(path);
		AudioFile audioFile = AudioFileIO.read(file);
		FlacTag tag = (FlacTag) audioFile.getTag();
		tag.setField(FieldKey.TITLE, "Skeksis (Original Mix)");
		tag.setField(FieldKey.ARTIST, "Adam Beyer");
		tag.setField(FieldKey.ALBUM, "Skeksis");
		tag.setField(FieldKey.ALBUM_ARTIST, "Alan Fitzpatrick");
		tag.setField(FieldKey.BPM, "128");
		tag.setField(FieldKey.COMMENT, "Good hit!");
		tag.setField(FieldKey.GENRE, "Techno");
		tag.setField(FieldKey.GROUPING, "Drumcode");
		tag.setField(FieldKey.IS_COMPILATION, "false");
		tag.setField(FieldKey.TRACK, "9");
		tag.setField(FieldKey.DISC_NO, "1");
		tag.setField(FieldKey.YEAR, "2003");
		AudioFileIO.write(audioFile);
		return file;
	}
	
	public File m4aFile(String path) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		File file = new File(path);
		AudioFile audioFile = AudioFileIO.read(file);
		Mp4Tag tag = (Mp4Tag) audioFile.getTag();
		tag.setField(Mp4FieldKey.TITLE, "Skeksis (Original Mix)");
		tag.setField(Mp4FieldKey.ARTIST, "Adam Beyer");
		tag.setField(Mp4FieldKey.ALBUM, "Skeksis");
		tag.setField(Mp4FieldKey.ALBUM_ARTIST, "Alan Fitzpatrick");
		tag.setField(Mp4FieldKey.BPM, "128");
		tag.setField(Mp4FieldKey.COMMENT, "Good hit!");
		tag.setField(Mp4FieldKey.GENRE, "Techno");
		tag.setField(Mp4FieldKey.GROUPING, "Drumcode");
		tag.setField(Mp4FieldKey.COMPILATION, "1");
		tag.setField(Mp4FieldKey.TRACK, "9");
		tag.setField(Mp4FieldKey.DISCNUMBER, "1");
		tag.setField(Mp4FieldKey.MM_ORIGINAL_YEAR, "2003");
		AudioFileIO.write(audioFile);
		return file;
	}
	
	public File mp3File(String path) throws NotSupportedException, IOException, UnsupportedTagException, InvalidDataException {
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
}