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
import org.junit.Test;

import com.musicott.model.ObservableTrack;
import com.musicott.task.parser.FlacParser;

/**
 * @author Octavio Calleya
 *
 */
public class FlacParserTest {
	
	@Test
	public void flacTagTest() throws Exception {
		File flacFile = tagFile("/users/octavio/test/testeable.flac");
		ObservableTrack track = FlacParser.parseFlacFile(flacFile);
		assertEquals(track.getName().get(),"Skeksis (Original Mix)");
		assertEquals(track.getArtist().get(),"Adam Beyer");
		assertEquals(track.getAlbum().get(),"Skeksis");
		assertEquals(track.getAlbumArtist().get(),"Alan Fitzpatrick");
		assertEquals(track.getComments().get(),"Good hit!");
		assertEquals(track.getGenre().get(),"Techno");
		assertEquals(track.getLabel().get(),"Drumcode");
		assertTrue(!track.getIsCompilation().get());
		assertEquals(track.getBPM().get(), 128);
		assertEquals(track.getTrackNumber().get(), 9);
		assertEquals(track.getDiscNumber().get(), 1);
		assertEquals(track.getYear().get(), 2003);
	}
	
	@Test
	public void flacNoTagTest() throws Exception {
		File flacFile = noTagFile("/users/octavio/test/testeable.flac");
		ObservableTrack track = FlacParser.parseFlacFile(flacFile);
		assertEquals(track.getName().get(),"");
		assertEquals(track.getArtist().get(),"");
		assertEquals(track.getAlbum().get(),"");
		assertEquals(track.getAlbumArtist().get(),"");
		assertEquals(track.getComments().get(),"");
		assertEquals(track.getGenre().get(),"");
		assertEquals(track.getLabel().get(),"");
		assertTrue(!track.getIsCompilation().get());
		assertEquals(track.getBPM().get(), -1);
		assertEquals(track.getTrackNumber().get(), 0);
		assertEquals(track.getDiscNumber().get(), 0);
		assertEquals(track.getYear().get(), 0);
	}
	
	public File tagFile(String path) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
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
	
	public File noTagFile(String path) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		File file = new File(path);
		AudioFile audioFile = AudioFileIO.read(file);
		FlacTag tag = (FlacTag) audioFile.getTag();
		tag.setField(FieldKey.TITLE, "");
		tag.setField(FieldKey.ARTIST, "");
		tag.setField(FieldKey.ALBUM, "");
		tag.setField(FieldKey.ALBUM_ARTIST, "");
		tag.setField(FieldKey.BPM, "-1");
		tag.setField(FieldKey.COMMENT, "");
		tag.setField(FieldKey.GENRE, "");
		tag.setField(FieldKey.GROUPING, "");
		tag.setField(FieldKey.IS_COMPILATION, "false");
		tag.setField(FieldKey.TRACK, "0");
		tag.setField(FieldKey.DISC_NO, "0");
		tag.setField(FieldKey.YEAR, "0");
		AudioFileIO.write(audioFile);
		return file;
	}
}