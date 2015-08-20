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

package com.musicott.task.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.util.Duration;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;

import com.musicott.error.WriteMetadataException;
import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class FlacParser {

	private static Track track;
	
	public static Track parseFlacFile(final File fileToParse) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, WriteMetadataException {
		track = new Track();
		AudioFile audioFile = AudioFileIO.read(fileToParse);
		FlacTag tag = (FlacTag) audioFile.getTag();
		track.setFileFolder(new File(fileToParse.getParent()).getAbsolutePath());
		track.setFileName(fileToParse.getName());
		track.setInDisk(true);
		track.setSize((int) (fileToParse.length()));
		track.setBitRate(Integer.parseInt(audioFile.getAudioHeader().getBitRate()));
		track.setTotalTime(Duration.seconds(audioFile.getAudioHeader().getTrackLength()));
		checkCover(tag);
		for(FieldKey t: FieldKey.values()) {
			switch (t){
				case TITLE:
					track.setName(tag.getFirst(t));
					break;
				case ARTIST:
					track.setArtist(tag.getFirst(t));
					break;
				case ALBUM:
					track.setAlbum(tag.getFirst(t));
					break;
				case ALBUM_ARTIST:
					track.setAlbumArtist(tag.getFirst(t));
					break;
				case BPM:
					try {
						track.setBpm(Integer.parseInt(tag.getFirst(t)));
					} catch (NumberFormatException e) {}
					break;
				case COMMENT:
					track.setComments(tag.getFirst(t));
					break;
				case GENRE:
					track.setGenre(tag.getFirst(t));
					break;
				case GROUPING:
					track.setLabel(tag.getFirst(t));
					break;
				case IS_COMPILATION:
					track.setCompilation(tag.getFirst(t).equals("true") ? Boolean.valueOf(tag.getFirst(t)) : false);
					break;
				case TRACK:
					try {
						track.setTrackNumber(Integer.parseInt(tag.getFirst(t)));
					} catch (NumberFormatException e) {}
					break;
				case DISC_NO:
					try {
						track.setDiscNumber(Integer.parseInt(tag.getFirst(t)));
					} catch (NumberFormatException e) {}
					break;
				case YEAR:
					try {
						track.setYear(Integer.parseInt(tag.getFirst(t)));
					} catch (NumberFormatException e) {}
					break;
				default:
			}
		}
		return track;
	}
	
	private static void checkCover(FlacTag tag) throws WriteMetadataException {
		if(!tag.getArtworkList().isEmpty())
			track.setHasCover(true);
		else {
			File f = new File(track.getFileFolder()+"/cover.jpg");
			if(f.exists()) {
				try {
					track.setCoverFile(Files.readAllBytes(Paths.get(f.getPath())),"jpg");
					track.setHasCover(true);
				} catch (IOException e) {}
			}
			else {
				f = new File(track.getFileFolder()+"/cover.jpeg");
				if(f.exists()) {
					try {
						track.setCoverFile(Files.readAllBytes(Paths.get(f.getPath())),"jpeg");
						track.setHasCover(true);
					} catch (IOException e) {}
				}				
				else {
					f = new File(track.getFileFolder()+"/cover.png");
					if(f.exists()) {
						try {
							track.setCoverFile(Files.readAllBytes(Paths.get(f.getPath())),"png");
							track.setHasCover(true);
						} catch (IOException e) {}
					}
					else
						track.setHasCover(false);
				}
			}
		}
	}
}