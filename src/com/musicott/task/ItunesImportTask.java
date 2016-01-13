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
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.error.ErrorHandler;
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
	private ItunesLibrary itunesLibrary;
	private Map<Integer, ItunesTrack> itunesItems;
	private Map<Integer, Track> tracks;
	private final String itunesLibraryXMLPath;
	private final int metadataPolicy;
	private boolean importPlaylists, keepPlayCount;
	private int currentItems, totalItems;
	private List<String> notFoundFiles, parseErrors;
	private long startMillis, totalTime;

	public ItunesImportTask(String path, int metadataPolicy, boolean importPlaylists, boolean keepPlaycount) {
		sc = SceneManager.getInstance();
		ml = MusicLibrary.getInstance();
		itunesLibraryXMLPath = path;
		this.metadataPolicy = metadataPolicy;
		this.importPlaylists = importPlaylists;
		this.keepPlayCount = keepPlaycount;
		tracks = new HashMap<>();
		notFoundFiles = new ArrayList<>();
		parseErrors = new ArrayList<>();
		itunesItems = new HashMap<>();
		currentItems = 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Void call() throws Exception {
		startMillis = System.currentTimeMillis();
		ItunesParserLogger iLogger = new ItunesParserLogger();
		itunesLibrary = ItunesLibraryParser.parseLibrary(itunesLibraryXMLPath, iLogger);
		itunesItems = itunesLibrary.getTracks();
		totalItems = itunesItems.size();
		parseItunesLibrary();
		if(importPlaylists)
			parsePlaylists();
		return null;
	}
	
	private void parseItunesLibrary() {
		for(ItunesTrack it: itunesItems.values()) {
			Track track = null;
			if(isValidItunesTrack(it)) {
				if(metadataPolicy == HOLD_METADATA_POLICY)
					track = parseTrack(it);
				else if(metadataPolicy == HOLD_ITUNES_DATA_POLICY)
					track = convertTrack(it);
				if(track != null) {
					tracks.put(track.getTrackID(), track);
				}
			}
			Platform.runLater(() -> {
				sc.getRootController().setStatusMessage("Imported "+currentItems+" of "+totalItems);
				sc.getRootController().setStatusProgress((double) ++currentItems/totalItems);
			});
		}
	}
	
	private void parsePlaylists() {
		// TODO
	}
	
	private boolean isValidItunesTrack(ItunesTrack iTrack) {
		boolean valid = true;
		if(iTrack.getTrackType().equals("URL") || iTrack.getTrackType().equals("Remote"))
			valid = false;
		else {
			File itunesFile = Paths.get(URI.create(iTrack.getLocation())).toFile();
			int index = itunesFile.toString().lastIndexOf(".");
			String fileExtension = itunesFile.toString().substring(index+1);
			 if(!(fileExtension.equals("mp3") || fileExtension.equals("m4a") || fileExtension.equals("wav")))
					valid = false;
		}
		return valid;
	}
	
	private Track convertTrack(ItunesTrack iTrack) {
		File itunesFile = Paths.get(URI.create(iTrack.getLocation())).toFile();
		int index = itunesFile.toString().lastIndexOf(File.separator);
		String fileFolder = itunesFile.toString().substring(0, index);
		String fileName = itunesFile.toString().substring(index+1);
		Track newTrack = null;
		if(itunesFile.exists()) {
			newTrack = new Track();
			newTrack.setFileFolder(fileFolder);
			newTrack.setFileName(fileName);
			newTrack.setInDisk(itunesFile.exists());
			newTrack.setSize(iTrack.getSize());
			newTrack.setTotalTime(Duration.millis(iTrack.getTotalTime()));
			newTrack.setName(iTrack.getName() == null ? "" : iTrack.getName());
			newTrack.setAlbum(iTrack.getAlbum() == null ? "" : iTrack.getAlbum());
			newTrack.setArtist(iTrack.getArtist() == null ? "" : iTrack.getArtist());
			newTrack.setAlbumArtist(iTrack.getAlbumArtist() == null ? "" : iTrack.getAlbumArtist());
			newTrack.setGenre(iTrack.getGenre() == null ? "" : iTrack.getGenre());
			newTrack.setLabel(iTrack.getGrouping() == null ? "" : iTrack.getGrouping());
			newTrack.setCompilation(false);
			newTrack.setBpm(iTrack.getBPM() < 1 ? 0 : iTrack.getBPM());
			newTrack.setDiscNumber(iTrack.getDiscNumber() < 1 ? 0 : iTrack.getDiscNumber());
			newTrack.setTrackNumber(iTrack.getTrackNumber() < 1 ? 0 : iTrack.getTrackNumber());
			newTrack.setYear(iTrack.getYear() < 1 ? 0 : iTrack.getYear());
			if(keepPlayCount)
				newTrack.setPlayCount(iTrack.getPlayCount() < 1 ? 0 : iTrack.getPlayCount());
			AudioFile audioFile;
			try {
				audioFile = AudioFileIO.read(itunesFile);
				newTrack.setEncoding(audioFile.getAudioHeader().getEncodingType());
				newTrack.setEncoder(audioFile.getTag().getFirst(FieldKey.ENCODER));
				MetadataParser.checkCoverImage(newTrack, audioFile.getTag());
				String bitRate = audioFile.getAudioHeader().getBitRate();
				if(bitRate.substring(0, 1).equals("~")) {
					newTrack.setIsVariableBitRate(true);
					bitRate = bitRate.substring(1);
				}
				newTrack.setBitRate(Integer.parseInt(bitRate));
			} catch (CannotReadException | IOException | TagException | ReadOnlyFileException
					| InvalidAudioFrameException e) {
				LOG.warn("Error getting encoder or bitrate from track {}: {}", newTrack.getTrackID(), e.getMessage());
			}
		}
		else
			notFoundFiles.add(itunesFile.toString());
		return newTrack;
	}
	
	private Track parseTrack(ItunesTrack iTrack) {
		File itunesFile = Paths.get(URI.create(iTrack.getLocation())).toFile();
		Track newTrack = null;
		if(itunesFile.exists()) {
			MetadataParser parser = new MetadataParser(itunesFile);
			try {
				newTrack = parser.createTrack();
			} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
				LOG.error("Error parsing "+itunesFile, e);
				parseErrors.add(itunesFile+e.getMessage());
			}
			if(newTrack != null && keepPlayCount)
				newTrack.setPlayCount(iTrack.getPlayCount() < 1 ? 0 : iTrack.getPlayCount());
		}
		else
			notFoundFiles.add(itunesFile.toString());
		return newTrack;
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage("Itunes import succeeded");
		sc.getRootController().setStatusProgress(-1);
		new Thread(() -> {
			ml.addTracks(tracks);
			totalTime = System.currentTimeMillis() - startMillis;
			Platform.runLater(() -> {
				sc.closeIndeterminatedProgressScene();
				sc.getRootController().setStatusProgress(0.0);
				sc.getRootController().setStatusMessage("Imported "+tracks.size()+" files in "+Duration.millis(totalTime).toSeconds()+" seconds");
			});
		}).start();
		sc.openIndeterminatedProgressScene();
		if(!notFoundFiles.isEmpty())
			ErrorHandler.getInstance().showExpandableErrorsDialog("Some files were not found", "", notFoundFiles);
		if(!parseErrors.isEmpty())
			ErrorHandler.getInstance().showExpandableErrorsDialog("Errors importing files", "", parseErrors);
		LOG.info("Itunes import task completed");
	}
	
	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Itunes import cancelled");
		LOG.info("Itunes import task cancelled");
	}
}