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
 */

package tests.unit;

import java.nio.file.Path;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;

import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public abstract class AudioBaseTest {
	
	protected Track expectedTest;
	protected Path testPath;
	protected String fileFolder = "/users/octavio/test";
	protected String[] mimeTypes = {"mp3","m4a","flac","wav"};
	protected final int MP3 = 0;
	protected final int M4A = 1;
	protected final int FLAC = 2;
	protected final int WAV = 3;
	
	protected void writeMetadata(Track expected, Path path) throws Exception {
		AudioFile audioFile = AudioFileIO.read(path.toFile());
		Tag tag = audioFile.getTag();
		tag.setField(FieldKey.TITLE, expected.getName());
		tag.setField(FieldKey.ALBUM, expected.getAlbum());
		tag.setField(FieldKey.ALBUM_ARTIST, expected.getAlbumArtist());
		tag.setField(FieldKey.ARTIST, expected.getArtist());
		tag.setField(FieldKey.GENRE, expected.getGenre());
		tag.setField(FieldKey.COMMENT, expected.getComments());
		tag.setField(FieldKey.GROUPING, expected.getLabel());
		tag.setField(FieldKey.TRACK, expected.getTrackNumber()+"");
		tag.setField(FieldKey.DISC_NO, expected.getDiscNumber()+"");
		tag.setField(FieldKey.YEAR, expected.getYear()+"");
		tag.setField(FieldKey.BPM, expected.getBpm()+"");
		if(expected.getFileFormat().equals("m4a"))
			((Mp4Tag)tag).setField(Mp4FieldKey.COMPILATION, "1");
		tag.setField(FieldKey.IS_COMPILATION, ""+expected.getIsCompilation());
		audioFile.commit();
	}
	
	protected void updateCover(Path path, Path coverFile) throws Exception {
		AudioFile audioFile = AudioFileIO.read(path.toFile());
		Tag tag = audioFile.getTag();
		Artwork cover = ArtworkFactory.createArtworkFromFile(coverFile.toFile());
		tag.deleteArtworkField();
		tag.addField(cover);
		audioFile.commit();
	}
}