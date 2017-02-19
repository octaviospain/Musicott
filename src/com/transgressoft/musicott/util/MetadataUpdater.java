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
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package com.transgressoft.musicott.util;

import com.transgressoft.musicott.model.*;
import org.jaudiotagger.audio.*;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.audio.wav.*;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.images.*;
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

	public MetadataUpdater(Track track) {
		this.track = track;
	}

	/**
	 * Writes the {@link Track} information to an audio file metadata.
	 *
	 * @return {@code True} if the operation was successful, {@code False} otherwise
	 */
	public void writeAudioMetadata() throws TrackUpdateException {
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
			throw new TrackUpdateException(errorText, exception);
		}
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
		tag.setField(FieldKey.IS_COMPILATION, Boolean.toString(track.isPartOfCompilation()));
	}

	/**
	 * Saves a new cover image to the audio file metadata of the {@link Track}
	 *
	 * @param coverFile The {@link File} of the new cover image to save
	 *
	 * @return {@code True} if the operation was successful, {@code False} otherwise
	 *
	 * @throws Exception If something went bad updating the cover on the metadata
	 */
	public void updateCover(File coverFile) throws TrackUpdateException {
		Path trackPath = Paths.get(track.getFileFolder(), track.getFileName());
		File trackFile = trackPath.toFile();
		updateCoverOnTag(trackFile, coverFile);
		track.hasCoverProperty().set(true);
	}

	private void updateCoverOnTag(File trackFile, File coverFile) throws TrackUpdateException {
		try {
			AudioFile audioFile = AudioFileIO.read(trackFile);
			Artwork cover = ArtworkFactory.createArtworkFromFile(coverFile);
			Tag tag = audioFile.getTag();
			tag.deleteArtworkField();
			tag.addField(cover);
			audioFile.commit();
		}
		catch (IOException | TagException | CannotWriteException | CannotReadException |
				InvalidAudioFrameException | ReadOnlyFileException exception) {
			LOG.warn("Error saving cover image of {}", track, exception);
			String errorText = "Error saving cover image of " + track.getArtist() + " - " + track.getName();
			throw new TrackUpdateException(errorText, exception);
		}
	}

	/**
	 * Search for an image in the folder of the audio file, and saves it to his
	 * metadata.
	 *
	 * @return {@code True} if the operation was successful, {@code False} otherwise
	 */
	public void searchCoverInFolderAndUpdate() throws TrackUpdateException {
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
			updateCover(coverFile);
		}
	}
}
