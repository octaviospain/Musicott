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
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cedarsoftware.util.io.JsonIoException;
import com.cedarsoftware.util.io.JsonReader;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.player.PlayerFacade;
import com.musicott.services.ServiceManager;
import com.musicott.task.TaskPoolManager;
import com.musicott.util.ObservableMapWrapperCreator;
import com.musicott.util.Utils;
import com.musicott.view.PlayQueueController;
import com.musicott.view.RootController;
import com.sun.javafx.application.LauncherImpl;
import com.sun.javafx.collections.ObservableMapWrapper;

import de.codecentric.centerdevice.MenuToolkit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader.PreloaderNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
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
			LauncherImpl.notifyPreloader(this, new CustomProgressNotification(0, FIRST_USE_EVENT));
		eh.setApplicationHostServices(getHostServices());
		loadConfigProperties();
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
	}
	
	/**
	 * Preloads the layouts in the preloader
	 */
	private void loadLayout() {
		LOG.debug("Loading layouts");
		notifyPreloader(1, 3, "Loading layout...");
	    rootLoader = new FXMLLoader();
		rootLoader.setLocation(getClass().getResource(LAYOUTS_PATH+ROOT_LAYOUT));
		
		// Set the dropdown play queue 
		playQueueLoader = new FXMLLoader();
		playQueueLoader.setLocation(getClass().getResource(LAYOUTS_PATH+PLAYQUEUE_LAYOUT));
		notifyPreloader(2, 3, "Loading tracks...");
	}
	
	/**
	 * Builds the root stage and layout
	 */
	private void loadStage() {
		try {
			LOG.info("Building application");
			rootLayout = (BorderPane) rootLoader.load();
			RootController rootController = (RootController) rootLoader.getController();
			rootController.setStage(rootStage);
			sc.setRootController(rootController);
			LOG.debug("Root layout loaded");
			
			// Set the play queue layout on the rootlayout
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
			TableView<Map.Entry<Integer, Track>> trackTable = (TableView<Map.Entry<Integer, Track>>) rootLayout.lookup("#trackTable");
			trackTable.setOnMouseClicked(event -> {if(playQueuePane.isVisible()) {playQueuePane.setVisible(false); playQueueButton.setSelected(false);}});
			LOG.debug("Configured play queue pane to close if the user click outside it");			

			// MenuBar, os x native or default
			MenuBar menuBar = new MenuBar();
			Menu fileMN, editMN, controlsMN, aboutMN;
			MenuItem openFileMI, importFolderMI, importItunesMI, preferencesMI, editMI, deleteMI, prevMI, nextMI, volIncrMI, volDecrMI, selCurrMI, aboutMI;

			fileMN = new Menu("File"); editMN = new Menu("Menu"); controlsMN = new Menu("Controls"); aboutMN = new Menu("About");
			openFileMI = new MenuItem("Open File(s)..."); importFolderMI = new MenuItem("Import Folder..."); importItunesMI = new MenuItem("Import from iTunes Library...");
			preferencesMI = new MenuItem("Preferences"); editMI = new MenuItem("Edit"); deleteMI = new MenuItem("Delete"); prevMI = new MenuItem("Previous");
			nextMI = new MenuItem("Next"); volIncrMI = new MenuItem("Increase volume"); volDecrMI = new MenuItem("Decrease volume");
			selCurrMI = new MenuItem("Select Current Track"); aboutMI = new MenuItem("About");
			
			fileMN.getItems().addAll(openFileMI, importFolderMI, importItunesMI);
			editMN.getItems().addAll(editMI, deleteMI);
			controlsMN.getItems().addAll(prevMI, nextMI, volIncrMI, volDecrMI, selCurrMI);
			aboutMN.getItems().add(aboutMI);
			
			// Native Menubar for os x
			String os = System.getProperty ("os.name");
			Modifier keyModifierOS = os != null && os.startsWith ("Mac") ? KeyCodeCombination.META_DOWN : KeyCodeCombination.CONTROL_DOWN;
			if (os != null && os.startsWith ("Mac")) {
				MenuToolkit tk = MenuToolkit.toolkit();
				
				Menu appMenu = new Menu("Musicott");
				appMenu.getItems().addAll(preferencesMI, new SeparatorMenuItem(), tk.createQuitMenuItem("Musicott"));				
				Menu windowMenu = new Menu("Window");
				windowMenu.getItems().addAll(tk.createMinimizeMenuItem(), tk.createZoomMenuItem(),
				new SeparatorMenuItem(), tk.createBringAllToFrontItem());
				
				tk.setApplicationMenu(appMenu);
				menuBar.getMenus().addAll(appMenu, fileMN, editMN, controlsMN, windowMenu, aboutMN);
				tk.autoAddWindowMenuItems(windowMenu);
				tk.setGlobalMenuBar(menuBar);
			}
			else {	// Default MenuBar
				MenuItem closeMI = new MenuItem("Close");
				closeMI.setAccelerator(new KeyCodeCombination(KeyCode.F4, KeyCodeCombination.ALT_DOWN));
				closeMI.setOnAction(event -> {LOG.info("Exiting Musicott"); System.exit(0);});
				fileMN.getItems().addAll(new SeparatorMenuItem(), preferencesMI, new SeparatorMenuItem(), closeMI);
				
				menuBar.getMenus().addAll(fileMN, editMN, controlsMN, aboutMN);
				VBox headerVBox = (VBox) rootLayout.lookup("#headerVBox");
				headerVBox.getChildren().add(0, menuBar);
			}
			
			// Key acceleratos. Command down for os x and control down for windows and linux
			openFileMI.setAccelerator(new KeyCodeCombination(KeyCode.O, keyModifierOS));
			importFolderMI.setAccelerator(new KeyCodeCombination(KeyCode.O, keyModifierOS, KeyCodeCombination.SHIFT_DOWN));
			importItunesMI.setAccelerator(new KeyCodeCombination(KeyCode.I, keyModifierOS, KeyCodeCombination.SHIFT_DOWN));
			preferencesMI.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, keyModifierOS));
			editMI.setAccelerator(new KeyCodeCombination(KeyCode.I, keyModifierOS));
			deleteMI.setAccelerator(new KeyCodeCombination(KeyCode.BACK_SPACE, keyModifierOS));
			prevMI.setAccelerator(new KeyCodeCombination(KeyCode.RIGHT, keyModifierOS));
			nextMI.setAccelerator(new KeyCodeCombination(KeyCode.LEFT, keyModifierOS));
			volIncrMI.setAccelerator(new KeyCodeCombination(KeyCode.UP, keyModifierOS));
			volDecrMI.setAccelerator(new KeyCodeCombination(KeyCode.DOWN, keyModifierOS));
			selCurrMI.setAccelerator(new KeyCodeCombination(KeyCode.L, keyModifierOS));
			
			// actions and bindings for the menus
			openFileMI.setOnAction(e -> {
				LOG.debug("Selecting files to open");
				FileChooser chooser = new FileChooser();
				chooser.setTitle("Open file(s)...");
				chooser.getExtensionFilters().addAll(
						new ExtensionFilter("All Supported (*.mp3, *.flac, *.wav, *.m4a)","*.mp3", "*.flac", "*.wav", "*.m4a"),
						new ExtensionFilter("mp3 files (*.mp3)", "*.mp3"),
						new ExtensionFilter("flac files (*.flac)","*.flac"),
						new ExtensionFilter("wav files (*.wav)", "*.wav"),
						new ExtensionFilter("m4a files (*.wav)", "*.m4a"));
				List<File> files = chooser.showOpenMultipleDialog(rootStage);
				if(files != null) {
					TaskPoolManager.getInstance().parseFiles(files, true);
					sc.getRootController().setStatusMessage("Opening files");
				}
			});
			importFolderMI.setOnAction(e -> {
				LOG.debug("Choosing folder to being imported");
				DirectoryChooser chooser = new DirectoryChooser();
				chooser.setTitle("Choose folder");
				File folder = chooser.showDialog(rootStage);
				if(folder != null) {
					Thread countFilesThread = new Thread(() -> {
						List<File> files = Utils.getAllFilesInFolder(folder, MainPreferences.getInstance().getExtensionsFileFilter(), 0);
						Platform.runLater(() -> {
							if(files.isEmpty()) {
								Alert alert = createAlert("Import", "No files", "There are no valid files to import on the selected folder."
										+ "Change the folder or the import options in preferences", AlertType.WARNING);
								alert.showAndWait();
							}
							else {
								Alert alert = createAlert("Import", "Import " + files.size() + " files?", "", AlertType.CONFIRMATION);
								Optional<ButtonType> result = alert.showAndWait();
								if(result.isPresent() && result.get().equals(ButtonType.OK)) {
									TaskPoolManager.getInstance().parseFiles(files, false);
									sc.getRootController().setStatusMessage("Importing files");
								}
							}
						});
					});
					countFilesThread.start();
				}
			});
			importItunesMI.setOnAction(e -> {
				LOG.debug("Choosing Itunes xml file");
				FileChooser chooser = new FileChooser();
				chooser.setTitle("Select 'iTunes Music Library.xml' file");
				chooser.getExtensionFilters().add(new ExtensionFilter("xml files (*.xml)", "*.xml"));
				File xmlFile = chooser.showOpenDialog(rootStage);
				if(xmlFile != null) 
					TaskPoolManager.getInstance().parseItunesLibrary(xmlFile.getAbsolutePath());
			});
			preferencesMI.setOnAction(e -> sc.openPreferencesScene());
			editMI.setOnAction(e -> doEdit());
			deleteMI.setOnAction(e -> doDelete());
			Button prevButton = (Button) rootLayout.lookup("#prevButton");
			prevMI.disableProperty().bind(prevButton.disableProperty());
			prevMI.setOnAction(e -> PlayerFacade.getInstance().previous());
			Button nextButton = (Button) rootLayout.lookup("#nextButton");
			nextMI.disableProperty().bind(nextButton.disableProperty());
			nextMI.setOnAction(e -> PlayerFacade.getInstance().next());
			volIncrMI.setOnAction(e -> sc.getRootController().doIncreaseVolume());
			volDecrMI.setOnAction(e -> sc.getRootController().doDecreaseVolume());
			selCurrMI.setOnAction(e -> {
				Track currentTrack = PlayerFacade.getInstance().getCurrentTrack();
				trackTable.getSelectionModel().clearSelection();
				if(currentTrack != null) {
					Map.Entry<Integer, Track> currentEntry = new AbstractMap.SimpleEntry<Integer, Track>(currentTrack.getTrackID(), currentTrack);
					trackTable.getSelectionModel().select(currentEntry);
					trackTable.scrollTo(currentEntry);
				}
				LOG.debug("Current track in the player selected in the table");
			});
			aboutMI.setOnAction(e -> {
				Alert alert = createAlert("About Musicott", "Musicott", "", AlertType.INFORMATION);
				Label text = new Label(" Version 0.8.0\n\n Copyright © 2015 Octavio Calleya.");
				Label text2 = new Label(" Licensed under GNU GPLv3. This product includes\n software developed by other open source projects.");
				Hyperlink githubLink = new Hyperlink("https://github.com/octaviospain/Musicott/");
				githubLink.setOnAction(event -> getHostServices().showDocument(githubLink.getText()));
				FlowPane fp = new FlowPane();
				fp.getChildren().addAll(text, githubLink, text2);
				alert.getDialogPane().contentProperty().set(fp);
				ImageView iv = new ImageView();
				iv.setImage(new Image("file:resources/images/musicotticon.png"));
				alert.setGraphic(iv);
				alert.showAndWait();
				LOG.debug("Showing about window");
			});
			
			// Context menu on table
			MenuItem cmPlay = new MenuItem("Play");
			cmPlay.setOnAction(event -> {
				List<Map.Entry<Integer, Track>> selection = trackTable.getSelectionModel().getSelectedItems();
				if(!selection.isEmpty())
					PlayerFacade.getInstance().addTracks(selection.stream().map(Map.Entry::getKey).collect(Collectors.toList()), true);
			});
			MenuItem cmEdit = new MenuItem("Edit");
			cmEdit.setOnAction(event -> doEdit());
			MenuItem cmDelete = new MenuItem("Delete");
			cmDelete.setOnAction(event -> doDelete());
			MenuItem cmAddToQueue = new MenuItem("Add to Play Queue");
			cmAddToQueue.setOnAction(event -> {
				List<Map.Entry<Integer, Track>> selection = trackTable.getSelectionModel().getSelectedItems();
				if(!selection.isEmpty())
					PlayerFacade.getInstance().addTracks(selection.stream().map(Map.Entry::getKey).collect(Collectors.toList()), false);
			});
			ContextMenu cm = new ContextMenu();
			cm.getItems().add(cmPlay);
			cm.getItems().add(cmAddToQueue);
			cm.getItems().add(cmEdit);
			cm.getItems().add(cmDelete);
			
			// Right click on row = show ContextMenu
			trackTable.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
				if(event.getButton() == MouseButton.SECONDARY) {
					cm.show(trackTable,event.getScreenX(),event.getScreenY());
					LOG.debug("Showing context menu");
				}
				else if(event.getButton() == MouseButton.PRIMARY && cm.isShowing())
					cm.hide();
			});

			rootStage.setTitle("Musicott");
			rootStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/musicotticon.png")));
			rootStage.setMinWidth(1200);
			rootStage.setMinHeight(790);
			rootStage.setMaxWidth(1800);
			Scene mainScene = new Scene(rootLayout,1200,775);
			rootStage.setScene(mainScene);
			rootStage.show();
		} catch (IOException | RuntimeException e) {
			LOG.error("Error", e);
			System.exit(0);
		}
	}
	
	/**
	 * Auxiliary function to create an Alert
	 * 
	 */
	private Alert createAlert(String title, String header, String content, AlertType type) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.initModality(Modality.APPLICATION_MODAL);
		alert.initOwner(rootStage);
		return alert;
	}
	
	/**
	 * Handles edit action
	 */
	private void doEdit() {
		TableView<Map.Entry<Integer, Track>> trackTable = (TableView<Map.Entry<Integer, Track>>) rootLayout.lookup("#trackTable");
		List<Map.Entry<Integer, Track>> selection = trackTable.getSelectionModel().getSelectedItems();
		if(selection != null & !selection.isEmpty()) {
			if(selection.size() > 1) {
				Alert alert = createAlert("", "Are you sure you want to edit multiple files?", "", AlertType.CONFIRMATION);
				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK) {
					sc.openEditScene(selection.stream().map(Map.Entry::getValue).collect(Collectors.toList()));
					LOG.debug("Opened edit stage for various tracks");
				}
				else
					alert.close();
			}
			else {
				sc.openEditScene(selection.stream().map(Map.Entry::getValue).collect(Collectors.toList()));
				LOG.debug("Opened edit stage for a single track");
			}
		}
	}
	
	/**
	 * Handles delete action
	 */
	private void doDelete() {
		TableView<Map.Entry<Integer, Track>> trackTable = (TableView<Map.Entry<Integer, Track>>) rootLayout.lookup("#trackTable");
		List<Map.Entry<Integer, Track>> selection = trackTable.getSelectionModel().getSelectedItems();
		if(selection != null && !selection.isEmpty()) {
			int numDeletedTracks = selection.size();
			Alert alert = createAlert("", "Delete "+numDeletedTracks+" files from Musicott?", "", AlertType.CONFIRMATION);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				new Thread(() -> {
					ml.removeTracks(selection.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
					Platform.runLater(() -> sc.closeIndeterminatedProgressScene());
				}).start();
				sc.openIndeterminatedProgressScene();
			}
			else
				alert.close();
		}
	}
	
	/**
	 * Loads the track map collection from a saved file formatted in JSON using Json-IO
	 * 
	 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
	 */
	private void loadTracks() {
		ObservableMap<Integer, Track> map = null;
		File tracksFile = new File(pf.getMusicottUserFolder()+"/"+TRACKS_PERSISTENCE_FILE);
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
		File waveformsFile = new File(pf.getMusicottUserFolder()+"/"+WAVEFORMS_PERSISTENCE_FILE);
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
				LOG.error("Error loading waveform thumbnails", e);
				eh.showErrorDialog("Error loading waveforms thumbnails", null, e);
			}
		}
		else
			waveformsMap = new HashMap<Integer, float[]>();
		ml.setWaveforms(waveformsMap);
		notifyPreloader(1, 3, "Loading layout...");
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