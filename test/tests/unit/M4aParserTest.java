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
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import org.junit.Test;

import com.musicott.error.ParseException;
import com.musicott.model.Track;
import com.musicott.task.parser.M4aParser;

/**
 * @author Octavio Calleya
 *
 */
public class M4aParserTest {
	
	@Test
	public void m4aTagTest() throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException, ParseException {
		File m4aFile = tagFile("/users/octavio/test/testeable.m4a");
		Track track = M4aParser.parseM4a(m4aFile);
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
	public void m4aNoTagTest() throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException, ParseException {
		File m4aFile = noTagFile("/users/octavio/test/testeable.m4a");
		Track track = M4aParser.parseM4a(m4aFile);
		assertEquals(track.getName(),"");
		assertEquals(track.getArtist(),"");
		assertEquals(track.getAlbum(),"");
		assertEquals(track.getAlbumArtist(),"");
		assertEquals(track.getComments(),"");
		assertEquals(track.getGenre(),"");
		assertEquals(track.getLabel(),"");
		assertTrue(!track.getIsCompilation());
		assertEquals(track.getBpm(), 0);
		assertEquals(track.getTrackNumber(), 0);
		assertEquals(track.getDiscNumber(), 0);
		assertEquals(track.getYear(), 0);
	}
	
	public File tagFile(String path) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
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
	
	public File noTagFile(String path) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		File file = new File(path);
		AudioFile audioFile = AudioFileIO.read(file);
		Mp4Tag tag = (Mp4Tag) audioFile.getTag();
		tag.setField(Mp4FieldKey.TITLE, "");
		tag.setField(Mp4FieldKey.ARTIST, "");
		tag.setField(Mp4FieldKey.ALBUM, "");
		tag.setField(Mp4FieldKey.ALBUM_ARTIST, "");
		tag.setField(Mp4FieldKey.BPM, "0");
		tag.setField(Mp4FieldKey.COMMENT, "");
		tag.deleteField(Mp4FieldKey.GENRE);
		tag.setField(Mp4FieldKey.GROUPING, "");
		tag.setField(Mp4FieldKey.COMPILATION, "0");
		tag.deleteField(Mp4FieldKey.TRACK);
		tag.deleteField(Mp4FieldKey.DISCNUMBER);
		tag.setField(Mp4FieldKey.MM_ORIGINAL_YEAR, "0");
		AudioFileIO.write(audioFile);
		return file;
	}
}