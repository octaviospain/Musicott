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

package com.musicott;

import com.cedarsoftware.util.io.*;
import com.musicott.model.*;
import com.musicott.services.*;
import com.musicott.util.*;
import com.sun.javafx.application.*;
import com.sun.javafx.collections.*;
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
 * Creates and launch Musicott
 * 
 * @author Octavio Calleya
 * @version 0.9
 * @see <a href="https://octaviospain.github.io/Musicott">Musicott</a>
 */
@SuppressWarnings({"restriction", "unchecked", "unused"})
public class MainApp extends Application {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private static final String CONFIG_FILE = "resources/config/config.properties";
	private static final String LOGGING_PROPERTIES = "resources/config/logging.properties";
	private static final String LOG_FILE = "Musicott-main-log.txt";
	protected static final String FIRST_USE_EVENT = "first_use";

	public static final String TRACKS_PERSISTENCE_FILE = "Musicott-tracks.json";
	public static final String WAVEFORMS_PERSISTENCE_FILE = "Musicott-waveforms.json";
	public static final String PLAYLISTS_PERSISTENCE_FILE = "Musicott-playlists.json";

	private ErrorDemon errorDemon;
	private StageDemon stageDemon;
	private MusicLibrary musicLibrary;
	private MainPreferences preferences;
	private Stage rootStage;
	private String applicationFolder;
	private int numPreloaderSteps;
	
	public MainApp() {
		preferences = MainPreferences.getInstance();
		stageDemon = StageDemon.getInstance();
		errorDemon = ErrorDemon.getInstance();
		musicLibrary = MusicLibrary.getInstance();
	}

	public static void main(String[] args) {
		initializeLogger();
		LauncherImpl.launchApplication(MainApp.class, MainPreloader.class, args);
	}
	
	@Override
	public void init() throws Exception {
		if(preferences.getMusicottUserFolder() == null || !new File (preferences.getMusicottUserFolder()).exists())
			LauncherImpl.notifyPreloader(this, new CustomProgressNotification(0, FIRST_USE_EVENT));
		stageDemon.setApplicationHostServices(getHostServices());
		applicationFolder = preferences.getMusicottUserFolder();
		loadConfigProperties();
		loadWaveforms();
		loadPlaylists();
		loadTracks();
	}	
	
	@Override
	public void start(Stage primaryStage) throws IOException {
		rootStage = primaryStage;
		rootStage.setOnCloseRequest(event -> {
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
		Properties properties = new Properties ();
		try {
			notifyPreloader(0, 4, "Loading configuration...");
			properties.load(new FileInputStream(CONFIG_FILE));
			String apiKey = properties.getProperty("lastfm_api_key");
			String apiSecret = properties.getProperty("lastfm_api_secret");
			Services.getInstance().getServicesPreferences().setAPISecret(apiSecret);
			Services.getInstance().getServicesPreferences().setAPIKey(apiKey);
		}
		catch (IOException exception) {
			LOG.warn("Error loading configuration properties", exception);
			errorDemon.showErrorDialog("Error", "Error when loading configuration properties", exception);
		}
	}

	/**
	 * Loads the waveforms map from a saved file formatted in JSON using Json-IO
	 * 
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private void loadWaveforms() {
		notifyPreloader(1, 4, "Loading waveforms...");
		Map<Integer, float[]> waveformsMap = null;
		File waveformsFile = new File(applicationFolder + File.separator + WAVEFORMS_PERSISTENCE_FILE);
		if(waveformsFile.exists()) {
			try {
				FileInputStream fileInputStream = new FileInputStream(waveformsFile);
				JsonReader jsonReader = new JsonReader(fileInputStream);
				waveformsMap = (Map<Integer, float[]>) jsonReader.readObject();
				jsonReader.close();
				fileInputStream.close();
				LOG.info("Loaded waveform images from {}", waveformsFile);
			}
			catch(IOException exception) {
				LOG.error("Error loading waveform thumbnails", exception);
				errorDemon.showErrorDialog("Error", "Error when loading waveform thumbnails", exception);
			}
		}
		else
			waveformsMap = new HashMap<>();
		musicLibrary.setWaveforms(waveformsMap);
	}
	
	/**
	 * Loads the saved playlists or creates some predefined ones
	 */
	private void loadPlaylists() {
		notifyPreloader(3, 4, "Loading playlists...");
		String playlistsPath = applicationFolder + File.separator + PLAYLISTS_PERSISTENCE_FILE;
		File playlistsFile = new File(playlistsPath);
		List<Playlist> playlists;
		if(playlistsFile.exists())
			playlists = parsePlaylistFromJsonFile(playlistsFile);
		else {
			playlists = new ArrayList<>();
			playlists.add(new Playlist("My Top 10", false));
			playlists.add(new Playlist("Favourites", false));
			playlists.add(new Playlist("Listen later", false));
		}
		musicLibrary.setPlaylists(playlists);
	}

	/**
	 * Loads the playlists from a saved file formatted in JSON using Json-IO
	 *
	 * @param playlistsFile The JSON formatted file of the playlists
	 * @return a <tt>List</tt> of playlists or null if an error was found
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private List<Playlist> parsePlaylistFromJsonFile(File playlistsFile) {
		List<Playlist> playlists;
		int totalPlaylists;
		int step = 0;
		try {
			FileInputStream fileInputStream = new FileInputStream(playlistsFile);
			JsonReader jsonReader = new JsonReader(fileInputStream);
			playlists = (List<Playlist>) jsonReader.readObject();
			totalPlaylists = playlists.size();
			jsonReader.close();
			fileInputStream.close();

			for(Playlist playlist: playlists) {
				if(playlist.isFolder())
					playlist.getContainedPlaylists().forEach(
							childPlaylist -> childPlaylist.nameProperty().setValue(childPlaylist.getName()));
				playlist.nameProperty().setValue(playlist.getName());
				notifyPreloader(++step, totalPlaylists, "Loading playlists...");
			}
			LOG.info("Loaded playlists from {}", playlistsFile);
		}
		catch(IOException exception) {
			playlists = null;
			LOG.error("Error loading playlists", exception);
			errorDemon.showErrorDialog("Error loading playlists", null, exception);
		}
		return playlists;
	}

	/**
	 * Loads the track map collection from a saved file formatted in JSON using Json-IO
	 * 
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private void loadTracks() {
		ObservableMap<Integer, Track> map = null;
		File tracksFile = new File(applicationFolder + File.separator + TRACKS_PERSISTENCE_FILE);
		int totalTracks;
		int step = 0;

		if(tracksFile.exists()) {
			notifyPreloader(-1, 0, "Loading tracks...");

			try {
				FileInputStream fis = new FileInputStream(tracksFile);
				JsonReader jsr = new JsonReader(fis);
				JsonReader.assignInstantiator(ObservableMapWrapper.class, new ObservableMapWrapperCreator ());
				map = (ObservableMap<Integer, Track>) jsr.readObject();
				totalTracks = map.size();
				jsr.close();
				fis.close();
				for(Track t: map.values()) {
					setTrackProperties(t);
					notifyPreloader(++step, totalTracks, "Loading tracks...");
				}
				LOG.info("Loaded tracks from {}", tracksFile);
			} catch (IOException | JsonIoException exception) {
				LOG.error("Error loading track library", exception);
			}
		}
		if(map != null)
			MusicLibrary.getInstance().setTracks(map);
		else
			MusicLibrary.getInstance().setTracks(FXCollections.observableHashMap());
	}

	/**
	 * Sets the values of the {@link Track}'s properties, because those are
	 * not stored on the <tt>json</tt> file when deserialized
	 *
	 * @param track The track to the value of its properties
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
		track.hasCoverProperty().setValue(track.hasCover());
		track.dateModifiedProperty().setValue(track.getDateModified());
		track.playCountProperty().setValue(track.getPlayCount());
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
			System.err.println("Error iniciating logger");
			exception.printStackTrace();
		}
	}

	/**
	 * Constructs a log message given a {@link LogRecord}
	 *
	 * @param record The <tt>LogRecord</tt> instance
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
		String secondLine = sequenceNumber + "\t" + loggerLevel + ":" + message +"\n";

		stringBuilder.append(firstLine);
		stringBuilder.append(secondLine);

		if(record.getThrown() != null) {
			stringBuilder.append(record.getThrown() + "\n");
			for(StackTraceElement stackTraceElement: record.getThrown().getStackTrace())
				stringBuilder.append(stackTraceElement + "\n");
		}
		return stringBuilder.toString();
	}
	
	/**
	 * Handles a notification to be shown in the preloader stage
	 * 
	 * @param step The step number of the preloader process
	 * @param totalSteps The total number of steps of the preloader process
	 * @param detailMessage A notification message to be shown in the preloader
	 */
	private void notifyPreloader(int step, int totalSteps, String detailMessage) {
		if(totalSteps != 0)
			numPreloaderSteps = totalSteps;
		else
			numPreloaderSteps = 3;
		double progress = (double) step/numPreloaderSteps;
		LauncherImpl.notifyPreloader(this, new CustomProgressNotification(progress, detailMessage));
	}
	
	/**
	 * Extends from {@link PreloaderNotification} to encapsulate progress and message
	 * of a preloader application process
	 */
	class CustomProgressNotification implements PreloaderNotification {
		
		private final double progress;
        private final String details;
        
        public CustomProgressNotification(double progress) {
            this(progress, "");
        }
        
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
