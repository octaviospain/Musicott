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

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.musicott.error.WriteMetadataException;
import com.musicott.model.Track;

import javafx.util.Duration;

/**
 * @author Octavio Calleya
 *
 */
public class Mp3Parser {
	
	private static File file;
	private static Mp3File mp3File;

	public static Track parseMp3File(final File fileToParse) throws UnsupportedTagException, InvalidDataException, IOException, WriteMetadataException {
		file = fileToParse;
		Track mp3Track = new Track();
		mp3File = new Mp3File(file);
		mp3Track.setFileFolder(new File(file.getParent()).getAbsolutePath());
		mp3Track.setFileName(file.getName());
		checkCover(mp3Track);
		mp3Track.setInDisk(true);
		mp3Track.setSize((int) (file.length()));
		mp3Track.setBitRate(mp3File.getBitrate());
		mp3Track.setTotalTime(Duration.seconds(mp3File.getLengthInSeconds()));
		if(mp3File.hasId3v2Tag())
			readId3v2Tag(mp3Track);
		else
			if(mp3File.hasId3v1Tag())
				readId3v1Tag(mp3Track);
			else {
				mp3Track.setName(file.getName());
			}
		return mp3Track;
	}
	
	private static void readId3v2Tag(Track track) {
		ID3v2 tag = mp3File.getId3v2Tag();
		if(tag.getTitle() != null)
			track.setName(tag.getTitle());
		if(tag.getArtist() != null)
			track.setArtist(tag.getArtist());
		if(tag.getAlbum() != null)
			track.setAlbum(tag.getAlbum());
		if(tag.getAlbumArtist() != null)
			track.setAlbumArtist(tag.getAlbumArtist());
		track.setBpm(tag.getBPM());
		if(tag.getComment() != null)
			track.setComments(tag.getComment());
		if(tag.getGenreDescription() != null)
		track.setGenre(tag.getGenreDescription());
		if(tag.getGrouping() != null)
			track.setLabel(tag.getGrouping());
		track.setCompilation(tag.isCompilation());
		try {
			track.setTrackNumber(Integer.valueOf(tag.getTrack()));
			track.setYear(Integer.valueOf(tag.getYear()));
			track.setDiscNumber(Integer.valueOf(tag.getPartOfSet()));
		} catch (NumberFormatException e) {
			
		}
		//TODO Think what to do with trackId here
	}
	
	private static void readId3v1Tag(Track track) {
		ID3v1 tag = mp3File.getId3v1Tag();
		if(tag.getTitle() != null)
			track.setName(tag.getTitle());
		if(tag.getArtist() != null)
			track.setArtist(tag.getArtist());
		if(tag.getAlbum() != null)
			track.setAlbum(tag.getAlbum());
		if(tag.getComment() != null)
			track.setComments(tag.getComment());
		if(tag.getGenreDescription() != null)
			track.setGenre(tag.getGenreDescription());
		try {
			track.setTrackNumber(Integer.valueOf(tag.getTrack()));
			track.setYear(Integer.valueOf(tag.getYear()));
		} catch (NumberFormatException e) {}
	}
		
	private static void checkCover(Track track) throws WriteMetadataException {
		if(mp3File.hasId3v2Tag()) {
			byte[] img = mp3File.getId3v2Tag().getAlbumImage();
			if(img != null)
				track.setHasCover(true);
		}
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