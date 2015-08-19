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

package com.musicott.util;

import static com.mpatric.mp3agic.ID3v1Genres.matchGenreDescription;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

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

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.error.WriteMetadataException;
import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class MetadataWriter {

	/**
	 * Writes the information stored in the <tt>Track</tt> object to the audio file
	 * that it represents (mp3, m4a or flac; special case for wav files)
	 * @param track The Track object
	 * @throws WriteMetadataException 
	 */
	public static void writeTrackMetadata(Track track) {
		String format = track.getFileName();
		StringTokenizer stk = new StringTokenizer(format, ".");
		while(stk.hasMoreTokens()) format = stk.nextToken();
		try {
			switch(format) {
				case "mp3":
					writeMp3Metadata(track);
					break;				
				case "m4a":
					writeM4aMetadata(track);
					break;				
				case "flac":
					writeFlacMetadata(track);
					break;
			}
		}
		catch (CannotReadException | IOException | TagException | UnsupportedTagException | InvalidDataException |
			   ReadOnlyFileException | InvalidAudioFrameException | CannotWriteException | NotSupportedException e) {
			WriteMetadataException pe = new WriteMetadataException("Write Metadata Error", e, track);
			ErrorHandler.getInstance().addError(pe, ErrorType.METADATA);
		}
	}
	
	private static void writeMp3Metadata(Track track) throws UnsupportedTagException, InvalidDataException, IOException, NotSupportedException {
		File file = new File(track.getFileFolder()+"/"+track.getFileName());
		Mp3File mp3 = new Mp3File(file);
		ID3v2 tag = new ID3v24Tag();
		tag.setTitle(track.getName());
		tag.setArtist(track.getArtist());
		tag.setAlbum(track.getAlbum());
		tag.setGenre(matchGenreDescription(track.getGenre()));
		tag.setGenreDescription(track.getGenre());
		tag.setComment(track.getComments());
		tag.setAlbumArtist(track.getAlbumArtist());
		tag.setGrouping(track.getLabel());
		tag.setTrack(""+track.getTrackNumber());
		tag.setPartOfSet(""+track.getDiscNumber());
		tag.setYear(""+track.getYear());
		tag.setBPM(track.getBpm());
		tag.setCompilation(track.getIsCompilation());
		mp3.setId3v2Tag(tag);
		mp3.save(track.getFileFolder()+"/_"+track.getFileName());
		
		File file2 = new File(track.getFileFolder()+"/_"+track.getFileName());
		file.delete();
		file2.renameTo(file);
	}

	private static void writeM4aMetadata(Track track) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		File file = new File(track.getFileFolder()+"/"+track.getFileName());
		AudioFile audioFile = AudioFileIO.read(file);
		Mp4Tag tag = (Mp4Tag) audioFile.getTag();
		tag.setField(Mp4FieldKey.TITLE, track.getName());
		tag.setField(Mp4FieldKey.ARTIST, track.getArtist());
		tag.setField(Mp4FieldKey.ALBUM, track.getAlbum());
		tag.setField(Mp4FieldKey.ALBUM_ARTIST, track.getAlbumArtist());
		tag.setField(Mp4FieldKey.BPM, ""+track.getBpm());
		tag.setField(Mp4FieldKey.COMMENT, track.getComments());
		tag.setField(Mp4FieldKey.GENRE, track.getGenre());
		tag.setField(Mp4FieldKey.GROUPING, track.getLabel());
		tag.setField(Mp4FieldKey.COMPILATION, track.getIsCompilation() ? ""+1 : ""+0);
		tag.setField(Mp4FieldKey.TRACK, ""+track.getTrackNumber());
		tag.setField(Mp4FieldKey.DISCNUMBER, ""+track.getDiscNumber());
		tag.setField(Mp4FieldKey.MM_ORIGINAL_YEAR, ""+track.getYear());
		AudioFileIO.write(audioFile);
	}
	
	private static void writeFlacMetadata(Track track) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		File file = new File(track.getFileFolder()+"/"+track.getFileName());
		AudioFile audioFile = AudioFileIO.read(file);
		FlacTag tag = (FlacTag) audioFile.getTag();
		tag.setField(FieldKey.TITLE, track.getName());
		tag.setField(FieldKey.ARTIST, track.getArtist());
		tag.setField(FieldKey.ALBUM, track.getAlbum());
		tag.setField(FieldKey.ALBUM_ARTIST, track.getAlbumArtist());
		tag.setField(FieldKey.BPM, ""+track.getBpm());
		tag.setField(FieldKey.COMMENT, track.getComments());
		tag.setField(FieldKey.GENRE, track.getGenre());
		tag.setField(FieldKey.GROUPING, track.getLabel());
		tag.setField(FieldKey.IS_COMPILATION, track.getIsCompilation() ? "true" : "false");
		tag.setField(FieldKey.TRACK, ""+track.getTrackNumber());
		tag.setField(FieldKey.DISC_NO, ""+track.getDiscNumber());
		tag.setField(FieldKey.YEAR, ""+track.getYear());
		AudioFileIO.write(audioFile);
	}
}