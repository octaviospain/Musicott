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

package com.musicott.tasks;

import com.musicott.*;
import com.musicott.model.*;
import com.musicott.util.*;
import com.worldsworstsoftware.itunes.*;
import com.worldsworstsoftware.itunes.parser.*;
import javafx.application.*;
import javafx.concurrent.*;
import javafx.scene.control.*;
import javafx.stage.*;
import javafx.util.*;
import org.jaudiotagger.audio.*;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.tag.*;
import org.slf4j.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * @author Octavio Calleya
 *
 */
public class ItunesImportTask extends Task<Void> {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	public static final int METADATA_POLICY = 0;
	public static final int ITUNES_DATA_POLICY = 1;
	
	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	private ItunesLibrary itunesLibrary;
	private Map<Integer, ItunesTrack> itunesItems;
	private Map<Integer, Track> tracks;
	private List<Playlist> playlists;
	private final String itunesLibraryXMLPath;
	private final int metadataPolicy;
	private boolean importPlaylists, holdPlayCount;
	private int currentItems, totalItems, numPlaylists;
	private List<String> notFoundFiles, parseErrors;
	private List<ItunesPlaylist> itunesPlaylists;
	private Map<Integer, Integer> itunesIDtoMusicottIDMap;
	private long startMillis, totalTime;
	private Semaphore waitConfirmationSemaphore;

	public ItunesImportTask(String path) {
		itunesLibraryXMLPath = path;
		metadataPolicy = MainPreferences.getInstance().getItunesImportMetadataPolicy();
		importPlaylists = MainPreferences.getInstance().getItunesImportPlaylists();
		holdPlayCount = MainPreferences.getInstance().getItunesImportHoldPlaycount();
		tracks = new HashMap<>();
		notFoundFiles = new ArrayList<>();
		parseErrors = new ArrayList<>();
		itunesItems = new HashMap<>();
		playlists = new ArrayList<>();
		itunesIDtoMusicottIDMap = new HashMap<>();
		waitConfirmationSemaphore = new Semaphore(0);
		currentItems = 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Void call() throws Exception {
		if(isValidItunesXML()) {
			Platform.runLater(() -> {stageDemon.getNavigationController().setStatusMessage("Scanning itunes library..."); stageDemon.getNavigationController().setStatusProgress(-1);});
			startMillis = System.currentTimeMillis();
			ItunesParserLogger iLogger = new ItunesParserLogger();
			itunesLibrary = ItunesLibraryParser.parseLibrary(itunesLibraryXMLPath, iLogger);
			itunesItems = itunesLibrary.getTracks();
			totalItems = itunesItems.size();
			itunesPlaylists = ((List<ItunesPlaylist>) itunesLibrary.getPlaylists());
			itunesPlaylists = itunesPlaylists.stream().filter(pl -> !pl.getName().equals("####!####") && pl.isAllItems() && !pl.getPlaylistItems().isEmpty()).collect(Collectors.toList());
			
			numPlaylists = itunesPlaylists.size();
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());
				alert.setTitle("Import");
				alert.setHeaderText("Import " + totalItems + " tracks"+(importPlaylists ? " and "+numPlaylists+" playlists" : "")+" from itunes?");
				alert.initModality(Modality.APPLICATION_MODAL);
				Optional<ButtonType> result = alert.showAndWait();
				if(result.isPresent() && result.get().equals(ButtonType.OK)) {
					waitConfirmationSemaphore.release();
					stageDemon.getNavigationController().setStatusMessage("Importing files");
				}
				else
					cancel();
			});
		} else {
			ErrorDemon.getInstance().showErrorDialog("The seleted xml file is not valid");
			cancel();
		}
		waitConfirmationSemaphore.acquire();
		parseItunesLibrary();
		if(importPlaylists)
			parsePlaylists();
		return null;
	}
	
	private void parseItunesLibrary() {
		for(ItunesTrack it: itunesItems.values()) {
			Track track = null;
			if(isValidItunesTrack(it)) {
				if(metadataPolicy == METADATA_POLICY)
					track = parseTrack(it);
				else if(metadataPolicy == ITUNES_DATA_POLICY)
					track = convertTrack(it);
				if(track != null) {
					itunesIDtoMusicottIDMap.put(it.getTrackID(), track.getTrackID());
					tracks.put(track.getTrackID(), track);
				}
			}
			Platform.runLater(() -> {
				stageDemon.getNavigationController().setStatusProgress((double) ++currentItems/totalItems);
				stageDemon.getNavigationController().setStatusMessage("Imported "+currentItems+" of "+totalItems);
			});
		}
	}

	@SuppressWarnings("unchecked")
	private void parsePlaylists() {
		for(ItunesPlaylist itpls: itunesPlaylists) {
			Playlist pl = new Playlist(itpls.getName(), false);
			List<ItunesTrack> itunesItems = itpls.getPlaylistItems();
			for(ItunesTrack itks: itunesItems)
				if(isValidItunesTrack(itks) && itunesIDtoMusicottIDMap.containsKey(itks.getTrackID()))
					pl.getTracks().add(itunesIDtoMusicottIDMap.get(itks.getTrackID()));
			if(!pl.getTracks().isEmpty())
				playlists.add(pl);
		}
	}
	
	private boolean isValidItunesXML() {
		boolean valid = true;
		Scanner scnr;
		try {
			scnr = new Scanner(new File(itunesLibraryXMLPath));
			scnr.useDelimiter(Pattern.compile(">"));
			if(!(scnr.hasNextLine() && scnr.nextLine().contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")))
					valid = false;
			if(!(scnr.hasNextLine() && scnr.nextLine().contains("<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">")))
					valid = false;
			scnr.close();
		} catch (FileNotFoundException e) {
			valid = false;
		}
		return valid;
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
			if(holdPlayCount)
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
			if(newTrack != null && holdPlayCount)
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
		new Thread(() -> {
			musicLibrary.addTracks(tracks);
			int i = 0;
			for(Playlist p: playlists) {
				musicLibrary.addPlaylist(p);				
				Platform.runLater(() -> {
					stageDemon.getNavigationController().setStatusProgress((double) i / playlists.size());
					stageDemon.getNavigationController().addNewPlaylist(p);
				});
			}
			totalTime = System.currentTimeMillis() - startMillis;
			Platform.runLater(() -> {
				stageDemon.closeIndeterminateProgress();
				stageDemon.getNavigationController().setStatusProgress(0);
				stageDemon.getNavigationController().setStatusMessage(tracks.size()+" ("+Duration.millis(totalTime).toMinutes()+") mins");
			});
		}).start();
		stageDemon.showIndeterminateProgress();
		if(!notFoundFiles.isEmpty())
			ErrorDemon.getInstance().showExpandableErrorsDialog("Some files were not found", "", notFoundFiles);
		if(!parseErrors.isEmpty())
			ErrorDemon.getInstance().showExpandableErrorsDialog("Errors importing files", "", parseErrors);
		LOG.info("Itunes import task completed");
	}
	
	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Itunes import cancelled");
		LOG.info("Itunes import task cancelled");
	}
}
