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
 */
@SuppressWarnings({"restriction", "unchecked", "unused"})
public class MainApp extends Application {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private static final String CONFIG_FILE = "resources/config/config.properties";
	private static final String LOGGING_PROPERTIES = "resources/config/logging.properties";
	private static final String LOG_FILE = "Musicott-main-log.txt";
	
	public static final String TRACKS_PERSISTENCE_FILE = "Musicott-tracks.json";
	public static final String WAVEFORMS_PERSISTENCE_FILE = "Musicott-waveforms.json";
	public static final String PLAYLISTS_PERSISTENCE_FILE = "Musicott-playlists.json";
	public static final String FIRST_USE_EVENT = "first_use";

	private ErrorDemon errorDemon;
	private StageDemon stageDemon;
	private MusicLibrary musicLibrary;
	private MainPreferences preferences;
	private Stage rootStage;
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
	 * Loads required configuration parameters from a properties file
	 */
	private void loadConfigProperties() {
		Properties prop = new Properties ();
		try {
			notifyPreloader(0, 4, "Loading configuration...");
			prop.load(new FileInputStream(CONFIG_FILE));
			String apiKey = prop.getProperty("lastfm_api_key");
			String apiSecret = prop.getProperty("lastfm_api_secret");
			Services.getInstance().getServicesPreferences().setAPISecret(apiSecret);
			Services.getInstance().getServicesPreferences().setAPIKey(apiKey);
		} catch (IOException e) {
			errorDemon.showErrorDialog("Error", "Error loading Musicott", e);
		}
	}
		
	/**
	 * Loads the waveforms map collection from a saved file formatted in JSON using Json-IO
	 * 
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private void loadWaveforms() {
		Map<Integer, float[]> waveformsMap = null;
		File waveformsFile = new File(preferences.getMusicottUserFolder()+File.separator+WAVEFORMS_PERSISTENCE_FILE);
		if(waveformsFile.exists()) {
			notifyPreloader(1, 4, "Loading waveforms...");
			try {
				FileInputStream fis = new FileInputStream(waveformsFile);
				JsonReader jsr = new JsonReader(fis);
				waveformsMap = (Map<Integer,float[]>) jsr.readObject();
				jsr.close();
				fis.close();
				LOG.info("Loaded waveform images from {}", waveformsFile);
			} catch(IOException e) {
				LOG.error("Error loading waveform thumbnails", e);
			}
		}
		else
			waveformsMap = new HashMap<>();
		musicLibrary.setWaveforms(waveformsMap);
	}
	
	/**
	 * Loads the playlists from a file or created a predefined ones
	 */
	private void loadPlaylists() {
		String applicationFolder = preferences.getMusicottUserFolder();
		String playlistsPath = applicationFolder + File.separator+PLAYLISTS_PERSISTENCE_FILE;
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
	 */
	private List<Playlist> parsePlaylistFromJsonFile(File playlistsFile) {
		notifyPreloader(3, 4, "Loading playlists...");
		List<Playlist> playlists;
		int totalPlaylists;
		int step = 0;
		try {
			FileInputStream fis = new FileInputStream(playlistsFile);
			JsonReader jsr = new JsonReader(fis);
			playlists = (List<Playlist>) jsr.readObject();
			totalPlaylists = playlists.size();
			jsr.close();
			fis.close();
			for(Playlist playlist: playlists) {
				if(playlist.isFolder())
					playlist.getContainedPlaylists().forEach(
							childPlaylist -> childPlaylist.nameProperty().setValue(childPlaylist.getName()));
				playlist.nameProperty().setValue(playlist.getName());
				notifyPreloader(++step, totalPlaylists, "Loading playlists...");
			}
			LOG.info("Loaded playlists from {}", playlistsFile);
		} catch(IOException e) {
			playlists = null;
			LOG.error("Error loading playlists", e);
			errorDemon.showErrorDialog("Error loading playlists", null, e);
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
		File tracksFile = new File(preferences.getMusicottUserFolder()+File.separator+TRACKS_PERSISTENCE_FILE);
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
					t.nameProperty().setValue(t.getName());
					t.artistProperty().setValue(t.getArtist());
					t.albumProperty().setValue(t.getAlbum());
					t.genreProperty().setValue(t.getGenre());
					t.commentsProperty().setValue(t.getComments());
					t.albumArtistProperty().setValue(t.getAlbumArtist());
					t.labelProperty().setValue(t.getLabel());
					t.trackNumberProperty().setValue(t.getTrackNumber());
					t.yearProperty().setValue(t.getYear());
					t.discNumberProperty().setValue(t.getDiscNumber());
					t.bpmProperty().setValue(t.getBpm());
					t.hasCoverProperty().setValue(t.hasCover());
					t.dateModifiedProperty().setValue(t.getDateModified());
					t.playCountProperty().setValue(t.getPlayCount());
					notifyPreloader(++step, totalTracks, "Loading tracks...");
				}
				LOG.info("Loaded tracks from {}", tracksFile);
			} catch (IOException | JsonIoException e) {
				LOG.error("Error loading track library", e);
			}
		}
		if(map != null)
			MusicLibrary.getInstance().setTracks(map);
		else
			MusicLibrary.getInstance().setTracks(FXCollections.observableHashMap());
	}
	
	/**
	 * Builds and sets the logger to a file in the root of the application folder
	 */
	private static void initializeLogger() {
		Handler baseFileHandler;
		java.util.logging.Logger logger = LogManager.getLogManager().getLogger("");
		Handler rootHandler = logger.getHandlers()[0];

		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream(LOGGING_PROPERTIES));
			baseFileHandler = new FileHandler(LOG_FILE);
			baseFileHandler.setFormatter(new SimpleFormatter() {

				@Override
				public String format(LogRecord rec) {
					StringBuilder str = new StringBuilder();
					DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss :nnnnnnnnn");
					str.append(rec.getLoggerName()+" "+rec.getSourceMethodName()+" "+ LocalDateTime.now().format(dateFormatter)+"\n");
					str.append(rec.getSequenceNumber()+"\t"+rec.getLevel()+":"+rec.getMessage()+"\n");
					if(rec.getThrown() != null) {
						str.append(rec.getThrown()+"\n");
						for(StackTraceElement ste: rec.getThrown().getStackTrace())
							str.append(ste+"\n");
					}
					return str.toString();
				}
			});

			LogManager.getLogManager().getLogger("").removeHandler(rootHandler);
			LogManager.getLogManager().getLogger("").addHandler(baseFileHandler);
		} catch (SecurityException | IOException exception) {
			System.err.println("Error iniciating logger");
			exception.printStackTrace();
		}
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
