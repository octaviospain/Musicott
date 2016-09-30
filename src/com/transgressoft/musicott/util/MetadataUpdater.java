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

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import org.jaudiotagger.audio.*;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.audio.wav.*;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.images.*;
import org.jaudiotagger.tag.mp4.*;
import org.jaudiotagger.tag.wav.*;
import org.slf4j.*;

import java.io.*;
import java.nio.file.*;

/**
 * Performs the operation of writing the information of a {@link Track} instance
 * to the audio metadata of the file.
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 * @see <a href="http://www.jthink.net/jaudiotagger/">jAudioTagger</a>
 */
public class MetadataUpdater {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private Track track;

	private ErrorDemon errorDemon = ErrorDemon.getInstance();

	public MetadataUpdater(Track track) {
		this.track = track;
	}

	/**
	 * Writes the {@link Track} information to an audio file metadata.
	 *
	 * @return <tt>true</tt> if the operation was successful, <tt>false</tt> otherwise
	 *
	 * @throws TrackUpdateException if something went bad in the operation.
	 */
	public boolean writeAudioMetadata() {
		boolean succeeded = false;
		Path trackPath = Paths.get(track.getFileFolder(), track.getFileName());
		try {
			AudioFile audio = AudioFileIO.read(trackPath.toFile());
			String format = audio.getAudioHeader().getFormat();
			if (format.startsWith("WAV")) {
				WavTag wavTag = new WavTag(WavOptions.READ_ID3_ONLY);
				wavTag.setID3Tag(new ID3v24Tag());
				wavTag.setInfoTag(new WavInfoTag());
				audio.setTag(wavTag);
			}
			setTrackFieldsToTag(audio.getTag());
			audio.commit();
		}
		catch (IOException | CannotReadException | ReadOnlyFileException |
				TagException | CannotWriteException | InvalidAudioFrameException exception) {
			LOG.warn("Error updating metadata of {}", track, exception);
			String errorText = "Error writing metadata of " + track.getArtist() + " - " + track.getName();
			errorDemon.showErrorDialog(errorText, "", exception);
		}
		succeeded = true;
		return succeeded;
	}

	private void setTrackFieldsToTag(Tag tag) throws FieldDataInvalidException {
		//	tag.setEncoding(Charset.forName("UTF-8"));	//TODO when jaudiotagger supports it
		tag.setField(FieldKey.TITLE, track.getName());
		tag.setField(FieldKey.ALBUM, track.getAlbum());
		tag.setField(FieldKey.ALBUM_ARTIST, track.getAlbumArtist());
		tag.setField(FieldKey.ARTIST, track.getArtist());
		tag.setField(FieldKey.GENRE, track.getGenre());
		tag.setField(FieldKey.COMMENT, track.getComments());
		tag.setField(FieldKey.GROUPING, track.getLabel());
		tag.setField(FieldKey.TRACK, Integer.toString(track.getTrackNumber()));
		tag.deleteField(FieldKey.TRACK_TOTAL);
		tag.setField(FieldKey.DISC_NO, Integer.toString(track.getDiscNumber()));
		tag.deleteField(FieldKey.DISC_TOTAL);
		tag.setField(FieldKey.YEAR, Integer.toString(track.getYear()));
		tag.setField(FieldKey.BPM, Integer.toString(track.getBpm()));
		if ("m4a".equals(track.getFileFormat())) {
			((Mp4Tag) tag).setField(Mp4FieldKey.COMPILATION, track.isPartOfCompilation() ? "1" : "0");
		}
		tag.setField(FieldKey.IS_COMPILATION, Boolean.toString(track.isPartOfCompilation()));
	}

	/**
	 * Saves a new cover image to the audio file metadata of the {@link Track}
	 *
	 * @param coverFile The {@link File} of the new cover image to save
	 *
	 * @return <tt>true</tt> if the operation was successful, <tt>false</tt> otherwise
	 */
	public boolean updateCover(File coverFile) {
		Path trackPath = Paths.get(track.getFileFolder(), track.getFileName());
		File trackFile = trackPath.toFile();
		boolean result = updateCoverOnTag(trackFile, coverFile);
		if (result) {
			track.hasCoverProperty().set(true);
		}
		return result;
	}

	private boolean updateCoverOnTag(File trackFile, File coverFile) {
		boolean result = false;
		try {
			AudioFile audioFile = AudioFileIO.read(trackFile);
			Artwork cover = ArtworkFactory.createArtworkFromFile(coverFile);
			Tag tag = audioFile.getTag();
			tag.deleteArtworkField();
			tag.addField(cover);
			audioFile.commit();
			result = true;
		}
		catch (IOException | TagException | CannotWriteException | CannotReadException |
				InvalidAudioFrameException | ReadOnlyFileException exception) {
			LOG.warn("Error saving cover image of {}", track, exception);
			String errorText = "Error saving cover image of " + track.getArtist() + " - " + track.getName();
			errorDemon.showErrorDialog(errorText, "", exception);
		}
		return result;
	}

	/**
	 * Search for an image in the folder of the audio file, and saves it to his
	 * metadata.
	 *
	 * @return <tt>true</tt> if the operation was successful, <tt>false</tt> otherwise
	 */
	public boolean searchCoverInFolderAndUpdate() {
		boolean found = false;
		File coverFile = null;
		String[] acceptedMimeTypes = {"jpg", "jpeg", "png"};
		String trackFolder = track.getFileFolder();
		for (String mimeType : acceptedMimeTypes) {
			File file = new File(trackFolder + "/cover." + mimeType);
			if (file.exists()) {
				coverFile = file;
				break;
			}
		}
		if (coverFile != null) {
			track.setCoverImage(coverFile);
			found = updateCover(coverFile);
		}
		return found;
	}
}
