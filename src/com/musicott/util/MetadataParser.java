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

package com.musicott.util;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import com.musicott.model.Track;

import javafx.util.Duration;

/**
 * @author Octavio Calleya
 *
 */
public class MetadataParser {
	
	private File fileToParse;
	
	public MetadataParser(File file) {
		this.fileToParse = file;
	}

	public Track createTrack() throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
		Track track = new Track();
		AudioFile audioFile = AudioFileIO.read(fileToParse);
		track.setFileFolder(fileToParse.getParent().toString());
		track.setFileName(fileToParse.getName());
		track.setInDisk(true);
		track.setSize((int) (fileToParse.length()));
		track.setTotalTime(Duration.seconds(audioFile.getAudioHeader().getTrackLength()));
		String bitRate = audioFile.getAudioHeader().getBitRate();
		if(bitRate.substring(0, 1).equals("~")) {
			track.setIsVariableBitRate(true);
			bitRate = bitRate.substring(1);
		}
		track.setBitRate(Integer.parseInt(bitRate));
		Tag tag = audioFile.getTag();
		parseBaseMetadata(track, tag);
		checkCoverImage(track, tag);
		track.setSize((int) (fileToParse.length()));
		return track;
	}
	
	private void parseBaseMetadata(Track track, Tag tag) {
		track.setName(tag.getFirst(FieldKey.TITLE));
		track.setAlbum(tag.getFirst(FieldKey.ALBUM));
		track.setAlbumArtist(tag.getFirst(FieldKey.ALBUM_ARTIST));
		track.setArtist(tag.getFirst(FieldKey.ARTIST));
		track.setGenre(tag.getFirst(FieldKey.GENRE));
		track.setComments(tag.getFirst(FieldKey.COMMENT));
		track.setLabel(tag.getFirst(FieldKey.GROUPING));
		track.setEncoder(tag.getFirst(FieldKey.ENCODER));
		if(track.getFileFormat().equals("m4a"))
			track.setCompilation(tag.getFirst(FieldKey.IS_COMPILATION).equals("1") ? true : false);
		else
			track.setCompilation(tag.getFirst(FieldKey.IS_COMPILATION).equals("true") ? true : false);
		try {
			track.setBpm(Integer.parseInt(tag.getFirst(FieldKey.BPM)));
		} catch (NumberFormatException e) {}
		try {
			track.setDiscNumber(Integer.parseInt(tag.getFirst(FieldKey.DISC_NO)));
		} catch (NumberFormatException e) {}
		try {
			track.setTrackNumber(Integer.parseInt(tag.getFirst(FieldKey.TRACK)));
		} catch (NumberFormatException e) {}
		try {
			track.setYear(Integer.parseInt(tag.getFirst(FieldKey.YEAR)));
		} catch (NumberFormatException e) {}
	}
	
	private void checkCoverImage(Track track, Tag tag) {
		if(!tag.getArtworkList().isEmpty())
			track.setHasCover(true);
	}
}