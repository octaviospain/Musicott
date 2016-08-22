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

package com.transgressoft.musicott.tasks;

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.view.*;
import com.worldsworstsoftware.itunes.*;
import com.worldsworstsoftware.itunes.parser.*;
import javafx.application.*;
import javafx.concurrent.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
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

import static com.transgressoft.musicott.view.MusicottController.*;

/**
 * Extends from {@link Task} to perform the operation of import a
 * <tt>iTunes</tt> library to the {@link MusicLibrary} of the application.
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 */
public class ItunesImportTask extends Task<Void> {

	public static final int METADATA_POLICY = 0;
	public static final int ITUNES_DATA_POLICY = 1;
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private final String itunesLibraryXmlPath;
	private final int metadataPolicy;

	private Semaphore waitConfirmationSemaphore;

	private Map<Integer, Integer> itunesIdToMusicottIdMap;
	private Map<Integer, ItunesTrack> itunesTracks;
	private List<ItunesPlaylist> itunesPlaylists;
	private Map<Integer, Track> tracks;
	private List<Playlist> playlists;

	private List<String> notFoundFiles;
	private List<String> parseErrors;

	private boolean importPlaylists;
	private boolean holdPlayCount;
	private volatile int currentItunesTracks;
	private volatile int totalItunesTracks;
	private int currentPlaylists;
	private int totalTracks;
	private long startMillis;
	private String playlistsAlertText;

	private NavigationController navigationController;
	private StageDemon stageDemon = StageDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	private ErrorDemon errorDemon = ErrorDemon.getInstance();

	public ItunesImportTask(String path) {
		super();
		itunesLibraryXmlPath = path;
		MainPreferences mainPreferences = MainPreferences.getInstance();
		metadataPolicy = mainPreferences.getItunesImportMetadataPolicy();
		importPlaylists = mainPreferences.getItunesImportPlaylists();
		holdPlayCount = mainPreferences.getItunesImportHoldPlaycount();
		navigationController = stageDemon.getNavigationController();
		itunesIdToMusicottIdMap = new HashMap<>();
		itunesTracks = new HashMap<>();
		playlists = new ArrayList<>();
		tracks = new HashMap<>();
		notFoundFiles = new ArrayList<>();
		parseErrors = new ArrayList<>();
		waitConfirmationSemaphore = new Semaphore(0);
		currentItunesTracks = 0;
		currentPlaylists = 0;
	}

	@Override
	protected Void call() throws Exception {
		if (isValidItunesXML())
			askForUserConfirmation();
		else {
			errorDemon.showErrorDialog("The selected xml file is not valid");
			cancel();
		}
		waitConfirmationSemaphore.acquire();

		startMillis = System.currentTimeMillis();
		itunesTracks.values().parallelStream().forEach(this::parseItunesTrack);

		if (importPlaylists)
			parsePlaylists();
		return null;
	}

	private boolean isValidItunesXML() {
		boolean valid = true;
		Scanner scanner;
		String itunesXmlSampleLine = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		String itunesPlistSampleLine = "<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" " +
				"\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">";
		try {
			scanner = new Scanner(new File(itunesLibraryXmlPath));
			scanner.useDelimiter(Pattern.compile(">"));
			if (! (scanner.hasNextLine() && scanner.nextLine().contains(itunesXmlSampleLine)))
				valid = false;
			if (! (scanner.hasNextLine() && scanner.nextLine().contains(itunesPlistSampleLine)))
				valid = false;
			scanner.close();
		}
		catch (FileNotFoundException exception) {
			LOG.info("Error accessing to the itunes xml: ", exception);
			errorDemon.showErrorDialog("Error opening the iTunes Library file", "", exception);
			valid = false;
		}
		return valid;
	}

	/**
	 * Calculates the total number of tracks and playlists to import
	 * and ask the user for a confirmation to continue.
	 */
	@SuppressWarnings ("unchecked")
	private void askForUserConfirmation() {
		Platform.runLater(() -> {
			navigationController.setStatusMessage("Scanning itunes library...");
			navigationController.setStatusProgress(- 1);
		});
		ItunesParserLogger itunesLogger = new ItunesParserLogger();
		ItunesLibrary itunesLibrary = ItunesLibraryParser.parseLibrary(itunesLibraryXmlPath, itunesLogger);
		itunesTracks = itunesLibrary.getTracks();
		totalItunesTracks = itunesTracks.size();

		playlistsAlertText = "";
		if (importPlaylists) {
			itunesPlaylists = (List<ItunesPlaylist>) itunesLibrary.getPlaylists();
			itunesPlaylists = itunesPlaylists.stream().filter(this::isValidItunesPlaylist).collect(Collectors.toList());

			totalTracks = itunesPlaylists.size();
			playlistsAlertText += "and " + Integer.toString(totalTracks) + " playlists ";
		}
		Platform.runLater(this::showConfirmationAlert);
	}

	private void showConfirmationAlert() {
		String itunesTracksString = Integer.toString(totalItunesTracks);
		String headerText = "Import " + itunesTracksString + " tracks " + playlistsAlertText + "from itunes?";

		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.getDialogPane().getStylesheets().add(getClass().getResource(DIALOG_STYLE).toExternalForm());
		alert.setTitle("Import");
		alert.initModality(Modality.APPLICATION_MODAL);
		alert.setHeaderText(headerText);

		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get().equals(ButtonType.OK)) {
			waitConfirmationSemaphore.release();
			navigationController.setStatusMessage("Importing files");
		}
		else
			cancel();
	}

	private boolean isValidItunesPlaylist(ItunesPlaylist itunesPlaylist) {
		return ! "####!####".equals(itunesPlaylist.getName()) &&
				itunesPlaylist.isAllItems() &&
				! itunesPlaylist.getPlaylistItems().isEmpty();
	}

	private void parseItunesTrack(ItunesTrack itunesTrack) {
		Optional<Track> newTrack = Optional.empty();
		if (isValidItunesTrack(itunesTrack)) {
			if (metadataPolicy == METADATA_POLICY)
				newTrack = createTrackFromFileMetadata(itunesTrack);
			else if (metadataPolicy == ITUNES_DATA_POLICY)
				newTrack = createTrackFromItunesData(itunesTrack);

			newTrack.ifPresent(track -> {
				itunesIdToMusicottIdMap.put(itunesTrack.getTrackID(), track.getTrackId());
				tracks.put(track.getTrackId(), track);
			});
		}

		double progress = (double) ++ currentItunesTracks / totalItunesTracks;
		String progressMessage = Integer.toString(currentItunesTracks) + " / " + Integer.toString(totalItunesTracks);
		Platform.runLater(() -> updateTaskProgressOnView(progress, progressMessage));
	}

	/**
	 * Parse the itunes playlists into {@link Playlist} objects.
	 */
	@SuppressWarnings ("unchecked")
	private void parsePlaylists() {
		for (ItunesPlaylist itunesPlaylist: itunesPlaylists) {
			Playlist playlist = new Playlist(itunesPlaylist.getName(), false);
			List<ItunesTrack> itunesTracksList = itunesPlaylist.getPlaylistItems();
			List<Integer> playlistTracksIds = getTracksAlreadyParsed(itunesTracksList);
			playlist.addTracks(playlistTracksIds);

			double progress = (double) ++ currentPlaylists / totalTracks;
			String progressMessage = Integer.toString(currentPlaylists) + " / " + Integer.toString(totalTracks);
			Platform.runLater(() -> updateTaskProgressOnView(progress, progressMessage));

			if (! playlist.isEmpty())
				playlists.add(playlist);
		}
	}

	private boolean isValidItunesTrack(ItunesTrack itunesTrack) {
		boolean valid = true;
		if ("URL".equals(itunesTrack.getTrackType()) || "Remote".equals(itunesTrack.getTrackType()))
			valid = false;
		else {
			File itunesFile = Paths.get(URI.create(itunesTrack.getLocation())).toFile();
			int index = itunesFile.toString().lastIndexOf('.');
			String fileExtension = itunesFile.toString().substring(index + 1);
			if (! ("mp3".equals(fileExtension) || "m4a".equals(fileExtension) || "wav".equals(fileExtension)))
				valid = false;
		}
		return valid;
	}

	/**
	 * Creates a {@link Track} instance from the audio file metadata
	 *
	 * @param itunesTrack The {@link ItunesTrack} object
	 *
	 * @return The <tt>Track</tt> instance if the parse was successful
	 */
	private Optional<Track> createTrackFromFileMetadata(ItunesTrack itunesTrack) {
		File itunesFile = Paths.get(URI.create(itunesTrack.getLocation())).toFile();
		Optional<Track> parsedTrack = Optional.empty();
		if (itunesFile.exists()) {
			try {
				parsedTrack = Optional.of(MetadataParser.createTrack(itunesFile));
			}
			catch (TrackParseException exception) {
				LOG.error("Error parsing {}", itunesFile, exception.getCause());
				parseErrors.add(itunesFile + exception.getMessage());
			}
			if (parsedTrack.isPresent() && holdPlayCount)
				parsedTrack.get().setPlayCount(itunesTrack.getPlayCount() < 1 ? 0 : itunesTrack.getPlayCount());
		}
		else
			notFoundFiles.add(itunesFile.toString());
		return parsedTrack;
	}

	/**
	 * Creates a {@link Track} instance from the data stored on the <tt>iTunes</tt> library
	 *
	 * @param itunesTrack The {@link ItunesTrack} object
	 *
	 * @return The <tt>Track</tt> instance if the parse was successful
	 */
	private Optional<Track> createTrackFromItunesData(ItunesTrack itunesTrack) {
		Path itunesPath = Paths.get(URI.create(itunesTrack.getLocation()));
		File itunesFile = itunesPath.toFile();
		Optional<Track> newTrack = Optional.empty();
		if (itunesFile.exists()) {
			Track track = parseItunesFieldsToTrackFields(itunesTrack, itunesPath);
			newTrack = Optional.of(track);
		}
		else
			notFoundFiles.add(itunesFile.toString());
		return newTrack;
	}

	/**
	 * Updates on the view the progress and a message of the task.
	 * Should be called on the JavaFX Application Thread.
	 */
	private void updateTaskProgressOnView(double progress, String message) {
		navigationController.setStatusProgress(progress);
		navigationController.setStatusMessage(message);
	}

	/**
	 * Retrieves a {@link List} of identification integers of {@link Track}
	 * instances that have been already parsed, if so.
	 *
	 * @param itunesTracks The <tt>List</tt> of {@link ItunesTrack} instances
	 *
	 * @return The <tt>List</tt> of integers that are the ids of the <tt>tracks</tt>
	 */
	private List<Integer> getTracksAlreadyParsed(List<ItunesTrack> itunesTracks) {
		return itunesTracks.stream().filter(itunesTrack -> isValidItunesTrack(itunesTrack) && itunesIdToMusicottIdMap
																				.containsKey(itunesTrack.getTrackID()))
						   .map(itunesTrack -> itunesIdToMusicottIdMap.get(itunesTrack.getTrackID()))
						   .collect(Collectors.toList());
	}

	private Track parseItunesFieldsToTrackFields(ItunesTrack itunesTrack, Path itunesPath) {
		String fileFolder = itunesPath.getParent().toString();
		String fileName = itunesPath.getName(itunesPath.getNameCount() - 1).toString();
		Track newTrack = new Track();
		newTrack.setFileFolder(fileFolder);
		newTrack.setFileName(fileName);
		newTrack.setInDisk(true);
		newTrack.setSize(itunesTrack.getSize());
		newTrack.setTotalTime(Duration.millis(itunesTrack.getTotalTime()));
		newTrack.setName(itunesTrack.getName() == null ? "" : itunesTrack.getName());
		newTrack.setAlbum(itunesTrack.getAlbum() == null ? "" : itunesTrack.getAlbum());
		newTrack.setArtist(itunesTrack.getArtist() == null ? "" : itunesTrack.getArtist());
		newTrack.setAlbumArtist(itunesTrack.getAlbumArtist() == null ? "" : itunesTrack.getAlbumArtist());
		newTrack.setGenre(itunesTrack.getGenre() == null ? "" : itunesTrack.getGenre());
		newTrack.setLabel(itunesTrack.getGrouping() == null ? "" : itunesTrack.getGrouping());
		newTrack.setIsPartOfCompilation(false);
		newTrack.setBpm(itunesTrack.getBPM() < 1 ? 0 : itunesTrack.getBPM());
		newTrack.setDiscNumber(itunesTrack.getDiscNumber() < 1 ? 0 : itunesTrack.getDiscNumber());
		newTrack.setTrackNumber(itunesTrack.getTrackNumber() < 1 ? 0 : itunesTrack.getTrackNumber());
		newTrack.setYear(itunesTrack.getYear() < 1 ? 0 : itunesTrack.getYear());

		if (holdPlayCount)
			newTrack.setPlayCount(itunesTrack.getPlayCount() < 1 ? 0 : itunesTrack.getPlayCount());
		setEncoderAndBitRateToTrack(itunesPath.toFile(), newTrack);
		return newTrack;
	}

	private void setEncoderAndBitRateToTrack(File trackFile, Track track) {
		try {
			AudioFile audioFile = AudioFileIO.read(trackFile);
			track.setEncoding(audioFile.getAudioHeader().getEncodingType());
			track.setEncoder(audioFile.getTag().getFirst(FieldKey.ENCODER));
			String bitRate = audioFile.getAudioHeader().getBitRate();
			if ("~".equals(bitRate.substring(0, 1))) {
				track.setIsVariableBitRate(true);
				bitRate = bitRate.substring(1);
			}
			track.setBitRate(Integer.parseInt(bitRate));
		}
		catch (CannotReadException | IOException | TagException |
				ReadOnlyFileException | InvalidAudioFrameException exception) {
			LOG.warn("Error getting encoder or bitrate from track {}:", track.getTrackId(), exception);
		}
	}

	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage("Itunes import succeeded");
		LOG.info("Itunes import task completed");

		stageDemon.showIndeterminateProgress();
		Thread addTracksAndPlaylistToMusicLibrary = new Thread(this::addTracksAndPlaylistsToMusicLibrary);
		addTracksAndPlaylistToMusicLibrary.start();

		if (! notFoundFiles.isEmpty())
			errorDemon.showExpandableErrorsDialog("Some files were not found", "", notFoundFiles);
		if (! parseErrors.isEmpty())
			errorDemon.showExpandableErrorsDialog("Errors importing files", "", parseErrors);
	}

	private void addTracksAndPlaylistsToMusicLibrary() {
		Platform.runLater(() -> updateTaskProgressOnView(- 1, ""));
		playlists.forEach(playlist -> Platform.runLater(() -> navigationController.addNewPlaylist(playlist)));
		musicLibrary.addTracks(tracks);
		Platform.runLater(stageDemon::closeIndeterminateProgress);

		long endMillis = System.currentTimeMillis() - startMillis;
		double totalTaskTime = Duration.millis(endMillis).toMinutes();
		String statusMessage = tracks.size() + " in (" + totalTaskTime + ") mins";
		Platform.runLater(() -> updateTaskProgressOnView(0.0, statusMessage));
	}

	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Itunes import cancelled");
		LOG.info("Itunes import task cancelled");
	}
}
