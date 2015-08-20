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
import org.jaudiotagger.tag.TagException;

import com.musicott.error.WriteMetadataException;
import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class WavParser {

	private static Track track;
	
	public static Track parseWavFile(final File fileToParse) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, WriteMetadataException {
		track = new Track();
		AudioFile audioFile = AudioFileIO.read(fileToParse);
		track.setFileFolder(new File(fileToParse.getParent()).getAbsolutePath());
		track.setFileName(fileToParse.getName());
		track.setInDisk(true);
		track.setSize((int) (fileToParse.length()));
		track.setBitRate(Integer.parseInt(audioFile.getAudioHeader().getBitRate()));
		track.setTotalTime(Duration.seconds(audioFile.getAudioHeader().getTrackLength()));
		checkCover(track);
		track.setName(track.getFileName());
		return track;
	}
	
	private static void checkCover(Track track) throws WriteMetadataException {
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