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
 * Copyright (C) 2005, 2006 Octavio Calleya
 */

package com.musicott.util;

import com.musicott.model.*;
import javafx.util.*;
import org.jaudiotagger.audio.*;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.tag.*;

import java.io.*;

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
		track.setEncoding(audioFile.getAudioHeader().getEncodingType());
		String bitRate = audioFile.getAudioHeader().getBitRate();
		if(bitRate.substring(0, 1).equals("~")) {
			track.setIsVariableBitRate(true);
			bitRate = bitRate.substring(1);
		}
		track.setBitRate(Integer.parseInt(bitRate));
		Tag tag = audioFile.getTag();
		parseBaseMetadata(track, tag);
		checkCoverImage(track, tag);
		return track;
	}
	
	private void parseBaseMetadata(Track track, Tag tag) {
		if(tag.hasField(FieldKey.TITLE))
			track.setName(tag.getFirst(FieldKey.TITLE));
		if(tag.hasField(FieldKey.ALBUM))
			track.setAlbum(tag.getFirst(FieldKey.ALBUM));
		if(tag.hasField(FieldKey.ALBUM_ARTIST))
			track.setAlbumArtist(tag.getFirst(FieldKey.ALBUM_ARTIST));
		if(tag.hasField(FieldKey.ARTIST))
			track.setArtist(tag.getFirst(FieldKey.ARTIST));
		if(tag.hasField(FieldKey.GENRE))
			track.setGenre(tag.getFirst(FieldKey.GENRE));
		if(tag.hasField(FieldKey.COMMENT))
			track.setComments(tag.getFirst(FieldKey.COMMENT));
		if(tag.hasField(FieldKey.GROUPING))
			track.setLabel(tag.getFirst(FieldKey.GROUPING));
		if(tag.hasField(FieldKey.ENCODER))
			track.setEncoder(tag.getFirst(FieldKey.ENCODER));
		if(tag.hasField(FieldKey.IS_COMPILATION))
			if(track.getFileFormat().equals("m4a"))
				track.setCompilation(tag.getFirst(FieldKey.IS_COMPILATION).equals("1") ? true : false);
			else
				track.setCompilation(tag.getFirst(FieldKey.IS_COMPILATION).equals("true") ? true : false);
		if(tag.hasField(FieldKey.BPM))
			try {
				int bpm = Integer.parseInt(tag.getFirst(FieldKey.BPM));
				track.setBpm(bpm < 1 ? 0 : bpm);
			} catch (NumberFormatException e) {}
		if(tag.hasField(FieldKey.DISC_NO))
			try {
				int dn = Integer.parseInt(tag.getFirst(FieldKey.DISC_NO));
				track.setDiscNumber(dn < 1 ? 0 : dn);
			} catch (NumberFormatException e) {}
		if(tag.hasField(FieldKey.TRACK))
			try {
				int tn = Integer.parseInt(tag.getFirst(FieldKey.TRACK));
				track.setTrackNumber(tn < 1 ? 0 : tn);
			} catch (NumberFormatException e) {}
		if(tag.hasField(FieldKey.YEAR))
			try {
				int year = Integer.parseInt(tag.getFirst(FieldKey.YEAR));
				track.setYear(year < 1 ? 0 : year);
			} catch (NumberFormatException e) {}
	}
	
	public static void checkCoverImage(Track track, Tag tag) {
		if(!tag.getArtworkList().isEmpty())
			track.setHasCover(true);
	}
}