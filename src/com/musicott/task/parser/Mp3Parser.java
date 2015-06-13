package com.musicott.task.parser;

import java.io.File;
import java.io.IOException;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ParseException;
import com.musicott.model.Track;

public class Mp3Parser {
	
	private static File file;

	public static Track parseMp3File(final File fileToParse) throws UnsupportedTagException, InvalidDataException, IOException {
		file = fileToParse;
		Track mp3Track = new Track();
		Mp3File mp3File = new Mp3File(file);
		mp3Track.setFileFolder(new File(file.getParent()).getAbsolutePath());
		mp3Track.setFileName(file.getName());
		checkCover(mp3Track);
		mp3Track.getIsInDisk().set(true);
		mp3Track.getSize().set((int) (file.length()));
		
		if(mp3File.hasId3v2Tag())
			readId3v2Tag(mp3Track, mp3File);
		else
			if(mp3File.hasId3v1Tag())
				readId3v1Tag(mp3Track, mp3File);
			else {
				mp3Track.getName().set(file.getName());
			}
		return mp3Track;
	}
	
	private static void readId3v2Tag(Track track, Mp3File file) {
		ID3v2 tag = file.getId3v2Tag();
		if(tag.getTitle() != null)
			track.getName().set((tag.getTitle()));
		if(tag.getArtist() != null)
			track.getArtist().set(tag.getArtist());
		if(tag.getAlbum() != null)
			track.getAlbum().set(tag.getAlbum());
		if(tag.getAlbumArtist() != null)
			track.getAlbumArtist().set(tag.getAlbumArtist());
		track.getBPM().set(tag.getBPM());
		if(tag.getComment() != null)
			track.getComments().set(tag.getComment());
		if(tag.getGenreDescription() != null)
		track.getGenre().set(tag.getGenreDescription());
		if(tag.getGrouping() != null)
			track.getLabel().set(tag.getGrouping());
		track.getTotalTime().set(tag.getLength());
		try {
			track.getTrackNumber().set(Integer.valueOf(tag.getTrack()));
			track.getYear().set(Integer.valueOf(tag.getYear()));
		} catch (NumberFormatException e) {
			track.getTrackNumber().set(-1);
			track.getYear().set(-1);
		}

		//TODO Think what to do with trackId here
		//TODO Missing set DiscNumber in mp3agic Mp3File -- not in Track
	}
	
	private static void readId3v1Tag(Track track, Mp3File file) {
		ID3v1 tag = file.getId3v1Tag();
		if(tag.getTitle() != null)
			track.getName().set(tag.getTitle());
		if(tag.getArtist() != null)
			track.getArtist().set(tag.getArtist());
		if(tag.getAlbum() != null)
			track.getAlbum().set(tag.getAlbum());
		if(tag.getComment() != null)
			track.getComments().set(tag.getComment());
		if(tag.getGenreDescription() != null)
			track.getGenre().set(tag.getGenreDescription());
		try {
			track.getTrackNumber().set(Integer.valueOf(tag.getTrack()));
			track.getYear().set(Integer.valueOf(tag.getYear()));
		} catch (NumberFormatException e) {
			track.getTrackNumber().set(-1);
			track.getYear().set(-1);
		}
	}
	
	private static void checkCover(Track track) {
		if(new File(file.getParentFile().getAbsolutePath()+"/cover.jpg").exists())
			track.getHasCover().set(true);
		else
			if(new File(file.getParentFile().getAbsolutePath()+"/cover.jpeg").exists())
				track.getHasCover().set(true);
			else
				if(new File(file.getParentFile().getAbsolutePath()+"/cover.png").exists())
					track.getHasCover().set(true);
				else
					track.getHasCover().set(false);
		
		//TODO Check for the cover image in the ID3Layer
	}
}