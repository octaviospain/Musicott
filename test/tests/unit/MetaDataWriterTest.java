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
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import org.junit.Test;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.musicott.model.Track;
import com.musicott.util.MetadataWriter;

/**
 * @author Octavio Calleya
 *
 */
public class MetaDataWriterTest {

	@Test
	public void mp3MetaDataWriterTest() throws UnsupportedTagException, InvalidDataException, IOException {
		Track t = new Track();
		t.setFileFolder("/users/octavio/test/");
		t.setFileName("testeable.mp3");
		t.setName("Skeksis (Original Mix)");
		t.setAlbum("Skeksis");
		t.setAlbumArtist("Alan Fitzpatrick");
		t.setArtist("Alan Fitzpatrick");
		t.setGenre("Dark Techno");
		t.setComments("Temazo");
		t.setLabel("Drumcode");
		t.setTrackNumber(1);
		t.setDiscNumber(1);
		t.setYear(2002);
		t.setBpm(128);
		t.setCompilation(true);
		MetadataWriter.writeTrackMetadata(t);
		
		Mp3File mp3File = new Mp3File(new File(t.getFileFolder()+"/"+t.getFileName()));
		ID3v2 tag = mp3File.getId3v2Tag();
		assertEquals(t.getName(), tag.getTitle());
		assertEquals(t.getArtist(), tag.getArtist());
		assertEquals(t.getAlbum(), tag.getAlbum());
		assertEquals(t.getAlbumArtist(), tag.getAlbumArtist());
		assertEquals(t.getBpm(), tag.getBPM());
		assertEquals(t.getGenre(), tag.getGenreDescription());
		assertEquals(t.getComments(), tag.getComment());
		assertEquals(t.getLabel(), tag.getGrouping());
		assertEquals(t.getTrackNumber(), Integer.parseInt(tag.getTrack()));
		assertEquals(t.getDiscNumber(), Integer.parseInt(tag.getPartOfSet()));
		assertEquals(t.getYear(), Integer.parseInt(tag.getYear()));
		assertEquals(t.getIsCompilation(), tag.isCompilation());
	}
	
	@Test
	public void m4aMetaDataWriterTest() throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
		Track t = new Track();
		t.setFileFolder("/users/octavio/test/");
		t.setFileName("testeable.m4a");
		t.setName("Bang Bang You're Mine (Rock Me Gently Original Radio)");
		t.setAlbum("Warp10+1 Influences");
		t.setAlbumArtist("Warp Records");
		t.setArtist("Bang The Party");
		t.setGenre("Electronic");
		t.setComments("Temazo");
		t.setLabel("Warp Records");
		t.setTrackNumber(1);
		t.setDiscNumber(1);
		t.setYear(2002);
		t.setBpm(128);
		t.setCompilation(true);
		MetadataWriter.writeTrackMetadata(t);
		
		AudioFile audioFile = AudioFileIO.read(new File(t.getFileFolder()+"/"+t.getFileName()));
		Mp4Tag tag = (Mp4Tag) audioFile.getTag();
		assertEquals(t.getName(), tag.getFirst(Mp4FieldKey.TITLE));
		assertEquals(t.getArtist(), tag.getFirst(Mp4FieldKey.ARTIST));
		assertEquals(t.getAlbum(), tag.getFirst(Mp4FieldKey.ALBUM));
		assertEquals(t.getAlbumArtist(), tag.getFirst(Mp4FieldKey.ALBUM_ARTIST));
		assertEquals(t.getBpm(), Integer.parseInt(tag.getFirst(Mp4FieldKey.BPM)));
		assertEquals(t.getGenre(), tag.getFirst(Mp4FieldKey.GENRE));
		assertEquals(t.getComments(), tag.getFirst(Mp4FieldKey.COMMENT));
		assertEquals(t.getLabel(), tag.getFirst(Mp4FieldKey.GROUPING));
		assertEquals(t.getTrackNumber(), Integer.parseInt(tag.getFirst(Mp4FieldKey.TRACK)));
		assertEquals(t.getDiscNumber(), Integer.parseInt(tag.getFirst(Mp4FieldKey.DISCNUMBER)));
		assertEquals(t.getYear(), Integer.parseInt(tag.getFirst(Mp4FieldKey.MM_ORIGINAL_YEAR)));
		assertEquals(t.getIsCompilation(), tag.getFirst(Mp4FieldKey.COMPILATION).equals("1") ? true : false);
	}
	
	@Test
	public void flacMetaDataWriterTest() throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
		Track t = new Track();
		t.setFileFolder("/users/octavio/test/");
		t.setFileName("testeable.flac");
		t.setName("Bang Bang You're Mine (Rock Me Gently Original Radio)");
		t.setAlbum("Warp10+1 Influences");
		t.setAlbumArtist("Warp Records");
		t.setArtist("Bang The Party");
		t.setGenre("Electronic");
		t.setComments("Temazo");
		t.setLabel("Warp Records");
		t.setTrackNumber(1);
		t.setDiscNumber(1);
		t.setYear(2002);
		t.setBpm(128);
		t.setCompilation(true);
		MetadataWriter.writeTrackMetadata(t);
		
		AudioFile audioFile = AudioFileIO.read(new File(t.getFileFolder()+"/"+t.getFileName()));
		FlacTag tag = (FlacTag) audioFile.getTag();
		assertEquals(t.getName(), tag.getFirst(FieldKey.TITLE));
		assertEquals(t.getArtist(), tag.getFirst(FieldKey.ARTIST));
		assertEquals(t.getAlbum(), tag.getFirst(FieldKey.ALBUM));
		assertEquals(t.getAlbumArtist(), tag.getFirst(FieldKey.ALBUM_ARTIST));
		assertEquals(t.getBpm(), Integer.parseInt(tag.getFirst(FieldKey.BPM)));
		assertEquals(t.getGenre(), tag.getFirst(FieldKey.GENRE));
		assertEquals(t.getComments(), tag.getFirst(FieldKey.COMMENT));
		assertEquals(t.getLabel(), tag.getFirst(FieldKey.GROUPING));
		assertEquals(t.getTrackNumber(), Integer.parseInt(tag.getFirst(FieldKey.TRACK)));
		assertEquals(t.getDiscNumber(), Integer.parseInt(tag.getFirst(FieldKey.DISC_NO)));
		assertEquals(t.getYear(), Integer.parseInt(tag.getFirst(FieldKey.YEAR)));
		assertEquals(t.getIsCompilation(), tag.getFirst(FieldKey.IS_COMPILATION).equals("true") ? true : false);
	}
}