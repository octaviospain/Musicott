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

package com.transgressoft.musicott;

import com.cedarsoftware.util.io.*;
import com.sun.javafx.application.*;
import com.sun.javafx.collections.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.services.*;
import com.transgressoft.musicott.services.lastfm.*;
import com.transgressoft.musicott.util.*;
import javafx.application.*;
import javafx.application.Preloader.*;
import javafx.collections.*;
import javafx.stage.*;
import org.slf4j.Logger;
import org.slf4j.*;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.logging.*;

/**
 * Creates and launches Musicott. The creation of the application follows this steps:
 * <ol>
 * <li>Initialization of the <tt>Logger</tt></li>
 * <li>The {@link MainPreloader} is created and shown</li>
 * <li>It is checked whether it is the first use, and if so, the user enters the application folder</li>
 * <li>Configuration properties, tracks, playlists and waveforms are loaded</li>
 * <li>The main window is shown</li>
 * </ol>
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 * @see <a href="https://octaviospain.github.io/Musicott">Musicott</a>
 */
public class MusicottApplication extends Application {

	public static final String TRACKS_PERSISTENCE_FILE = "Musicott-tracks.json";
	public static final String WAVEFORMS_PERSISTENCE_FILE = "Musicott-waveforms.json";
	public static final String PLAYLISTS_PERSISTENCE_FILE = "Musicott-playlists.json";
	static final String FIRST_USE_EVENT = "first_use";
	private static final String CONFIG_FILE = "resources/config/config.properties";
	private static final String LOGGING_PROPERTIES = "resources/config/logging.properties";
	private static final String LOG_FILE = "Musicott-main-log.txt";
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private MusicLibrary musicLibrary;
	private MainPreferences preferences;
	private StageDemon stageDemon;
	private String applicationFolder;
	private volatile int step;

	public MusicottApplication() {
		musicLibrary = MusicLibrary.getInstance();
		preferences = MainPreferences.getInstance();
		stageDemon = StageDemon.getInstance();
		ErrorDemon.getInstance();
	}

	public static void main(String[] args) {
		initializeLogger();
		LauncherImpl.launchApplication(MusicottApplication.class, MainPreloader.class, args);
	}

	/**
	 * Initializes a {@link Logger} that stores the log entries on a file
	 *
	 * @see <a href=http://www.slf4j.org/>slf4j</href>
	 */
	private static void initializeLogger() {
		Handler baseFileHandler;
		LogManager logManager = LogManager.getLogManager();
		java.util.logging.Logger logger = logManager.getLogger("");
		Handler rootHandler = logger.getHandlers()[0];

		try {
			logManager.readConfiguration(new FileInputStream(LOGGING_PROPERTIES));
			baseFileHandler = new FileHandler(LOG_FILE);
			baseFileHandler.setFormatter(new SimpleFormatter() {

				@Override
				public String format(LogRecord record) {
					return logTextString(record);
				}
			});
			logManager.getLogger("").removeHandler(rootHandler);
			logManager.getLogger("").addHandler(baseFileHandler);
		}
		catch (SecurityException | IOException exception) {
			System.err.println("Error initiating logger: " + exception.getMessage());
			exception.printStackTrace();
		}
	}

	/**
	 * Constructs a log message given a {@link LogRecord}
	 *
	 * @param record The <tt>LogRecord</tt> instance
	 *
	 * @return The formatted string of a log entries
	 */
	private static String logTextString(LogRecord record) {
		StringBuilder stringBuilder = new StringBuilder();
		String dateTimePattern = "dd/MM/yy HH:mm:ss :nnnnnnnnn";
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateTimePattern);

		String logDate = LocalDateTime.now().format(dateFormatter);
		String loggerName = record.getLoggerName();
		String sourceMethod = record.getSourceMethodName();
		String firstLine = loggerName + " " + sourceMethod + " " + logDate + "\n";

		String sequenceNumber = String.valueOf(record.getSequenceNumber());
		String loggerLevel = record.getLevel().toString();
		String message = record.getMessage();
		String secondLine = sequenceNumber + "\t" + loggerLevel + ":" + message + "\n\n";

		stringBuilder.append(firstLine);
		stringBuilder.append(secondLine);

		if (record.getThrown() != null) {
			stringBuilder.append(record.getThrown() + "\n");
			for (StackTraceElement stackTraceElement : record.getThrown().getStackTrace())
				stringBuilder.append(stackTraceElement + "\n");
		}
		return stringBuilder.toString();
	}

	@Override
	public void init() throws Exception {
		if (preferences.getMusicottUserFolder() == null || ! new File(preferences.getMusicottUserFolder()).exists()) {
			LauncherImpl.notifyPreloader(this, new CustomProgressNotification(0, FIRST_USE_EVENT));
		}
		stageDemon.setApplicationHostServices(getHostServices());
		applicationFolder = preferences.getMusicottUserFolder();
		loadConfigProperties();
		loadWaveforms();
		loadPlaylists();
		loadTracks();
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		primaryStage.setOnCloseRequest(event -> {
			LOG.info("Exiting Musicott");
			System.exit(0);
		});
		stageDemon.showMusicott(primaryStage);
		LOG.debug("Showing root stage");
	}

	/**
	 * Loads required configuration parameters from a <tt>.properties</tt> file
	 */
	private void loadConfigProperties() {
		Properties properties = new Properties();
		try {
			notifyPreloader(0, 4, "Loading configuration...");
			properties.load(new FileInputStream(CONFIG_FILE));
			String apiKey = properties.getProperty("lastfm_api_key");
			String apiSecret = properties.getProperty("lastfm_api_secret");
			LastFmPreferences servicePreferences = ServiceDemon.getInstance().getLastFmPreferences();
			servicePreferences.setApiSecret(apiSecret);
			servicePreferences.setApiKey(apiKey);
		}
		catch (IOException exception) {
			LOG.warn("Error loading configuration properties", exception);
		}
	}

	/**
	 * Loads the waveforms or creates a new collection
	 */
	private void loadWaveforms() {
		notifyPreloader(1, 4, "Loading waveforms...");
		File waveformsFile = new File(applicationFolder + File.separator + WAVEFORMS_PERSISTENCE_FILE);
		Map<Integer, float[]> waveformsMap;
		if (waveformsFile.exists())
			waveformsMap = parseWaveformsFromJsonFile(waveformsFile);
		else
			waveformsMap = new HashMap<>();
		musicLibrary.addWaveforms(waveformsMap);
	}

	/**
	 * Loads saved playlists or creates some predefined ones
	 */
	private void loadPlaylists() {
		notifyPreloader(3, 4, "Loading playlists...");
		String playlistsPath = applicationFolder + File.separator + PLAYLISTS_PERSISTENCE_FILE;
		File playlistsFile = new File(playlistsPath);
		List<Playlist> playlists;
		if (playlistsFile.exists())
			playlists = parsePlaylistFromJsonFile(playlistsFile);
		else {
			playlists = new ArrayList<>();
			playlists.add(new Playlist("My Top 10", false));
			playlists.add(new Playlist("Favourites", false));
			playlists.add(new Playlist("Listen later", false));
		}
		musicLibrary.addPlaylists(playlists);
	}

	/**
	 * Loads save tracks or creates a new collection
	 */
	private void loadTracks() {
		notifyPreloader(- 1, 0, "Loading tracks...");
		File tracksFile = new File(applicationFolder + File.separator + TRACKS_PERSISTENCE_FILE);
		ObservableMap<Integer, Track> tracksMap;
		if (tracksFile.exists())
			tracksMap = parseTracksFromJsonFile(tracksFile);
		else
			tracksMap = FXCollections.observableHashMap();
		musicLibrary.addTracks(tracksMap);
	}

	/**
	 * Handles a notification to be shown in the preloader stage
	 *
	 * @param step          The step number of the preloader process
	 * @param totalSteps    The total number of steps of the preloader process
	 * @param detailMessage A notification message to be shown in the preloader
	 */
	private void notifyPreloader(int step, int totalSteps, String detailMessage) {
		int numPreloaderSteps;
		if (totalSteps != 0)
			numPreloaderSteps = totalSteps;
		else
			numPreloaderSteps = 3;
		double progress = (double) step / numPreloaderSteps;
		LauncherImpl.notifyPreloader(this, new CustomProgressNotification(progress, detailMessage));
	}

	/**
	 * Loads the waveforms from a saved file formatted in JSON
	 *
	 * @param waveformsFile The JSON formatted file of the tracks
	 *
	 * @return an {@link Map} of the waveforms, where the key is the track id of the
	 * waveform and the value the and array of values representing the amplitudes of
	 * the audio {@link Track}
	 */
	@SuppressWarnings ("unchecked")
	private Map<Integer, float[]> parseWaveformsFromJsonFile(File waveformsFile) {
		Map<Integer, float[]> waveformsMap;
		try {
			waveformsMap = (Map<Integer, float[]>) parseJsonFile(waveformsFile);
			LOG.info("Loaded waveform images from {}", waveformsFile);
		}
		catch (IOException exception) {
			waveformsMap = new HashMap<>();
			LOG.error("Error loading waveform thumbnails: {}", exception.getMessage(), exception);
		}
		return waveformsMap;
	}

	/**
	 * Loads the playlists from a saved file formatted in JSON
	 *
	 * @param playlistsFile The JSON formatted file of the playlists
	 *
	 * @return a {@link List} of {@link Playlist} objects
	 */
	@SuppressWarnings ("unchecked")
	private List<Playlist> parsePlaylistFromJsonFile(File playlistsFile) {
		List<Playlist> playlists;
		int totalPlaylists;
		step = 0;
		try {
			JsonReader.assignInstantiator(ObservableListWrapper.class, new ObservableListWrapperCreator());
			playlists = (List<Playlist>) parseJsonFile(playlistsFile);
			totalPlaylists = playlists.size();

			for (Playlist playlist : playlists) {
				if (playlist.isFolder())
					playlist.getContainedPlaylists().forEach(this::setPlaylistProperties);
				setPlaylistProperties(playlist);
				notifyPreloader(++ step, totalPlaylists, "Loading playlists...");
			}

			LOG.info("Loaded playlists from {}", playlistsFile);
		}
		catch (IOException exception) {
			playlists = new ArrayList<>();
			LOG.error("Error loading playlists: {}", exception.getMessage(), exception);
		}
		return playlists;
	}

	/**
	 * Loads the tracks from a saved file formatted in JSON
	 *
	 * @param tracksFile The JSON formatted file of the tracks
	 *
	 * @return an {@link ObservableMap} of the tracks, where the key is the track
	 * id and the value the {@link Track} object
	 */
	@SuppressWarnings ("unchecked")
	private ObservableMap<Integer, Track> parseTracksFromJsonFile(File tracksFile) {
		ObservableMap<Integer, Track> tracksMap;
		int totalTracks;
		step = 0;
		try {
			JsonReader.assignInstantiator(ObservableMapWrapper.class, new ObservableMapWrapperCreator());
			tracksMap = (ObservableMap<Integer, Track>) parseJsonFile(tracksFile);
			totalTracks = tracksMap.size();

			tracksMap.values().parallelStream().forEach(track -> {
				setTrackProperties(track);
				notifyPreloader(++ step, totalTracks, "Loading tracks...");
			});
			LOG.info("Loaded tracks from {}", tracksFile);
		}
		catch (IOException exception) {
			tracksMap = FXCollections.observableHashMap();
			LOG.error("Error loading track library: {}", exception.getMessage(), exception);
		}
		return tracksMap;
	}

	/**
	 * Parses an <tt>Object</tt> of a previously serialized instance using Json-IO
	 *
	 * @param jsonFormattedFile A JSON formatted {@link File}
	 *
	 * @return The parsed <tt>Object</tt>
	 *
	 * @throws IOException If something went bad
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private Object parseJsonFile(File jsonFormattedFile) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(jsonFormattedFile);
		JsonReader jsonReader = new JsonReader(fileInputStream);
		Object parsedObject = jsonReader.readObject();
		jsonReader.close();
		fileInputStream.close();
		return parsedObject;
	}

	/**
	 * Sets the values of the properties of a {@link Playlist} object,
	 * because those are not restored on the <tt>json</tt> file when deserialized
	 *
	 * @param playlist The track to set its properties values
	 */
	private void setPlaylistProperties(Playlist playlist) {
		playlist.nameProperty().setValue(playlist.getName());
		playlist.isFolderProperty().setValue(playlist.isFolder());
	}

	/**
	 * Sets the values of the properties of a {@link Track} object,
	 * because they are not restored on the <tt>json</tt> file when deserialized
	 *
	 * @param track The track to set its properties values
	 */
	private void setTrackProperties(Track track) {
		track.nameProperty().setValue(track.getName());
		track.artistProperty().setValue(track.getArtist());
		track.albumProperty().setValue(track.getAlbum());
		track.genreProperty().setValue(track.getGenre());
		track.commentsProperty().setValue(track.getComments());
		track.albumArtistProperty().setValue(track.getAlbumArtist());
		track.labelProperty().setValue(track.getLabel());
		track.trackNumberProperty().setValue(track.getTrackNumber());
		track.yearProperty().setValue(track.getYear());
		track.discNumberProperty().setValue(track.getDiscNumber());
		track.bpmProperty().setValue(track.getBpm());
		track.lastDateModifiedProperty().setValue(track.getLastDateModified());
		track.playCountProperty().setValue(track.getPlayCount());
		track.getCoverImage().ifPresent(coverBytes -> track.hasCoverProperty().set(true));
		track.isPlayableProperty().setValue(track.isPlayable());
	}

	/**
	 * Extends from {@link PreloaderNotification} to encapsulate progress and message
	 * of a preloader application process
	 */
	class CustomProgressNotification implements PreloaderNotification {

		private final double progress;
		private final String details;

		public CustomProgressNotification(double progress, String details) {
			this.progress = progress;
			this.details = details;
		}

		public double getProgress() {
			return progress;
		}

		public String getDetails() {
			return details;
		}
	}
}
