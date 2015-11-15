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
 */

package com.musicott.task;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.error.ParseException;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.util.ItunesParserLogger;
import com.musicott.util.MetadataParser;
import com.worldsworstsoftware.itunes.ItunesLibrary;
import com.worldsworstsoftware.itunes.ItunesTrack;
import com.worldsworstsoftware.itunes.parser.ItunesLibraryParser;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.util.Duration;

/**
 * @author Octavio Calleya
 *
 */
public class ItunesImportTask extends Task<Void> {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	public static final int HOLD_METADATA_POLICY = 0;
	public static final int HOLD_ITUNES_DATA_POLICY = 1;
	
	private SceneManager sc;
	private MusicLibrary ml;
	private List<Track> tracks;
	private ItunesLibrary itunesLibrary;
	private Map<Integer, ItunesTrack> itunesItems;
	private final String itunesLibraryXMLPath;
	private final int metadataPolicy;
	private boolean importPlaylists, keepPlayCount;
	private int counter, totalItems;

	public ItunesImportTask(String path, int metadataPolicy, boolean importPlaylists, boolean keepPlaycount) {
		itunesLibraryXMLPath = path;
		this.metadataPolicy = metadataPolicy;
		this.importPlaylists = importPlaylists;
		this.keepPlayCount = keepPlaycount;
		tracks = new ArrayList<>();
		itunesItems = new HashMap<>();
		counter = 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Void call() throws Exception {
		ItunesParserLogger iLogger = new ItunesParserLogger();
		itunesLibrary = ItunesLibraryParser.parseLibrary(itunesLibraryXMLPath, iLogger);
		itunesItems = itunesLibrary.getTracks();
		totalItems = itunesItems.size();
		if(metadataPolicy == HOLD_METADATA_POLICY)
			parseItunesLibraryFiles();
		else if(metadataPolicy == HOLD_ITUNES_DATA_POLICY)
			parseItunesLibraryData();
		if(importPlaylists)
			parsePlaylists();
		return null;
	}
	
	private void parseItunesLibraryFiles() {
		for(ItunesTrack it: itunesItems.values()) {
			Track t = parseTrack(it);
			if(t != null)
				tracks.add(t);
			Platform.runLater(() -> sc.getRootController().setStatusProgress(++counter/totalItems));
		}
	}
	
	private void parseItunesLibraryData() {
		for(ItunesTrack it: itunesItems.values()) {
			tracks.add(createTrack(it));
			Platform.runLater(() -> sc.getRootController().setStatusProgress(++counter/totalItems));
		}
	}
	
	private void parsePlaylists() {
		// TODO
	}
	
	private Track createTrack(ItunesTrack iTrack) {
		File itunesFile = Paths.get(URI.create(iTrack.getLocation())).toFile();
		int index = itunesFile.toString().lastIndexOf(File.separator);
		String fileFolder = itunesFile.toString().substring(0, index);
		String fileName = itunesFile.toString().substring(index+1);
		Track track = new Track();
		track.setFileFolder(fileFolder);
		track.setFileName(fileName);
		track.setInDisk(itunesFile.exists());
		track.setSize(iTrack.getSize());
		track.setTotalTime(Duration.seconds(iTrack.getTotalTime()));
		track.setBitRate(iTrack.getBitRate());
		track.setName(iTrack.getName());
		track.setAlbum(iTrack.getAlbum());
		track.setAlbumArtist(iTrack.getAlbumArtist());
		track.setGenre(iTrack.getGenre());
		track.setLabel(iTrack.getGrouping());
		track.setCompilation(false);
		track.setBpm(iTrack.getBPM());
		track.setDiscNumber(iTrack.getDiscNumber());
		track.setTrackNumber(iTrack.getTrackNumber());
		track.setYear(iTrack.getYear());
		if(keepPlayCount)
			track.setPlayCount(iTrack.getPlayCount());
		AudioFile audioFile;
		try {
			audioFile = AudioFileIO.read(itunesFile);
			Tag tag = audioFile.getTag();
			track.setEncoder(tag.getFirst(FieldKey.ENCODER));
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException
				| InvalidAudioFrameException e) {
			LOG.warn("Error getting encoder from track {}: {}", track.getTrackID(), e.getMessage());
			Platform.runLater(() -> sc.getRootController().setStatusMessage("Error getting encoder from "+track.getName()));
		}
		return track;
	}
	
	private Track parseTrack(ItunesTrack iTrack) {
		File itunesFile = Paths.get(URI.create(iTrack.getLocation())).toFile();
		MetadataParser parser = new MetadataParser(itunesFile);
		Track newTrack = null;
		try {
			newTrack = parser.createTrack();
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException
				| InvalidAudioFrameException e) {
			ParseException pe = new ParseException("Error parsing "+itunesFile+": "+e.getMessage(), e, itunesFile);
			LOG.error(pe.getMessage(), pe);
			ErrorHandler.getInstance().addError(pe, ErrorType.PARSE);
			Platform.runLater(() -> sc.getRootController().setStatusMessage("Error: "+e.getMessage()));
		}
		if(newTrack != null && keepPlayCount)
			newTrack.setPlayCount(iTrack.getPlayCount());
		return newTrack;
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage("Itunes import succeeded");
		ml.getTracks().addAll(tracks);
		sc.getRootController().setStatusProgress(0.0);
		sc.getRootController().setStatusMessage("Itunes import completed");
		LOG.info("Itunes import task completed");
	}
	
	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Itunes import cancelled");
		LOG.info("Itunes import task cancelled");
	}
}