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

import com.musicott.*;
import com.musicott.model.*;
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
 * @author Octavio Calleya
 *
 */
public class MetadataUpdater {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private boolean succeeded;
	private Track track;
	
	public MetadataUpdater(Track track) {
		this.track = track;
	}
	
	public boolean updateMetadata() throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		succeeded = false;
		Path trackPath = Paths.get(track.getFileFolder(), track.getFileName());
		AudioFile audio = AudioFileIO.read(trackPath.toFile());
		String format = audio.getAudioHeader().getFormat();
		if(format.startsWith("WAV")) {
			WavTag wavTag = new WavTag(WavOptions.READ_ID3_ONLY);
			wavTag.setID3Tag(new ID3v24Tag());
			wavTag.setInfoTag(new WavInfoTag());
			audio.setTag(wavTag);
		}
		baseUpdater(audio.getTag());
		audio.commit();
		succeeded = true;
//		LOG.warn("Error updating metadata of "+track, e);
//		ErrorHandler.getInstance().showErrorDialog("Error writing metadata of "+track.getArtist()+" - "+track.getName(), null, e);
		return succeeded;
	}
	
	public void baseUpdater(Tag tag) throws KeyNotFoundException, FieldDataInvalidException {
	//	tag.setEncoding(Charset.forName("UTF-8"));	//TODO when jaudiotagger supports it
		tag.setField(FieldKey.TITLE, track.getName());
		tag.setField(FieldKey.ALBUM, track.getAlbum());
		tag.setField(FieldKey.ALBUM_ARTIST, track.getAlbumArtist());
		tag.setField(FieldKey.ARTIST, track.getArtist());
		tag.setField(FieldKey.GENRE, track.getGenre());
		tag.setField(FieldKey.COMMENT, track.getComments());
		tag.setField(FieldKey.GROUPING, track.getLabel());
		tag.setField(FieldKey.TRACK, ""+track.getTrackNumber());
		tag.deleteField(FieldKey.TRACK_TOTAL);
		tag.setField(FieldKey.DISC_NO, ""+track.getDiscNumber());
		tag.deleteField(FieldKey.DISC_TOTAL);
		tag.setField(FieldKey.YEAR, ""+track.getYear());
		tag.setField(FieldKey.BPM, ""+track.getBpm());
		if(track.getFileFormat().equals("m4a"))
			((Mp4Tag)tag).setField(Mp4FieldKey.COMPILATION, track.getIsCompilation() ? "1" : "0");
		tag.setField(FieldKey.IS_COMPILATION, ""+track.getIsCompilation());
	}
	
	public boolean updateCover(File coverFile) {
		succeeded = false;
		Path trackPath = Paths.get(track.getFileFolder(), track.getFileName());
		try {
			AudioFile audioFile = AudioFileIO.read(trackPath.toFile());
			Tag tag = audioFile.getTag();
			Artwork cover = ArtworkFactory.createArtworkFromFile(coverFile);
			tag.deleteArtworkField();
			tag.addField(cover);
			audioFile.commit();
			succeeded = true;
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException
				| InvalidAudioFrameException | CannotWriteException e) {
			LOG.warn("Error saving cover image of "+track, e);
			ErrorDemon.getInstance().showErrorDialog("Error saving cover image of "+track.getArtist()+" - "+track.getName(), null, e);
		}
		if(succeeded)
			track.setHasCover(true);
		return succeeded;
	}
	
	// Unused
	public boolean searchCoverInFolder() {
		boolean finded = false;
		String[] mimeTypes = {"jpg","jpeg","png"};
		String trackFolder = track.getFileFolder();
		File coverFile = null;
		for(String m: mimeTypes) {
			File aux = new File(trackFolder+"/cover."+m);
			if(aux.exists()) {
				coverFile = aux;
				break;
			}
		}
		if(coverFile != null) {
			try {
				Artwork cover = ArtworkFactory.createArtworkFromFile(coverFile);
				AudioFile audioFile = AudioFileIO.read(new File(trackFolder+"/"+track.getFileName()));
				Tag tag = audioFile.getTag();
				tag.addField(cover);
				audioFile.commit();
				track.setHasCover(true);
				finded = true;
			} catch (IOException | TagException | CannotWriteException | CannotReadException
					| ReadOnlyFileException | InvalidAudioFrameException e) {}
		}
		return finded;
	}
}