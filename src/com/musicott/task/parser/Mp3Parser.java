package com.musicott.task.parser;

import java.io.File;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.musicott.model.Track;

public class Mp3Parser {
	
	private static File file;

	public static Track parseMp3File(final File fileToParse) {
		file = fileToParse;
		Track mp3Track = new Track();
		try {
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
		} catch (Exception e) {
			//TODO Save and inform the error
			e.printStackTrace();
		}
		return mp3Track;
	}
	
	private static void readId3v2Tag(Track track, Mp3File file) throws NumberFormatException{
		ID3v2 tag = file.getId3v2Tag();
		track.getName().set((tag.getTitle()));
		track.getArtist().set(tag.getArtist());
		track.getAlbum().set(tag.getAlbum());
		track.getAlbumArtist().set(tag.getAlbumArtist());
		track.getBPM().set(tag.getBPM());
		track.getComments().set(tag.getComment());
		track.getTrackNumber().set(Integer.valueOf(tag.getTrack()));
		track.getGenre().set(tag.getGenreDescription());
		track.getLabel().set(tag.getGrouping());
		track.getTotalTime().set(tag.getLength());
		track.getYear().set(Integer.valueOf(tag.getYear()));

		//TODO Think what to do with trackId here
		//TODO Missing set DiscNumber in mp3agic Mp3File -- not in Track
	}
	
	private static void readId3v1Tag(Track track, Mp3File file) throws NumberFormatException{
		ID3v1 tag = file.getId3v1Tag();
		track.getName().set(tag.getTitle());
		track.getArtist().set(tag.getArtist());
		track.getAlbum().set(tag.getAlbum());
		track.getComments().set(tag.getComment());
		track.getGenre().set(tag.getGenreDescription());
		track.getTrackNumber().set(Integer.valueOf(tag.getTrack()));
		track.getYear().set(Integer.valueOf(tag.getYear()));
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