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

package com.musicott;

import static com.musicott.SceneManager.LAYOUTS_PATH;
import static com.musicott.SceneManager.PLAYQUEUE_LAYOUT;
import static com.musicott.SceneManager.ROOT_LAYOUT;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cedarsoftware.util.io.JsonIoException;
import com.cedarsoftware.util.io.JsonReader;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Playlist;
import com.musicott.model.Track;
import com.musicott.services.ServiceManager;
import com.musicott.util.ObservableMapWrapperCreator;
import com.musicott.view.PlayQueueController;
import com.musicott.view.RootController;
import com.musicott.view.custom.MusicottScene;
import com.sun.javafx.application.LauncherImpl;
import com.sun.javafx.collections.ObservableMapWrapper;

import javafx.application.Application;
import javafx.application.Preloader.PreloaderNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Creates and launch Musicott
 * 
 * @author Octavio Calleya
 *
 */
@SuppressWarnings({"restriction", "unchecked"})
public class MainApp extends Application {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private final String CONFIG_FILE = "resources/config/config.properties";
	protected static final String FIRST_USE_EVENT = "first_use";
	private static final String LOGGING_PROPERTIES = "resources/config/logging.properties";
	public static final String TRACKS_PERSISTENCE_FILE = "Musicott-tracks.json";
	public static final String WAVEFORMS_PERSISTENCE_FILE = "Musicott-waveforms.json";
	public static final String PLAYLISTS_PERSISTENCE_FILE = "Musicott-playlists.json";
	private ErrorHandler eh;
	private SceneManager sc;
	private MusicLibrary ml;
	private MainPreferences pf;
	private Stage rootStage;
	private FXMLLoader rootLoader, playQueueLoader;
	private int totalPreloadSteps;
	
	public MainApp() {
		pf = MainPreferences.getInstance();
		eh = ErrorHandler.getInstance();
		sc = SceneManager.getInstance();
		ml = MusicLibrary.getInstance();
	}

	public static void main(String[] args) {
		prepareLogger();
		LauncherImpl.launchApplication(MainApp.class, MainPreloader.class, args);
	}
	
	@Override
	public void init() throws Exception {
		if(pf.getMusicottUserFolder() == null || !new File(pf.getMusicottUserFolder()).exists())
			LauncherImpl.notifyPreloader(this, new CustomProgressNotification(0, FIRST_USE_EVENT));
		eh.setApplicationHostServices(getHostServices());
		loadConfigProperties();
		loadWaveforms();
		loadLayout();
		loadTracks();
		loadPlaylists();
	}	
	
	@Override
	public void start(Stage primaryStage) throws IOException {
		sc.setMainStage(primaryStage);
		rootStage = primaryStage;
		loadStage();
		LOG.debug("Showing root stage");
	}
	
	/**
	 * Preloads the layouts in the preloader
	 */
	private void loadLayout() {
		LOG.debug("Loading layouts");
		notifyPreloader(1, 4, "Loading layout...");
	    rootLoader = new FXMLLoader();
		rootLoader.setLocation(getClass().getResource(LAYOUTS_PATH+ROOT_LAYOUT));		
		
		// Set the dropdown play queue 
		playQueueLoader = new FXMLLoader();
		playQueueLoader.setLocation(getClass().getResource(LAYOUTS_PATH+PLAYQUEUE_LAYOUT));
	}
	
	/**
	 * Builds the root stage and layout
	 */
	private void loadStage() {
		try {
			LOG.info("Building application");
			BorderPane rootLayout = (BorderPane) rootLoader.load();
			RootController rootController = (RootController) rootLoader.getController();
			rootController.setStage(rootStage);
			sc.setRootController(rootController);
			LOG.debug("Root layout loaded");
			
			AnchorPane playQueuePane = (AnchorPane) playQueueLoader.load();
			PlayQueueController pqc = (PlayQueueController) playQueueLoader.getController();
			sc.setPlayQueueController(pqc);
			LOG.debug("Playqueue layout loaded");

			rootStage.setTitle("Musicott");
			rootStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/musicotticon.png")));
			rootStage.setMinWidth(1200);
			rootStage.setMinHeight(790);
			rootStage.setMaxWidth(1800);
			MusicottScene mainScene = new MusicottScene(rootLayout, 1200, 775, getHostServices());
			mainScene.buildPlayQueuePane(playQueuePane);
			rootStage.setScene(mainScene);
			rootStage.show();
		} catch (IOException | RuntimeException e) {
			LOG.error("Error", e);
			System.exit(0);
		}
	}
	
	/**
	 * Loads the track map collection from a saved file formatted in JSON using Json-IO
	 * 
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private void loadTracks() {
		ObservableMap<Integer, Track> map = null;
		File tracksFile = new File(pf.getMusicottUserFolder()+File.separator+TRACKS_PERSISTENCE_FILE);
		int totalTracks, step = 0;
		if(tracksFile.exists()) {
			notifyPreloader(-1, 0, "Loading tracks...");
			try {
				FileInputStream fis = new FileInputStream(tracksFile);
				JsonReader jsr = new JsonReader(fis);
				JsonReader.assignInstantiator(ObservableMapWrapper.class, new ObservableMapWrapperCreator());
				map = (ObservableMap<Integer, Track>) jsr.readObject();
				totalTracks = map.size();
				jsr.close();
				fis.close();
				for(Track t: map.values()) {
					t.getNameProperty().setValue(t.getName());
					t.getArtistProperty().setValue(t.getArtist());
					t.getAlbumProperty().setValue(t.getAlbum());
					t.getGenreProperty().setValue(t.getGenre());
					t.getCommentsProperty().setValue(t.getComments());
					t.getAlbumArtistProperty().setValue(t.getAlbumArtist());
					t.getLabelProperty().setValue(t.getLabel());
					t.getTrackNumberProperty().setValue(t.getTrackNumber());
					t.getYearProperty().setValue(t.getYear());
					t.getDiscNumberProperty().setValue(t.getDiscNumber());
					t.getBpmProperty().setValue(t.getBpm());
					t.getHasCoverProperty().setValue(t.hasCover());
					t.getDateModifiedProperty().setValue(t.getDateModified());
					t.getPlayCountProperty().setValue(t.getPlayCount());
					notifyPreloader(++step, totalTracks, "Loading tracks...");
				}
				LOG.info("Loaded tracks from {}", tracksFile);
			} catch (IOException | JsonIoException e) {
				LOG.error("Error loading track library", e);
				eh.showErrorDialog("Error loading track library", null, e);
			}
		}
		if(map != null)
			MusicLibrary.getInstance().setTracks(map);
		else
			MusicLibrary.getInstance().setTracks(FXCollections.observableHashMap());
	}
	
	/**
	 * Loads the waveforms map collection from a saved file formatted in JSON using Json-IO
	 * 
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private void loadWaveforms() {
		Map<Integer,float[]> waveformsMap = null;
		File waveformsFile = new File(pf.getMusicottUserFolder()+File.separator+WAVEFORMS_PERSISTENCE_FILE);
		if(waveformsFile.exists()) {
			notifyPreloader(0, 4, "Loading waveforms...");
			try {
				FileInputStream fis = new FileInputStream(waveformsFile);
				JsonReader jsr = new JsonReader(fis);
				waveformsMap = (Map<Integer,float[]>) jsr.readObject();
				jsr.close();
				fis.close();
				LOG.info("Loaded waveform images from {}", waveformsFile);
			} catch(IOException e) {
				LOG.error("Error loading waveform thumbnails", e);
				eh.showErrorDialog("Error loading waveforms thumbnails", null, e);
			}
		}
		else
			waveformsMap = new HashMap<Integer, float[]>();
		ml.setWaveforms(waveformsMap);
	}
	
	/**
	 * Loads the playlists from a saved file formatted in JSON using Json-IO
	 * 
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private void loadPlaylists() {
		List<Playlist> playlists = null;
		File playlistsFile = new File(pf.getMusicottUserFolder()+File.separator+PLAYLISTS_PERSISTENCE_FILE);
		if(playlistsFile.exists()) {
			notifyPreloader(3, 4, "Loading playlists...");
			try {
				FileInputStream fis = new FileInputStream(playlistsFile);
				JsonReader jsr = new JsonReader(fis);
				playlists = (List<Playlist>) jsr.readObject();
				jsr.close();
				fis.close();
				LOG.info("Loaded playlists from {}", playlistsFile);
			} catch(IOException e) {
				LOG.error("Error loading playlists", e);
				eh.showErrorDialog("Error loading playlists", null, e);
			}
		}
		else {
			playlists = new ArrayList<Playlist>();
			playlists.add(new Playlist("Recently Added"));
			playlists.add(new Playlist("Random"));
		}
		ml.setPlaylists(playlists);
	}
	
	/**
	 * Loads required configuration parameters from a properties file
	 */
	private void loadConfigProperties() {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(CONFIG_FILE));
			String API_KEY = prop.getProperty("lastfm_api_key");
			String API_SECRET = prop.getProperty("lastfm_api_secret");
			ServiceManager.getInstance().getServicesPreferences().setAPISecret(API_SECRET);
			ServiceManager.getInstance().getServicesPreferences().setAPIKey(API_KEY);		
		} catch (IOException e) {}
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
			baseFileHandler = new FileHandler("Musicott-main-log.txt");
			baseFileHandler.setFormatter(new SimpleFormatter() {
				public String format(LogRecord rec) {
					StringBuffer str = new StringBuffer();
					DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss :nnnnnnnnn");
					str.append(rec.getLoggerName()+" "+rec.getSourceMethodName()+" "+LocalDateTime.now().format(dateFormatter)+"\n");
					str.append(rec.getSequenceNumber()+"\t"+rec.getLevel()+":"+rec.getMessage()+"\n");
					if(rec.getThrown() != null)
						for(StackTraceElement ste: rec.getThrown().getStackTrace())
							str.append(ste+"\n");
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