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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cedarsoftware.util.io.JsonReader;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.util.ObservableListWrapperCreator;
import com.musicott.view.RootController;
import com.musicott.view.PlayQueueController;
import com.sun.javafx.application.LauncherImpl;
import com.sun.javafx.collections.ObservableListWrapper;

import javafx.application.Application;
import javafx.application.Preloader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * @author Octavio Calleya
 *
 */
@SuppressWarnings({"restriction", "unchecked"})
public class MainApp extends Application {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private ErrorHandler eh;
	private MusicLibrary ml;
	private Stage rootStage;
	private RootController rootController;
	
	public MainApp() {
		eh = ErrorHandler.getInstance();
		ml = MusicLibrary.getInstance();
	}

	public static void main(String[] args) {
		prepareLogger();
		LauncherImpl.launchApplication(MainApp.class, MainPreloader.class, args);
	}
	
	@Override
	public void init() throws Exception {
		ErrorHandler.getInstance().setApplicationHostServices(this.getHostServices());
		AtomicInteger sequence = loadSequence();
		LauncherImpl.notifyPreloader(this, new Preloader.ProgressNotification(1));
		LoadLibraryThread task = new LoadLibraryThread();
		task.setDaemon(true);
		task.start();
		LauncherImpl.notifyPreloader(this, new Preloader.ProgressNotification(2));
		Map<Integer,float[]> waveforms = loadWaveforms();
		ml.setWaveforms(waveforms);
		ml.setTrackSequence(sequence);
		LauncherImpl.notifyPreloader(this, new Preloader.ProgressNotification(3));
	}
	
	@Override
	public void start(Stage primaryStage) throws IOException {			    
		rootStage = primaryStage;
		rootStage.setTitle("Musicott");
		rootStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/musicotticon.png")));
		SceneManager.getInstance().setMainStage(rootStage);
		initPrimaryStage();
		if(ErrorHandler.getInstance().hasErrors(ErrorType.FATAL)) {
			ErrorHandler.getInstance().showErrorDialog(rootStage.getScene(), ErrorType.FATAL);
			System.exit(0);
		}
	}
	
	private void initPrimaryStage() {
		try {
		    LOG.info("Building application primary stage");
		    FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("/view/RootLayout.fxml"));
			BorderPane rootLayout = (BorderPane) loader.load();
			rootController = (RootController) loader.getController();
			rootController.setApplicationHostServices(getHostServices());
			rootController.setStage(rootStage);
			SceneManager.getInstance().setRootController(rootController);
			
			Scene mainScene = new Scene(rootLayout,1200,775);
			rootStage.setMinWidth(1200);
			rootStage.setMinHeight(775);
			rootStage.setScene(mainScene);
			rootStage.show();
			
			// Set the dropdown play queue 
			FXMLLoader playQueueLoader = new FXMLLoader();
			playQueueLoader.setLocation(getClass().getResource("/view/PlayQueueLayout.fxml"));
			AnchorPane playQueuePane = (AnchorPane) playQueueLoader.load();
			PlayQueueController pqc = (PlayQueueController) playQueueLoader.getController();
			SceneManager.getInstance().setPlayQueueController(pqc);
			playQueuePane.setVisible(false);
			rootLayout.getChildren().add(playQueuePane);
			
			ToggleButton playQueueButton = (ToggleButton) rootLayout.lookup("#playQueueButton");
			playQueuePane.setLayoutY(playQueueButton.getLayoutY()+50);
			playQueuePane.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
			
			// The play queue pane moves if the window is resized
			rootStage.widthProperty().addListener((observable, oldValue, newValue) -> {
				playQueuePane.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
				playQueuePane.setLayoutY(playQueueButton.getLayoutY()+50);
			});
			rootStage.heightProperty().addListener((observable, oldValue, newValue) -> {
				playQueuePane.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
				playQueuePane.setLayoutY(playQueueButton.getLayoutY()+50);
			});
			
			// Closes the play queue pane when click on the view
			rootLayout.setOnMouseClicked(event -> {if(playQueuePane.isVisible()) playQueuePane.setVisible(false);});
			TableView<Track> trackTable = (TableView<Track>) rootLayout.lookup("#trackTable");
			trackTable.setOnMouseClicked(event -> {if(playQueuePane.isVisible()) playQueuePane.setVisible(false);});
			
			// Set the thumb image of the track slider
			StackPane thumb = (StackPane) rootLayout.lookup("#trackSlider").lookup(".thumb");
			thumb.getChildren().clear();
			thumb.getChildren().add(new ImageView(new Image(getClass().getResourceAsStream("/icons/sliderthumb-icon.png"))));
			thumb.setPrefSize(5,5);
		} catch (IOException | RuntimeException e) {
			eh.addError(e, ErrorType.FATAL);
			LOG.error("Error", e);
		}
	}
	
	private Map<Integer,float[]> loadWaveforms() {
		Map<Integer,float[]> waveformsMap = null;
		File waveformsFile = new File("./resources/waveforms.json");
		FileInputStream fis;
		JsonReader jsr;
		if(waveformsFile.exists()) {
			try {
				LOG.info("Retrieving waveform images from {}", waveformsFile);
				fis = new FileInputStream(waveformsFile);
				jsr = new JsonReader(fis);
				waveformsMap = (Map<Integer,float[]>) jsr.readObject();
				jsr.close();
				fis.close();
			} catch(IOException e) {
				eh.addError(e, ErrorType.FATAL);
				LOG.error("Error loading waveform images: ", e);
			}
		}
		else
			waveformsMap = new HashMap<Integer, float[]>();
		return waveformsMap;
	}
	
	private AtomicInteger loadSequence() {
		AtomicInteger sequence = null;
		File sequenceFile = new File("./resources/seq.json");
		FileInputStream fis;
		JsonReader jsr;
		if(sequenceFile.exists()) {
			try {
				LOG.info("Retrieving track sequence");
				fis = new FileInputStream(sequenceFile);
				jsr = new JsonReader(fis);
				sequence = (AtomicInteger) jsr.readObject();
				jsr.close();
				fis.close();
			} catch (IOException e) {
				eh.addError(e, ErrorType.FATAL);
				LOG.error("Error loading waveform images: ", e);
			}
		}
		else
			sequence = new AtomicInteger();
		return sequence;
	}
	
	private static void prepareLogger() {
		Handler baseFileHandler;
		java.util.logging.Logger logger = LogManager.getLogManager().getLogger("");
		Handler rootHandler = logger.getHandlers()[0];
		try {
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
}

class LoadLibraryThread extends Thread {

	@Override
	public void run() {
		ObservableList<Track> list = null;
		File tracksFile = new File("./resources/tracks.json");
		if(tracksFile.exists()) {
			try {
				FileInputStream fis = new FileInputStream(tracksFile);
				JsonReader jsr = new JsonReader(fis);
				JsonReader.assignInstantiator(ObservableListWrapper.class, new ObservableListWrapperCreator());
				list = (ObservableList<Track>) jsr.readObject();
				jsr.close();
				fis.close();
				for(Track t: list) {
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
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(list != null)
			MusicLibrary.getInstance().setTracks(list);
		else
			MusicLibrary.getInstance().setTracks(FXCollections.observableArrayList());
	}
}