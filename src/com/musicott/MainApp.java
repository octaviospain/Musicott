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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cedarsoftware.util.io.JsonIoException;
import com.cedarsoftware.util.io.JsonReader;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.util.ObservableMapWrapperCreator;
import com.musicott.view.RootController;
import com.musicott.view.PlayQueueController;
import com.sun.javafx.application.LauncherImpl;
import com.sun.javafx.collections.ObservableMapWrapper;

import javafx.application.Application;
import javafx.application.Preloader.PreloaderNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * @author Octavio Calleya
 *
 */
@SuppressWarnings({"restriction", "unchecked"})
public class MainApp extends Application {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private ErrorHandler eh;
	private SceneManager sc;
	private MusicLibrary ml;
	private MainPreferences pf;
	private Stage rootStage;
	private BorderPane rootLayout;
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
			LauncherImpl.notifyPreloader(this, new EventNotification("first_use"));
		eh.setApplicationHostServices(getHostServices());
		loadWaveforms();
		loadLayout();
		loadTracks();
	}
	
	@Override
	public void start(Stage primaryStage) throws IOException {
		sc.setMainStage(primaryStage);
		rootStage = primaryStage;
		loadStage();
		LOG.debug("Showing root stage");
		if(eh.hasErrors(ErrorType.FATAL)) {
			eh.showErrorDialog(rootStage.getScene(), ErrorType.FATAL);
			System.exit(0);
		}
	}
	
	private void loadLayout() {
		LOG.debug("Loading layouts");
		notifyPreloader(1, 3, "Loading layout...");
	    rootLoader = new FXMLLoader();
		rootLoader.setLocation(getClass().getResource("/view/RootLayout.fxml"));
		
		// Set the dropdown play queue 
		playQueueLoader = new FXMLLoader();
		playQueueLoader.setLocation(getClass().getResource("/view/PlayQueueLayout.fxml"));
		notifyPreloader(2, 3, "Loading tracks...");
	}
	
	private void loadStage() {
		try {
			LOG.info("Building application");
			rootLayout = (BorderPane) rootLoader.load();
			RootController rootController = (RootController) rootLoader.getController();
			rootController.setApplicationHostServices(getHostServices());
			rootController.setStage(rootStage);
			sc.setRootController(rootController);
			LOG.debug("Root layout loaded");
			
			AnchorPane playQueuePane = (AnchorPane) playQueueLoader.load();
			PlayQueueController pqc = (PlayQueueController) playQueueLoader.getController();
			sc.setPlayQueueController(pqc);
			playQueuePane.setVisible(false);
			rootLayout.getChildren().add(playQueuePane);
			LOG.debug("Play queue layout loaded");
			
			ToggleButton playQueueButton = (ToggleButton) rootLayout.lookup("#playQueueButton");
			playQueuePane.setLayoutY(playQueueButton.getLayoutY()+50);
			playQueuePane.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
			LOG.debug("Play queue placed");
			
			// The play queue pane moves if the window is resized
			rootStage.widthProperty().addListener((observable, oldValue, newValue) -> {
				playQueuePane.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
				playQueuePane.setLayoutY(playQueueButton.getLayoutY()+50);
			});
			rootStage.heightProperty().addListener((observable, oldValue, newValue) -> {
				playQueuePane.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
				playQueuePane.setLayoutY(playQueueButton.getLayoutY()+50);
			});
			LOG.debug("Configured play queue pane to move with the root layout");
			
			// Closes the play queue pane when click on the view
			rootLayout.setOnMouseClicked(event -> {if(playQueuePane.isVisible()) {playQueuePane.setVisible(false); playQueueButton.setSelected(false);}});
			TableView<Track> trackTable = (TableView<Track>) rootLayout.lookup("#trackTable");
			trackTable.setOnMouseClicked(event -> {if(playQueuePane.isVisible()) {playQueuePane.setVisible(false); playQueueButton.setSelected(false);}});
			LOG.debug("Configured play queue pane to close if the user click outside it");

			rootStage.setTitle("Musicott");
			rootStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/musicotticon.png")));
			rootStage.setMinWidth(1200);
			rootStage.setMinHeight(775);
			rootStage.setMaxWidth(1800);
			Scene mainScene = new Scene(rootLayout,1200,775);
			rootStage.setScene(mainScene);
			rootStage.show();
		} catch (IOException | RuntimeException e) {
			eh.addError(e, ErrorType.FATAL);
			LOG.error("Error", e);
		}
	}
	
	private void loadTracks() {
		ObservableMap<Integer, Track> map = null;
		File tracksFile = new File(pf.getMusicottUserFolder()+"/Musicott-tracks.json");
		int totalTracks, step = 0;
		if(tracksFile.exists()) {
			try {
				notifyPreloader(-1, 0, "Loading tracks...");
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
				LOG.error("Error loading track library: ", e);
				ErrorHandler.getInstance().addError(e, ErrorType.FATAL);
			}
		}
		if(map != null)
			MusicLibrary.getInstance().setTracks(map);
		else
			MusicLibrary.getInstance().setTracks(FXCollections.observableHashMap());
	}
	
	private void loadWaveforms() {
		Map<Integer,float[]> waveformsMap = null;
		File waveformsFile = new File(pf.getMusicottUserFolder()+"/Musicott-waveforms.json");
		FileInputStream fis;
		JsonReader jsr;
		if(waveformsFile.exists()) {
			try {
				notifyPreloader(0, 3, "Loading waveforms...");
				fis = new FileInputStream(waveformsFile);
				jsr = new JsonReader(fis);
				waveformsMap = (Map<Integer,float[]>) jsr.readObject();
				jsr.close();
				fis.close();
				LOG.info("Loaded waveform images from {}", waveformsFile);
			} catch(IOException e) {
				eh.addError(e, ErrorType.FATAL);
				LOG.error("Error loading waveform images: ", e);
			}
		}
		else
			waveformsMap = new HashMap<Integer, float[]>();
		ml.setWaveforms(waveformsMap);
		notifyPreloader(1, 3, "Loading layout...");
	}
	
	private static void prepareLogger() {
		Handler baseFileHandler;
		java.util.logging.Logger logger = LogManager.getLogManager().getLogger("");
		Handler rootHandler = logger.getHandlers()[0];
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream("resources/config/logging.properties"));
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
	
	private void notifyPreloader(int step, int totalWork, String detailMessage) {
		if(totalWork != 0)
			this.totalPreloadSteps = totalWork;
		else
			this.totalPreloadSteps = 3;
		double progress = (double) step/this.totalPreloadSteps;
		LauncherImpl.notifyPreloader(this, new CustomProgressNotification(progress, detailMessage));
	}
	
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
	
	class EventNotification implements PreloaderNotification {
		
		private String event;
		
		public EventNotification(String event) {
			this.event = event;
		}
		
		public String getEvent() {
			return event;
		}
	}
}