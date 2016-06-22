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
 * Copyright (C) 2005, 2006 Octavio Calleya
 */

package com.musicott;

import com.cedarsoftware.util.io.*;
import com.musicott.model.*;
import com.musicott.services.*;
import com.musicott.util.*;
import com.musicott.view.*;
import com.musicott.view.custom.*;
import com.sun.javafx.application.*;
import com.sun.javafx.collections.*;
import javafx.application.*;
import javafx.application.Preloader.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import org.slf4j.Logger;
import org.slf4j.*;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.logging.*;

import static com.musicott.view.MusicottView.*;

/**
 * Creates and launch Musicott
 * 
 * @author Octavio Calleya
 *
 */
@SuppressWarnings({"restriction", "unchecked", "unused"})
public class MainApp extends Application {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private final String CONFIG_FILE = "resources/config/config.properties";
	protected static final String FIRST_USE_EVENT = "first_use";
	private static final String LOGGING_PROPERTIES = "resources/config/logging.properties";
	private static final String LOG_FILE = "Musicott-main-log.txt";
	public static final String TRACKS_PERSISTENCE_FILE = "Musicott-tracks.json";
	public static final String WAVEFORMS_PERSISTENCE_FILE = "Musicott-waveforms.json";
	public static final String PLAYLISTS_PERSISTENCE_FILE = "Musicott-playlists.json";
	private ErrorDemon errorDemon;
	private StageDemon stageDemon;
	private MusicLibrary musicLibrary;
	private MainPreferences preferences;
	private Stage rootStage;
	private FXMLLoader rootLoader, playQueueLoader, navigationLoader, playerLoader;
	private int totalPreloadSteps;
	
	public MainApp() {
		preferences = MainPreferences.getInstance();
		stageDemon = StageDemon.getInstance();
		errorDemon = ErrorDemon.getInstance();
		musicLibrary = MusicLibrary.getInstance();
	}

	public static void main(String[] args) {
		prepareLogger();
		LauncherImpl.launchApplication(MainApp.class, MainPreloader.class, args);
	}
	
	@Override
	public void init() throws Exception {
		if(preferences.getMusicottUserFolder() == null || !new File (preferences.getMusicottUserFolder()).exists())
			LauncherImpl.notifyPreloader(this, new CustomProgressNotification(0, FIRST_USE_EVENT));
		stageDemon.setApplicationHostServices(getHostServices());
		loadConfigProperties();
		loadWaveforms();
		loadLayout();
		loadPlaylists();
		loadTracks();
	}	
	
	@Override
	public void start(Stage primaryStage) throws IOException {
		stageDemon.setMainStage(primaryStage);
		rootStage = primaryStage;
		rootStage.setOnCloseRequest(event -> {LOG.info("Exiting Musicott");	System.exit(0);});
		loadStage();
		LOG.debug("Showing root stage");
	}

	/**
	 * Builds the root stage and layouts
	 */
	private void loadStage() {
		try {
			LOG.info("Building application");			
			VBox navigationLayout = navigationLoader.load();
			NavigationController navigationController = (NavigationController) navigationLoader.getController();
			stageDemon.setNavigationController(navigationController);
			LOG.debug("Navigation layout loaded");
			
			GridPane playerGridPane = playerLoader.load();
			PlayerController playerController = playerLoader.getController();
			stageDemon.setPlayerController(playerController);
			TextField searchTextField = (TextField) playerGridPane.lookup("#searchTextField");
			LOG.debug("Player layout loaded");
			
			AnchorPane playQueuePane = playQueueLoader.load();
			PlayQueueController playQueueController = playQueueLoader.getController();
			stageDemon.setPlayQueueController(playQueueController);		
			playerController.setPlayQueuePane(playQueuePane);	
			LOG.debug("Playqueue layout loaded");
			
			// Hide playqueue pane if click outside
			navigationLayout.setOnMouseClicked(e -> playerController.showPlayQueue(false));

			BorderPane rootLayout = rootLoader.load();
			RootController rootController = rootLoader.getController();
			stageDemon.setRootController(rootController);
			LOG.debug("Root layout loaded");
			
			BorderPane contentBorderLayout = (BorderPane) rootLayout.lookup("#contentBorderLayout");
			contentBorderLayout.setBottom(playerGridPane);
			contentBorderLayout.setLeft(navigationLayout);
			navigationController.showMode(NavigationMode.ALL_SONGS_MODE);
			VBox headerVBox = (VBox) rootLayout.lookup("#headerVBox");
			MusicottMenuBar menuBar = new MusicottMenuBar(headerVBox);
			
			Scene mainScene = new Scene(rootLayout, 1200, 775);
			rootStage.setScene(mainScene);
			rootStage.setTitle("Musicott");
			rootStage.getIcons().add(new Image (getClass().getResourceAsStream("/images/musicotticon.png")));
			rootStage.setMinWidth(1200);
			rootStage.setMinHeight(790);
			rootStage.setMaxWidth(1800);
			rootStage.show();
		} catch (IOException | RuntimeException e) {
			LOG.error("Error", e);
			System.exit(0);
		}
	}
	
	/**
	 * Loads required configuration parameters from a properties file
	 */
	private void loadConfigProperties() {
		Properties prop = new Properties ();
		try {
			notifyPreloader(0, 4, "Loading configuration...");
			prop.load(new FileInputStream(CONFIG_FILE));
			String API_KEY = prop.getProperty("lastfm_api_key");
			String API_SECRET = prop.getProperty("lastfm_api_secret");
			Services.getInstance().getServicesPreferences().setAPISecret(API_SECRET);
			Services.getInstance().getServicesPreferences().setAPIKey(API_KEY);		
		} catch (IOException e) {}
	}
		
	/**
	 * Loads the waveforms map collection from a saved file formatted in JSON using Json-IO
	 * 
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private void loadWaveforms() {
		Map<Integer,float[]> waveformsMap = null;
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
			waveformsMap = new HashMap<Integer, float[]>();
		musicLibrary.setWaveforms(waveformsMap);
	}
	
	/**
	 * Preloads the layouts in the preloader
	 */
	private void loadLayout() {
		LOG.debug("Loading layouts");
		notifyPreloader(2, 4, "Loading layout...");
		// Root layout
	    rootLoader = new FXMLLoader();
		rootLoader.setLocation(getClass().getResource(ROOT_LAYOUT));
		
		// Dropdown play queue 
		playQueueLoader = new FXMLLoader();
		playQueueLoader.setLocation(getClass().getResource(PLAYQUEUE_LAYOUT));
		
		// Navigation layout
		navigationLoader = new FXMLLoader();
		navigationLoader.setLocation(getClass().getResource(NAVIGATION_LAYOUT));
		
		// Player Layout
		playerLoader = new FXMLLoader();
		playerLoader.setLocation(getClass().getResource(PLAYER_LAYOUT));
	}
	
	
	/**
	 * Loads the playlists from a saved file formatted in JSON using Json-IO
	 * 
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private void loadPlaylists() {
		List<Playlist> playlists = null;
		File playlistsFile = new File(preferences.getMusicottUserFolder()+File.separator+PLAYLISTS_PERSISTENCE_FILE);
		int totalPlaylists, step = 0;
		if(playlistsFile.exists()) {
			notifyPreloader(3, 4, "Loading playlists...");
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
				LOG.error("Error loading playlists", e);
				errorDemon.showErrorDialog("Error loading playlists", null, e);
			}
		}
		else {
			playlists = new ArrayList<>();
			playlists.add(new Playlist("My Top 10", false));
			playlists.add(new Playlist("Favourites", false));
			playlists.add(new Playlist("Listen later", false));
		}
		musicLibrary.setPlaylists(playlists);
	}

	/**
	 * Loads the track map collection from a saved file formatted in JSON using Json-IO
	 * 
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private void loadTracks() {
		ObservableMap<Integer, Track> map = null;
		File tracksFile = new File(preferences.getMusicottUserFolder()+File.separator+TRACKS_PERSISTENCE_FILE);
		int totalTracks, step = 0;
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
	private static void prepareLogger() {
		Handler baseFileHandler;
		java.util.logging.Logger logger = LogManager.getLogManager().getLogger("");
		Handler rootHandler = logger.getHandlers()[0];
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream(LOGGING_PROPERTIES));
			baseFileHandler = new FileHandler(LOG_FILE);
			baseFileHandler.setFormatter(new SimpleFormatter() {
				public String format(LogRecord rec) {
					StringBuffer str = new StringBuffer();
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
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Handles a notification to be shown in the preloader stage
	 * 
	 * @param step The step number of the preloader process
	 * @param totalWork The total numer of steps of the preloader process
	 * @param detailMessage A notification message to be shown in the preloader
	 */
	private void notifyPreloader(int step, int totalWork, String detailMessage) {
		if(totalWork != 0)
			this.totalPreloadSteps = totalWork;
		else
			this.totalPreloadSteps = 3;
		double progress = (double) step/this.totalPreloadSteps;
		LauncherImpl.notifyPreloader(this, new CustomProgressNotification(progress, detailMessage));
	}
	
	/**
	 * Extends from {@link PreloaderNotification} to encapsulate progress and message
	 * of a preloader application process
	 *
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