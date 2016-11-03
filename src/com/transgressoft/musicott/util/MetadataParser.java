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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.transgressoft.musicott.util;

import com.transgressoft.musicott.model.*;
import javafx.util.*;
import org.jaudiotagger.audio.*;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.tag.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;

/**
 * Performs the operation of parsing an audio file to a {@link Track} instance.
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 * @see <a href="http://www.jthink.net/jaudiotagger/">jAudioTagger</a>
 */
public class MetadataParser {

	private static final Logger LOG = LoggerFactory.getLogger(MetadataParser.class.getName());

	private MetadataParser() {}

	public static Track createTrack(File fileToParse) throws TrackParseException {
		Track track = new Track();
		try {
			LOG.debug("Creating AudioFile instance with jAudioTagger of: {}", fileToParse);
			AudioFile audioFile = AudioFileIO.read(fileToParse);
			track.setFileFolder(fileToParse.getParent());
			track.setFileName(fileToParse.getName());
			track.setInDisk(true);
			track.setSize((int) (fileToParse.length()));
			track.setTotalTime(Duration.seconds(audioFile.getAudioHeader().getTrackLength()));
			track.setEncoding(audioFile.getAudioHeader().getEncodingType());
			String bitRate = audioFile.getAudioHeader().getBitRate();
			if ("~".equals(bitRate.substring(0, 1))) {
				track.setIsVariableBitRate(true);
				bitRate = bitRate.substring(1);
			}
			track.setBitRate(Integer.parseInt(bitRate));
			Tag tag = audioFile.getTag();
			parseBaseMetadata(track, tag);
			getCoverBytes(tag).ifPresent(coverBytes -> track.hasCoverProperty().set(true));
		}
		catch (IOException | CannotReadException | ReadOnlyFileException |
				TagException | InvalidAudioFrameException exception) {
			LOG.debug("Error creating track from {}: ", fileToParse, exception);
			throw new TrackParseException("Error parsing the file " + fileToParse, exception);
		}
		return track;
	}

	private static void parseBaseMetadata(Track track, Tag tag) {
		LOG.debug("Parsing base metadata of the file");
		if (tag.hasField(FieldKey.TITLE))
			track.setName(tag.getFirst(FieldKey.TITLE));
		if (tag.hasField(FieldKey.ALBUM))
			track.setAlbum(tag.getFirst(FieldKey.ALBUM));
		if (tag.hasField(FieldKey.ALBUM_ARTIST))
			track.setAlbumArtist(tag.getFirst(FieldKey.ALBUM_ARTIST));
		if (tag.hasField(FieldKey.ARTIST))
			track.setArtist(tag.getFirst(FieldKey.ARTIST));
		if (tag.hasField(FieldKey.GENRE))
			track.setGenre(tag.getFirst(FieldKey.GENRE));
		if (tag.hasField(FieldKey.COMMENT))
			track.setComments(tag.getFirst(FieldKey.COMMENT));
		if (tag.hasField(FieldKey.GROUPING))
			track.setLabel(tag.getFirst(FieldKey.GROUPING));
		if (tag.hasField(FieldKey.ENCODER))
			track.setEncoder(tag.getFirst(FieldKey.ENCODER));
		if (tag.hasField(FieldKey.IS_COMPILATION)) {
			if ("m4a".equals(track.getFileFormat()))
				track.setIsPartOfCompilation("1".equals(tag.getFirst(FieldKey.IS_COMPILATION)));
			else
				track.setIsPartOfCompilation("true".equals(tag.getFirst(FieldKey.IS_COMPILATION)));
		}
		if (tag.hasField(FieldKey.BPM)) {
			try {
				int bpm = Integer.parseInt(tag.getFirst(FieldKey.BPM));
				track.setBpm(bpm < 1 ? 0 : bpm);
			}
			catch (NumberFormatException e) {}
		}
		if (tag.hasField(FieldKey.DISC_NO)) {
			try {
				int dn = Integer.parseInt(tag.getFirst(FieldKey.DISC_NO));
				track.setDiscNumber(dn < 1 ? 0 : dn);
			}
			catch (NumberFormatException e) {}
		}
		if (tag.hasField(FieldKey.TRACK)) {
			try {
				int trackNumber = Integer.parseInt(tag.getFirst(FieldKey.TRACK));
				track.setTrackNumber(trackNumber < 1 ? 0 : trackNumber);
			}
			catch (NumberFormatException e) {}
		}
		if (tag.hasField(FieldKey.YEAR)) {
			try {
				int year = Integer.parseInt(tag.getFirst(FieldKey.YEAR));
				track.setYear(year < 1 ? 0 : year);
			}
			catch (NumberFormatException e) {}
		}
	}

	public static Optional<Tag> getAudioTag(File file) {
		Optional<Tag> optionalTag;
		try {
			LOG.debug("Getting audio tag of {} ", file.getAbsolutePath());
			AudioFile audioFile = AudioFileIO.read(file);
			optionalTag = Optional.ofNullable(audioFile.getTag());
		}
		catch (IOException | CannotReadException | ReadOnlyFileException |
				TagException | InvalidAudioFrameException exception) {
			optionalTag = Optional.empty();
			LOG.debug("Unable to get the audio tag: ", exception);
		}
		return optionalTag;
	}

	public static Optional<byte[]> getCoverBytes(Tag tag) {
		LOG.debug("Getting cover bytes from tag");
		Optional<byte[]> coverBytes = Optional.empty();
		if (! tag.getArtworkList().isEmpty())
			coverBytes = Optional.ofNullable(tag.getFirstArtwork().getBinaryData());
		return coverBytes;
	}
}
